package io.github.kizio806.spectraevents.platform.paper.action;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.application.model.animation.runtime.ModelAnimationActionService;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.platform.paper.bossbar.EventBossBarManager;
import io.github.kizio806.spectraevents.platform.paper.integration.MiniPlaceholdersIntegration;
import io.github.kizio806.spectraevents.platform.paper.integration.item.CustomItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.ItemsAdderItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.NexoItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.OraxenItemProvider;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import io.github.kizio806.spectraevents.platform.paper.metadata.SpectraPdcKeys;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.github.kizio806.spectraevents.platform.paper.scoreboard.EventScoreboardManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Paper platform action adapter implementing {@link PlatformActionPort}. */
public final class PaperActionAdapter implements PlatformActionPort {
  private static final Logger LOGGER = Logger.getLogger(PaperActionAdapter.class.getName());
  private static final int MAX_PARTICLES_PER_ACTION = 10_000;
  private static final int MAX_MOBS_PER_ACTION = 128;
  private static final int MAX_LOOT_ENTRIES = 64;

  private final RegionTaskScheduler regionScheduler;
  private final PaperResourceCleaner cleaner;
  private final EventBossBarManager bossBarManager;
  private final EventScoreboardManager scoreboardManager;
  private final List<CustomItemProvider> itemProviders = new ArrayList<>();
  private ModelRuntimeService modelRuntimeService;
  private ModelAnimationActionService modelAnimationActionService;
  private BiConsumer<EventInstanceId, Throwable> fatalActionHandler =
      (instanceId, throwable) ->
          LOGGER.severe(
              "Asynchronous action failed for " + instanceId + ": " + throwable.getMessage());

  public PaperActionAdapter(RegionTaskScheduler regionScheduler, PaperResourceCleaner cleaner) {
    this.regionScheduler = Objects.requireNonNull(regionScheduler, "regionScheduler");
    this.cleaner = Objects.requireNonNull(cleaner, "cleaner");
    this.bossBarManager = new EventBossBarManager(regionScheduler);
    this.scoreboardManager = new EventScoreboardManager(regionScheduler);

    itemProviders.add(new NexoItemProvider());
    itemProviders.add(new OraxenItemProvider());
    itemProviders.add(new ItemsAdderItemProvider());
  }

  public EventBossBarManager bossBarManager() {
    return bossBarManager;
  }

  public EventScoreboardManager scoreboardManager() {
    return scoreboardManager;
  }

  public void setModelRuntimeService(ModelRuntimeService modelRuntimeService) {
    this.modelRuntimeService = modelRuntimeService;
  }

  public void setModelAnimationActionService(
      ModelAnimationActionService modelAnimationActionService) {
    this.modelAnimationActionService = modelAnimationActionService;
  }

  @Override
  public void setFatalActionHandler(BiConsumer<EventInstanceId, Throwable> handler) {
    this.fatalActionHandler = Objects.requireNonNull(handler, "handler");
  }

  @Override
  public void cleanupEvent(EventInstanceId instanceId) {
    if (modelAnimationActionService != null) {
      modelAnimationActionService.stopForEvent(instanceId);
    }
    cleaner.cleanup(instanceId);
  }

  @Override
  public void cleanupAll() {
    if (modelAnimationActionService != null && modelRuntimeService != null) {
      for (RenderedModelHandle handle : modelRuntimeService.getActiveInstances()) {
        modelAnimationActionService.stopForModel(handle);
      }
    }
    cleaner.cleanupAll();
  }

