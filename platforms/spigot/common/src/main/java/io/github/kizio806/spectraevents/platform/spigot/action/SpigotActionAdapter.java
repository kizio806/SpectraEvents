package io.github.kizio806.spectraevents.platform.spigot.action;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.platform.spigot.metadata.SpigotPdcKeys;
import io.github.kizio806.spectraevents.platform.spigot.render.SpigotModelRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Bukkit/Spigot implementation of the shared event action contract. */
public final class SpigotActionAdapter implements PlatformActionPort, Listener {
  private static final int MAX_PARTICLES_PER_ACTION = 10_000;
  private static final int MAX_MOBS_PER_ACTION = 128;
  private static final int MAX_LOOT_ENTRIES = 64;
  private final SpigotModelRenderer renderer;
  private final BukkitAudiences adventure;
  private final Map<EventInstanceId, List<UUID>> ownedEntities = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, BossBar> bossBars = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, Scoreboard> scoreboards = new ConcurrentHashMap<>();
  private ModelRuntimeService modelRuntimeService;

  public SpigotActionAdapter(
      Plugin plugin, SpigotModelRenderer renderer, BukkitAudiences adventure) {
    this.renderer = renderer;
    this.adventure = adventure;
  }

  public void setModelRuntimeService(ModelRuntimeService modelRuntimeService) {
    this.modelRuntimeService = modelRuntimeService;
  }

  /** Reconnects a persistent platform entity to lifecycle cleanup after a restart. */
  public void registerRecoveredEntity(EventInstanceId instanceId, UUID entityId) {
    ownedEntities
        .computeIfAbsent(instanceId, ignored -> new java.util.concurrent.CopyOnWriteArrayList<>())
        .add(entityId);
  }

  @Override
  public void executeAction(
      EventInstance instance, EventRuntimeState state, ActionDefinition action) {
    executeAction(instance, state, action, ExecutionContext.EMPTY);
  }

  @Override
  public void executeAction(
      EventInstance instance,
      EventRuntimeState state,
      ActionDefinition action,
      ExecutionContext context) {
    Map<String, Object> params = action.parameters();
    Location location = resolveLocation(state);
    switch (action.type().toLowerCase(java.util.Locale.ROOT)) {
      case "spawn_model" -> spawnModel(instance, params, location);
      case "move_model" -> moveModel(instance, location);
      case "remove_model" -> removeModels(instance.id());
      case "play_sound" -> playSound(params, location);
      case "spawn_particles" -> spawnParticles(params, location);
      case "give_item" -> giveItem(params, context);
      case "drop_loot" -> dropLoot(params, location);
      case "send_message" -> sendMessage(params, context);
      case "broadcast", "broadcast_message" -> broadcast(params);
      case "spawn_boss", "spawn_entity" -> spawnBoss(instance, state, params, location);
      case "spawn_mobs", "spawn_wave" -> spawnMobs(instance, params, location);
      case "show_bossbar", "create_bossbar", "update_bossbar" ->
          showOrUpdateBossBar(instance, state, params);
      case "remove_bossbar" -> removeBossBar(instance.id());
      case "show_scoreboard", "create_scoreboard", "update_scoreboard" ->
          showOrUpdateScoreboard(instance, state, params);
      case "remove_scoreboard" -> removeScoreboard(instance.id());
      default ->
          throw new FatalActionException(
              "Action '" + action.type() + "' is unsupported on Spigot/Bukkit");
    }
  }

  @Override
  public void cleanupEvent(EventInstanceId instanceId) {
    removeModels(instanceId);
    for (UUID entityId : ownedEntities.getOrDefault(instanceId, List.of())) {
      Entity entity = Bukkit.getEntity(entityId);
      if (entity != null && entity.isValid()) {
        entity.remove();
      }
    }
    ownedEntities.remove(instanceId);
    removeBossBar(instanceId);
    removeScoreboard(instanceId);
  }

  @Override
  public void cleanupAll() {
    List<EventInstanceId> instanceIds = new ArrayList<>();
    instanceIds.addAll(ownedEntities.keySet());
    instanceIds.addAll(bossBars.keySet());
    instanceIds.addAll(scoreboards.keySet());
    for (EventInstanceId instanceId : instanceIds.stream().distinct().toList()) {
      cleanupEvent(instanceId);
    }
    renderer.removeAll();
  }

  @Override
  public int resourceCount(EventInstanceId instanceId) {
    int modelResources =
        modelRuntimeService == null
            ? 0
            : modelRuntimeService.getActiveInstances().stream()
                .filter(handle -> instanceId.equals(handle.ownerEventId()))
                .mapToInt(handle -> handle.parts().size() + handle.interactions().size())
                .sum();
    return modelResources
        + ownedEntities.getOrDefault(instanceId, List.of()).size()
        + (bossBars.containsKey(instanceId) ? 1 : 0)
        + (scoreboards.containsKey(instanceId) ? 1 : 0);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    for (BossBar bossBar : bossBars.values()) {
      bossBar.addPlayer(event.getPlayer());
    }
    scoreboards.values().stream().findFirst().ifPresent(event.getPlayer()::setScoreboard);
  }

  private void spawnModel(
      EventInstance instance, Map<String, Object> params, Location baseLocation) {
    requireLocation(baseLocation, "spawn_model");
    if (modelRuntimeService == null) {
      throw new FatalActionException("Model runtime is unavailable on Spigot/Bukkit");
    }
    String modelId = string(params, "model", "meteor");
    int defaultHeight = "meteor".equalsIgnoreCase(modelId) ? 20 : 0;
    int height = integer(params, "height-offset", integer(params, "height_offset", defaultHeight));
    Location spawn = baseLocation.clone().add(0, height, 0);
    RenderedModelHandle handle =
        modelRuntimeService.spawnModel(new ModelId(modelId), toAnchor(spawn), instance.id());
    if (handle == null) {
      throw new FatalActionException("Failed to spawn model '" + modelId + "'");
    }
  }

  private void moveModel(EventInstance instance, Location location) {
    requireLocation(location, "move_model");
    if (modelRuntimeService == null) {
      throw new FatalActionException("Model runtime is unavailable on Spigot/Bukkit");
    }
    for (RenderedModelHandle handle : modelRuntimeService.getActiveInstances()) {
      if (instance.id().equals(handle.ownerEventId())) {
        modelRuntimeService.updateModelTransform(handle.runtimeId(), toAnchor(location));
      }
    }
  }

  private void removeModels(EventInstanceId instanceId) {
    if (modelRuntimeService == null) {
      return;
    }
    for (RenderedModelHandle handle : List.copyOf(modelRuntimeService.getActiveInstances())) {
      if (instanceId.equals(handle.ownerEventId())) {
        modelRuntimeService.removeModel(handle.runtimeId());
      }
    }
  }

  private void playSound(Map<String, Object> params, Location location) {
    requireLocation(location, "play_sound");
    NamespacedKey key =
        NamespacedKey.fromString(string(params, "sound", "minecraft:entity.generic.explode"));
    Sound sound = key == null ? null : Registry.SOUNDS.get(key);
    if (sound == null) {
      throw new FatalActionException("Unknown sound: " + params.get("sound"));
    }
    location
        .getWorld()
        .playSound(
            location, sound, decimal(params, "volume", 1.0f), decimal(params, "pitch", 1.0f));
  }

  private void spawnParticles(Map<String, Object> params, Location location) {
    requireLocation(location, "spawn_particles");
    String particleName = string(params, "particle", "minecraft:explosion");
    int count = integer(params, "count", 10);
    requireRange(count, 0, MAX_PARTICLES_PER_ACTION, "spawn_particles.count");
    try {
      Particle particle =
          Particle.valueOf(
              particleName.replace("minecraft:", "").toUpperCase(java.util.Locale.ROOT));
      location.getWorld().spawnParticle(particle, location, count);
    } catch (IllegalArgumentException exception) {
      throw new FatalActionException("Unknown particle: " + particleName, exception);
    }
  }

  private void giveItem(Map<String, Object> params, ExecutionContext context) {
    if (!(context.actor() instanceof Player player)) {
      throw new FatalActionException("give_item requires a player interaction context");
    }
    int amount = integer(params, "amount", 1);
    requireRange(amount, 1, 64, "give_item.amount");
    player
        .getInventory()
        .addItem(new ItemStack(material(string(params, "material", "minecraft:diamond")), amount));
  }

  private void dropLoot(Map<String, Object> params, Location location) {
    requireLocation(location, "drop_loot");
    Object configuredItems = params.get("items");
    if (!(configuredItems instanceof List<?> items)) {
      throw new FatalActionException("drop_loot requires an items list");
    }
    if (items.size() > MAX_LOOT_ENTRIES) {
      throw new FatalActionException(
          "drop_loot exceeds the limit of " + MAX_LOOT_ENTRIES + " item entries");
    }
    double radius = decimal(params, "radius", 2.0f);
    requireRange((int) Math.ceil(radius), 0, 128, "drop_loot.radius");
    java.util.Random random = new java.util.Random();
    for (Object configuredItem : items) {
      if (!(configuredItem instanceof Map<?, ?> item)) {
        throw new FatalActionException("drop_loot item must be an object");
      }
      int chance = number(item.get("chance"), 100);
      requireRange(chance, 0, 100, "drop_loot.items.chance");
      if (random.nextInt(100) >= chance) {
        continue;
      }
      Object configuredMaterial = item.get("material");
      String materialName =
          configuredMaterial == null ? "minecraft:diamond" : String.valueOf(configuredMaterial);
      int amount = number(item.get("amount"), 1);
      requireRange(amount, 1, 64, "drop_loot.items.amount");
      Location drop =
          location
              .clone()
              .add((random.nextDouble() - 0.5) * radius, 0.5, (random.nextDouble() - 0.5) * radius);
      location.getWorld().dropItemNaturally(drop, new ItemStack(material(materialName), amount));
    }
  }