  @Override
  public int resourceCount(EventInstanceId instanceId) {
    return cleaner.resourceCount(instanceId);
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
    String type = action.type().toLowerCase(Locale.ROOT);
    Map<String, Object> params = action.parameters();

    Location baseLoc = resolveLocation(state);

    switch (type) {
      case "spawn_model" -> handleSpawnModel(instance, params, baseLoc);
      case "move_model" -> handleMoveModel(instance, baseLoc);
      case "remove_model" -> removeModels(instance.id());
      case "play_animation", "play-animation" -> handlePlayAnimation(instance, params, baseLoc);
      case "play_sound" -> handlePlaySound(instance, params, baseLoc);
      case "spawn_particles" -> handleSpawnParticles(instance, params, baseLoc);
      case "give_item" -> handleGiveItem(instance, params, context);
      case "drop_loot" -> handleDropLoot(instance, params, baseLoc);
      case "send_message" -> handleSendMessage(instance, params, context);
      case "broadcast_message", "broadcast" -> handleBroadcastMessage(instance, params);
      case "spawn_boss", "spawn_entity" -> handleSpawnBoss(instance, state, params, baseLoc);
      case "spawn_mobs", "spawn_wave" -> handleSpawnMobs(instance, params, baseLoc);
      case "show_bossbar", "create_bossbar" ->
          executeGlobal(
              instance.id(),
              () -> {
                bossBarManager.showBossBar(instance, state, params);
                cleaner.registerCustomCleanup(
                    instance.id(),
                    () ->
                        executeGlobal(
                            instance.id(),
                            () -> bossBarManager.removeBossBar(instance.id().value())));
              });
      case "update_bossbar" ->
          executeGlobal(instance.id(), () -> bossBarManager.updateBossBar(instance, state, params));
      case "remove_bossbar" ->
          executeGlobal(instance.id(), () -> bossBarManager.removeBossBar(instance.id().value()));
      case "show_scoreboard", "create_scoreboard" ->
          executeGlobal(
              instance.id(),
              () -> {
                scoreboardManager.showScoreboard(instance, state, params);
                cleaner.registerCustomCleanup(
                    instance.id(),
                    () ->
                        executeGlobal(
                            instance.id(),
                            () -> scoreboardManager.removeScoreboard(instance.id().value())));
              });
      case "update_scoreboard" ->
          executeGlobal(
              instance.id(), () -> scoreboardManager.updateScoreboard(instance, state, params));
      case "remove_scoreboard" ->
          executeGlobal(
              instance.id(), () -> scoreboardManager.removeScoreboard(instance.id().value()));
      default ->
          throw new FatalActionException(
              "Unsupported Paper action type '" + type + "' for instance " + instance.id());
    }
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

  private void handleSpawnModel(
      EventInstance instance, Map<String, Object> params, Location baseLoc) {
    if (baseLoc == null) {
      throw new FatalActionException(
          "Cannot spawn model: platform location reference is null for instance " + instance.id());
    }

    String modelIdStr = getStringParam(params, "model", "meteor");
    int defaultHeightOffset = "meteor".equalsIgnoreCase(modelIdStr) ? 20 : 0;
    int heightOffset = getIntParam(params, "height-offset", defaultHeightOffset);
    if (!params.containsKey("height-offset") && params.containsKey("height_offset")) {
      heightOffset = getIntParam(params, "height_offset", defaultHeightOffset);
    }

    Location spawnLoc = baseLoc.clone().add(0, heightOffset, 0);
    ModelId modelId = new ModelId(modelIdStr);

    executeAt(
        instance.id(),
        spawnLoc,
        () -> {
          if (modelRuntimeService == null) {
            throw new FatalActionException("Model runtime is unavailable on Paper");
          }

          ModelAnchor anchor =
              new ModelAnchor(
                  spawnLoc.getWorld().getName(),
                  spawnLoc.getX(),
                  spawnLoc.getY(),
                  spawnLoc.getZ(),
                  spawnLoc.getYaw(),
                  spawnLoc.getPitch());

          RenderedModelHandle handle =
              modelRuntimeService.spawnModel(modelId, anchor, instance.id());

          if (handle != null) {
            cleaner.registerCustomCleanup(
                instance.id(),
                () -> {
                  if (modelAnimationActionService != null) {
                    modelAnimationActionService.stopForModel(handle);
                  }
                  modelRuntimeService.removeModel(handle.runtimeId());
                });
          } else {
            throw new FatalActionException(
                "Failed to spawn 3D model '" + modelId.value() + "' for instance " + instance.id());
          }
        });
  }

  private void handleMoveModel(EventInstance instance, Location baseLoc) {
    requireLocation(baseLoc, "move_model");
    if (modelRuntimeService == null) {
      throw new FatalActionException("Model runtime is unavailable on Paper");
    }
    executeAt(
        instance.id(),
        baseLoc,
        () -> {
          ModelAnchor anchor =
              new ModelAnchor(
                  baseLoc.getWorld().getName(),
                  baseLoc.getX(),
                  baseLoc.getY(),
                  baseLoc.getZ(),
                  baseLoc.getYaw(),
                  baseLoc.getPitch());
          for (RenderedModelHandle handle : modelRuntimeService.getActiveInstances()) {
            if (instance.id().equals(handle.ownerEventId())) {
              modelRuntimeService.updateModelTransform(handle.runtimeId(), anchor);
            }
          }
        });
  }

  private void handlePlayAnimation(
      EventInstance instance, Map<String, Object> params, Location baseLoc) {
    requireLocation(baseLoc, "play_animation");
    if (modelAnimationActionService == null) {
      throw new FatalActionException("Model animation runtime is unavailable on Paper");
    }
    executeAt(
        instance.id(), baseLoc, () -> modelAnimationActionService.play(instance.id(), params));
  }

  private void removeModels(EventInstanceId instanceId) {
    if (modelRuntimeService == null) {
      return;
    }
    for (RenderedModelHandle handle : List.copyOf(modelRuntimeService.getActiveInstances())) {
      if (instanceId.equals(handle.ownerEventId())) {
        if (modelAnimationActionService != null) {
          modelAnimationActionService.stopForModel(handle);
        }
        modelRuntimeService.removeModel(handle.runtimeId());
      }
    }
  }

  private void handlePlaySound(
      EventInstance instance, Map<String, Object> params, Location baseLoc) {
    requireLocation(baseLoc, "play_sound");
    World world = baseLoc.getWorld();
    if (world == null) throw new FatalActionException("play_sound requires a loaded world");

    String soundName = getStringParam(params, "sound", "minecraft:entity.generic.explode");
    float volume = getFloatParam(params, "volume", 1.0f);
    float pitch = getFloatParam(params, "pitch", 1.0f);

    Sound sound = resolveSound(soundName);
    executeAt(
        instance.id(),
        baseLoc,
        () -> {
          if (sound != null) {
            world.playSound(baseLoc, sound, volume, pitch);
          } else {
            world.playSound(baseLoc, soundName, volume, pitch);
          }
        });
  }

  private void handleSpawnParticles(
      EventInstance instance, Map<String, Object> params, Location baseLoc) {
    requireLocation(baseLoc, "spawn_particles");
    World world = baseLoc.getWorld();
    if (world == null) throw new FatalActionException("spawn_particles requires a loaded world");

    String particleName = getStringParam(params, "particle", "minecraft:explosion");
    int count = getIntParam(params, "count", 10);
    requireRange(count, 0, MAX_PARTICLES_PER_ACTION, "spawn_particles.count");

    Particle particle = resolveParticle(particleName);
    executeAt(
        instance.id(),
        baseLoc,
        () -> {
          if (particle != null) {
            world.spawnParticle(particle, baseLoc, count);
          }
        });
  }

  private void handleGiveItem(
      EventInstance instance, Map<String, Object> params, ExecutionContext context) {
    if (context == null || !(context.actor() instanceof Player player)) {
      throw new FatalActionException("give_item requires a player interaction context");
    }
    String materialName = getStringParam(params, "material", "minecraft:diamond");
    int amount = getIntParam(params, "amount", 1);
    requireRange(amount, 1, 64, "give_item.amount");

    ItemStack itemStack = null;

    for (CustomItemProvider provider : itemProviders) {
      if (provider.isAvailable()) {
        ItemStack custom = provider.resolveItem(materialName, amount);
        if (custom != null) {
          itemStack = custom;
          break;
        }
      }
    }

    if (itemStack == null) {
      Material mat = resolveMaterial(materialName);
      if (mat != null) {
        itemStack = new ItemStack(mat, amount);
      }
    }

    if (itemStack != null) {
      ItemStack finalStack = itemStack;
      executeFor(
          instance.id(),
          player,
          () -> {
            player.getInventory().addItem(finalStack);
          });
    } else {
      throw new FatalActionException("Unknown item material or provider item: " + materialName);
    }
  }

  private void handleSendMessage(
      EventInstance instance, Map<String, Object> params, ExecutionContext context) {
    if (context == null || !(context.actor() instanceof CommandSender sender)) {
      throw new FatalActionException("send_message requires a command-sender context");
    }
    String msg = getStringParam(params, "message", "");
    if (!msg.isEmpty()) {
      var component = MiniPlaceholdersIntegration.getMiniMessage().deserialize(msg);
      if (sender instanceof Player player) {
        executeFor(instance.id(), player, () -> player.sendMessage(component));
      } else {
        sender.sendMessage(component);
      }
    }
  }

  private void handleBroadcastMessage(EventInstance instance, Map<String, Object> params) {
    String msg = getStringParam(params, "message", "");
    if (!msg.isEmpty()) {
      executeGlobal(
          instance.id(),
          () -> Bukkit.broadcast(MiniPlaceholdersIntegration.getMiniMessage().deserialize(msg)));
    }
  }

  private void handleSpawnBoss(
      EventInstance instance,
      EventRuntimeState state,
      Map<String, Object> params,
      Location baseLoc) {
    requireLocation(baseLoc, "spawn_boss");

    int offsetX = getIntParam(params, "offset-x", 2);
    int offsetY = getIntParam(params, "offset-y", 0);
    int offsetZ = getIntParam(params, "offset-z", 0);
    String typeName = getStringParam(params, "entity_type", "minecraft:zombie");
    String name = getStringParam(params, "name", "<red>Boss");

    Location spawnLoc = baseLoc.clone().add(offsetX, offsetY, offsetZ);
    EntityType entityType = resolveEntityType(typeName);

    executeAt(
        instance.id(),
        spawnLoc,
        () -> {
          World world = spawnLoc.getWorld();
          if (world == null) return;

          Entity entity = world.spawnEntity(spawnLoc, entityType);
          entity.customName(MiniPlaceholdersIntegration.getMiniMessage().deserialize(name));
          entity.setCustomNameVisible(true);

          entity
              .getPersistentDataContainer()
              .set(SpectraPdcKeys.INSTANCE_ID, PersistentDataType.STRING, instance.id().toString());

          state.setBossEntityId(entity.getUniqueId());

          cleaner.registerCustomCleanup(
              instance.id(),
              () -> {
                if (entity.isValid()) {
                  entity.remove();
                }
              });
        });
  }

  private Material resolveMaterial(String name) {
    String formatted = name.replace("minecraft:", "").toUpperCase(Locale.ROOT);
    Material material = Material.matchMaterial(formatted);
    if (material == null) {
      throw new FatalActionException("Unknown material: " + name);
    }
    return material;
  }

  private EntityType resolveEntityType(String name) {
    try {
      String formatted = name.replace("minecraft:", "").toUpperCase(Locale.ROOT);
      return EntityType.valueOf(formatted);
    } catch (IllegalArgumentException e) {
      throw new FatalActionException("Unknown entity type: " + name, e);
    }
  }

  @SuppressWarnings("removal")
  private Sound resolveSound(String name) {
    try {
      String formatted = name.replace("minecraft:", "").replace('.', '_').toUpperCase(Locale.ROOT);
      return Sound.valueOf(formatted);
    } catch (Exception e) {
      return Sound.ENTITY_GENERIC_EXPLODE;
    }
  }

  private Particle resolveParticle(String name) {
    try {
      String formatted = name.replace("minecraft:", "").toUpperCase(Locale.ROOT);
      return Particle.valueOf(formatted);
    } catch (IllegalArgumentException e) {
      throw new FatalActionException("Unknown particle: " + name, e);
    }
  }

  private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
    Object val = params.get(key);
    return val != null ? String.valueOf(val) : defaultValue;
  }

  private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
    Object val = params.get(key);
    if (val == null) return defaultValue;
    if (val instanceof Number n) return n.intValue();
    try {
      return Integer.parseInt(String.valueOf(val));
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private float getFloatParam(Map<String, Object> params, String key, float defaultValue) {
    Object val = params.get(key);
    if (val == null) return defaultValue;
    if (val instanceof Number n) return n.floatValue();
    try {
      return Float.parseFloat(String.valueOf(val));
    } catch (Exception e) {
      return defaultValue;
    }
  }

  @SuppressWarnings("unchecked")
  private void handleDropLoot(
      EventInstance instance, Map<String, Object> params, Location baseLoc) {
    requireLocation(baseLoc, "drop_loot");
    World world = baseLoc.getWorld();
    if (world == null) throw new FatalActionException("drop_loot requires a loaded world");

    double radius = getFloatParam(params, "radius", 2.0f);
    Object itemsObj = params.get("items");
    if (!(itemsObj instanceof List<?> itemsList)) {
      throw new FatalActionException("drop_loot requires an items list");
    }
    if (itemsList.size() > MAX_LOOT_ENTRIES) {
      throw new FatalActionException(
          "drop_loot exceeds the limit of " + MAX_LOOT_ENTRIES + " item entries");
    }
    requireRange((int) Math.ceil(radius), 0, 128, "drop_loot.radius");

    executeAt(
        instance.id(),
        baseLoc,
        () -> {
          ThreadLocalRandom rng = ThreadLocalRandom.current();
          for (Object itemObj : itemsList) {
            if (!(itemObj instanceof Map<?, ?> itemMap)) continue;
            Object matObj = itemMap.get("material");
            String materialName = matObj != null ? String.valueOf(matObj) : "minecraft:diamond";
            int amount = 1;
            if (itemMap.containsKey("amount")) {
              amount = Integer.parseInt(String.valueOf(itemMap.get("amount")));
            }
            requireRange(amount, 1, 64, "drop_loot.items.amount");
            int chance = 100;
            if (itemMap.containsKey("chance")) {
              chance = Integer.parseInt(String.valueOf(itemMap.get("chance")));
            }
            requireRange(chance, 0, 100, "drop_loot.items.chance");

            if (rng.nextInt(100) < chance) {
              ItemStack stack = resolveItemStack(materialName, amount);
              if (stack != null) {
                double offsetX = (rng.nextDouble() - 0.5) * radius;
                double offsetZ = (rng.nextDouble() - 0.5) * radius;
                Location dropLoc = baseLoc.clone().add(offsetX, 0.5, offsetZ);
                world.dropItemNaturally(dropLoc, stack);
              }
            }
          }
        });
  }

  private ItemStack resolveItemStack(String materialName, int amount) {
    for (CustomItemProvider provider : itemProviders) {
      if (provider.isAvailable()) {
        ItemStack custom = provider.resolveItem(materialName, amount);
        if (custom != null) return custom;
      }
    }
    Material mat = resolveMaterial(materialName);
    return mat != null ? new ItemStack(mat, amount) : null;
  }

  @SuppressWarnings("unchecked")
  private void handleSpawnMobs(
      EventInstance instance, Map<String, Object> params, Location baseLoc) {
    requireLocation(baseLoc, "spawn_mobs");

    Object mobsObj = params.get("mobs");
    if (!(mobsObj instanceof List<?> mobsList)) {
      throw new FatalActionException("spawn_mobs requires a mobs list");
    }
    int totalMobs = 0;
    for (Object mobObj : mobsList) {
      if (!(mobObj instanceof Map<?, ?> mobMap)) {
        throw new FatalActionException("spawn_mobs entries must be objects");
      }
      int amount =
          mobMap.containsKey("amount") ? Integer.parseInt(String.valueOf(mobMap.get("amount"))) : 1;
      requireRange(amount, 1, MAX_MOBS_PER_ACTION, "spawn_mobs.mobs.amount");
      totalMobs = Math.addExact(totalMobs, amount);
    }
    if (totalMobs > MAX_MOBS_PER_ACTION) {
      throw new FatalActionException(
          "spawn_mobs exceeds the limit of " + MAX_MOBS_PER_ACTION + " entities per action");
    }

    executeAt(
        instance.id(),
        baseLoc,
        () -> {
          World world = baseLoc.getWorld();
          if (world == null) return;
          ThreadLocalRandom rng = ThreadLocalRandom.current();

          for (Object mobObj : mobsList) {
            if (!(mobObj instanceof Map<?, ?> mobMap)) continue;
            Object typeObj = mobMap.get("entity_type");
            String entityTypeStr = typeObj != null ? String.valueOf(typeObj) : "minecraft:zombie";
            Object nameObj = mobMap.get("name");
            String name = nameObj != null ? String.valueOf(nameObj) : "<red>Mob";
            int amount = 1;
            if (mobMap.containsKey("amount")) {
              amount = Integer.parseInt(String.valueOf(mobMap.get("amount")));
            }
            double radius = 3.0;
            if (mobMap.containsKey("radius")) {
              radius = Double.parseDouble(String.valueOf(mobMap.get("radius")));
            }
            requireRange((int) Math.ceil(radius), 0, 128, "spawn_mobs.mobs.radius");

            EntityType type = resolveEntityType(entityTypeStr);
            for (int i = 0; i < amount; i++) {
              double offsetX = (rng.nextDouble() - 0.5) * radius * 2;
              double offsetZ = (rng.nextDouble() - 0.5) * radius * 2;
              Location spawnLoc = baseLoc.clone().add(offsetX, 0, offsetZ);

              Entity mob = world.spawnEntity(spawnLoc, type);
              mob.customName(MiniPlaceholdersIntegration.getMiniMessage().deserialize(name));
              mob.setCustomNameVisible(true);
              mob.getPersistentDataContainer()
                  .set(
                      SpectraPdcKeys.INSTANCE_ID,
                      PersistentDataType.STRING,
                      instance.id().toString());

              cleaner.registerCustomCleanup(
                  instance.id(),
                  () -> {
                    if (mob.isValid()) mob.remove();
                  });
            }
          }
        });
  }

  private void executeAt(EventInstanceId instanceId, Location location, Runnable action) {
    try {
      regionScheduler.executeAt(location, () -> runGuarded(instanceId, action));
    } catch (RuntimeException exception) {
      runFailureHandler(instanceId, exception);
      throw exception;
    }
  }

  private void executeGlobal(EventInstanceId instanceId, Runnable action) {
    try {
      regionScheduler.executeGlobal(() -> runGuarded(instanceId, action));
    } catch (RuntimeException exception) {
      runFailureHandler(instanceId, exception);
      throw exception;
    }
  }

  private void executeFor(EventInstanceId instanceId, Player player, Runnable action) {
    try {
      regionScheduler.executeFor(player, () -> runGuarded(instanceId, action));
    } catch (RuntimeException exception) {
      runFailureHandler(instanceId, exception);
      throw exception;
    }
  }

  private void runGuarded(EventInstanceId instanceId, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException throwable) {
      runFailureHandler(instanceId, throwable);
    }
  }

  private void runFailureHandler(EventInstanceId instanceId, Throwable throwable) {
    LOGGER.log(
        java.util.logging.Level.SEVERE,
        "Asynchronous platform action failed for event " + instanceId,
        throwable);
    try {
      fatalActionHandler.accept(instanceId, throwable);
    } catch (RuntimeException handlerFailure) {
      LOGGER.severe(
          "Could not fail event "
              + instanceId
              + " after action error: "
              + handlerFailure.getMessage());
    }
  }

  private void requireLocation(Location location, String action) {
    if (location == null) {
      throw new FatalActionException(action + " requires a persisted event location");
    }
  }

  private void requireRange(int value, int minimum, int maximum, String field) {
    if (value < minimum || value > maximum) {
      throw new FatalActionException(
          field + " must be between " + minimum + " and " + maximum + ", got " + value);
    }
  }
}