  private void sendMessage(Map<String, Object> params, ExecutionContext context) {
    if (!(context.actor() instanceof CommandSender sender)) {
      throw new FatalActionException("send_message requires a command-sender context");
    }
    adventure
        .sender(sender)
        .sendMessage(MiniMessage.miniMessage().deserialize(string(params, "message", "")));
  }

  private void broadcast(Map<String, Object> params) {
    Component message = MiniMessage.miniMessage().deserialize(string(params, "message", ""));
    for (Player player : Bukkit.getOnlinePlayers()) {
      adventure.player(player).sendMessage(message);
    }
    adventure.console().sendMessage(message);
  }

  private void spawnBoss(
      EventInstance instance,
      EventRuntimeState state,
      Map<String, Object> params,
      Location location) {
    requireLocation(location, "spawn_boss");
    Location spawn =
        location
            .clone()
            .add(
                integer(params, "offset-x", 2),
                integer(params, "offset-y", 0),
                integer(params, "offset-z", 0));
    Entity entity =
        location
            .getWorld()
            .spawnEntity(spawn, entityType(string(params, "entity_type", "minecraft:zombie")));
    entity.setCustomName(strip(string(params, "name", "<red>Boss</red>")));
    entity.setCustomNameVisible(true);
    tag(instance.id(), entity);
    state.setBossEntityId(entity.getUniqueId());
  }

  private void spawnMobs(EventInstance instance, Map<String, Object> params, Location location) {
    requireLocation(location, "spawn_mobs");
    Object configuredMobs = params.get("mobs");
    if (!(configuredMobs instanceof List<?> mobs)) {
      throw new FatalActionException("spawn_mobs requires a mobs list");
    }
    int totalMobs = 0;
    for (Object configuredMob : mobs) {
      if (!(configuredMob instanceof Map<?, ?> mob)) {
        throw new FatalActionException("spawn_mobs entry must be an object");
      }
      int amount = number(mob.get("amount"), 1);
      requireRange(amount, 1, MAX_MOBS_PER_ACTION, "spawn_mobs.mobs.amount");
      totalMobs = Math.addExact(totalMobs, amount);
    }
    if (totalMobs > MAX_MOBS_PER_ACTION) {
      throw new FatalActionException(
          "spawn_mobs exceeds the limit of " + MAX_MOBS_PER_ACTION + " entities per action");
    }
    java.util.Random random = new java.util.Random();
    for (Object configuredMob : mobs) {
      if (!(configuredMob instanceof Map<?, ?> mob)) {
        throw new FatalActionException("spawn_mobs entry must be an object");
      }
      int amount = number(mob.get("amount"), 1);
      double radius = number(mob.get("radius"), 3);
      requireRange((int) Math.ceil(radius), 0, 128, "spawn_mobs.mobs.radius");
      Object configuredType = mob.get("entity_type");
      EntityType type =
          entityType(configuredType == null ? "minecraft:zombie" : String.valueOf(configuredType));
      for (int index = 0; index < amount; index++) {
        Location spawn =
            location
                .clone()
                .add(
                    (random.nextDouble() - 0.5) * radius * 2,
                    0,
                    (random.nextDouble() - 0.5) * radius * 2);
        Entity entity = location.getWorld().spawnEntity(spawn, type);
        if (mob.get("name") != null) {
          entity.setCustomName(strip(String.valueOf(mob.get("name"))));
          entity.setCustomNameVisible(true);
        }
        tag(instance.id(), entity);
      }
    }
  }

  private void showOrUpdateBossBar(
      EventInstance instance, EventRuntimeState state, Map<String, Object> params) {
    removeBossBar(instance.id());
    float progress =
        state.maxHealth() > 0 ? (float) state.currentHealth() / state.maxHealth() : 1.0f;
    BossBar bossBar =
        Bukkit.createBossBar(
            render(string(params, "title", "<gold>SpectraEvents</gold>"), instance, state),
            enumValue(BarColor.class, string(params, "color", "PURPLE"), BarColor.PURPLE),
            enumValue(
                BarStyle.class,
                normalizeBarStyle(string(params, "style", "SOLID")),
                BarStyle.SOLID));
    bossBar.setProgress(Math.max(0, Math.min(1, progress)));
    for (Player player : Bukkit.getOnlinePlayers()) {
      bossBar.addPlayer(player);
    }
    bossBars.put(instance.id(), bossBar);
  }

  private void removeBossBar(EventInstanceId instanceId) {
    BossBar bossBar = bossBars.remove(instanceId);
    if (bossBar != null) {
      bossBar.removeAll();
    }
  }

  private void showOrUpdateScoreboard(
      EventInstance instance, EventRuntimeState state, Map<String, Object> params) {
    if (scoreboards.keySet().stream().anyMatch(id -> !id.equals(instance.id()))) {
      throw new FatalActionException(
          "A Bukkit client can display only one sidebar; another event already owns it");
    }
    removeScoreboard(instance.id());
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    Objective objective =
        scoreboard.registerNewObjective(
            "se_" + instance.id().toString().substring(0, 8),
            Criteria.DUMMY,
            render(string(params, "title", "SpectraEvents"), instance, state));
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    Object configuredLines = params.get("lines");
    if (configuredLines instanceof List<?> lines) {
      int score = lines.size();
      for (Object line : lines) {
        objective.getScore(render(String.valueOf(line), instance, state)).setScore(score--);
      }
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.setScoreboard(scoreboard);
    }
    scoreboards.put(instance.id(), scoreboard);
  }

  private void removeScoreboard(EventInstanceId instanceId) {
    Scoreboard scoreboard = scoreboards.remove(instanceId);
    if (scoreboard == null) {
      return;
    }
    Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (scoreboard.equals(player.getScoreboard())) {
        player.setScoreboard(main);
      }
    }
  }

  private void tag(EventInstanceId instanceId, Entity entity) {
    entity
        .getPersistentDataContainer()
        .set(SpigotPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, instanceId.toString());
    ownedEntities
        .computeIfAbsent(instanceId, ignored -> new ArrayList<>())
        .add(entity.getUniqueId());
  }

  private Location resolveLocation(EventRuntimeState state) {
    Object stored = state.platformLocation().orElse(null);
    if (stored instanceof Location location) {
      return location;
    }
    if (stored instanceof EventLocation location) {
      World world = Bukkit.getWorld(location.world());
      if (world != null) {
        return new Location(
            world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
      }
    }
    return null;
  }

  private ModelAnchor toAnchor(Location location) {
    return new ModelAnchor(
        location.getWorld().getName(),
        location.getX(),
        location.getY(),
        location.getZ(),
        location.getYaw(),
        location.getPitch());
  }

  private void requireLocation(Location location, String action) {
    if (location == null || location.getWorld() == null) {
      throw new FatalActionException(action + " requires a loaded event location");
    }
  }

  private Material material(String value) {
    Material material = Material.matchMaterial(value.replace("minecraft:", ""));
    if (material == null || material.isAir()) {
      throw new FatalActionException("Unknown item material: " + value);
    }
    return material;
  }

  private EntityType entityType(String value) {
    try {
      return EntityType.valueOf(value.replace("minecraft:", "").toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new FatalActionException("Unknown entity type: " + value, exception);
    }
  }

  private String render(String template, EventInstance instance, EventRuntimeState state) {
    return strip(
        template
            .replace("%event%", instance.definitionId().value())
            .replace("%phase%", instance.currentPhase().map(phase -> phase.value()).orElse("none"))
            .replace("%health%", String.valueOf(state.currentHealth()))
            .replace("%max_health%", String.valueOf(state.maxHealth())));
  }

  private String strip(String miniMessage) {
    return MiniMessage.miniMessage().stripTags(miniMessage);
  }

  private String normalizeBarStyle(String configured) {
    String upper = configured.toUpperCase(java.util.Locale.ROOT);
    if (upper.equals("PROGRESS")) {
      return "SOLID";
    }
    return upper.replace("NOTCHES_", "SEGMENTED_").replace("NOTCHED_", "SEGMENTED_");
  }

  private <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
    try {
      return Enum.valueOf(type, value.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return fallback;
    }
  }

  private String string(Map<String, Object> params, String key, String fallback) {
    Object value = params.get(key);
    return value == null ? fallback : String.valueOf(value);
  }

  private int integer(Map<String, Object> params, String key, int fallback) {
    return number(params.get(key), fallback);
  }

  private float decimal(Map<String, Object> params, String key, float fallback) {
    Object value = params.get(key);
    if (value instanceof Number number) {
      return number.floatValue();
    }
    try {
      return value == null ? fallback : Float.parseFloat(String.valueOf(value));
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  private int number(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return value == null ? fallback : Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  private void requireRange(int value, int minimum, int maximum, String field) {
    if (value < minimum || value > maximum) {
      throw new FatalActionException(
          field + " must be between " + minimum + " and " + maximum + ", got " + value);
    }
  }
}
