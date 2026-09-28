package io.github.kizio806.spectraevents.platform.spigot.action;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventZone;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.application.model.animation.runtime.ModelAnimationActionService;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.port.AbstractPlatformActionAdapter;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.contribution.ContributionRanking;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.platform.spigot.loot.SpigotSharedLootHolder;
import io.github.kizio806.spectraevents.platform.spigot.metadata.SpigotPdcKeys;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class SpigotActionAdapter extends AbstractPlatformActionAdapter implements Listener {
  private static final Logger LOGGER = Logger.getLogger(SpigotActionAdapter.class.getName());
  private static final int MAX_PARTICLES_PER_ACTION = 10_000;
  private static final int MAX_MOBS_PER_ACTION = 128;
  private static final int MAX_LOOT_ENTRIES = 64;
  private static final int MAX_VISIBLE_EVENT_BOSS_BARS = 3;

  private final Plugin plugin;
  private final BukkitAudiences adventure;

  private final Map<EventInstanceId, List<UUID>> ownedEntities = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, BossBar> bossBars = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, String> bossBarTemplates = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, Scoreboard> scoreboards = new ConcurrentHashMap<>();

  private ModelRuntimeService modelRuntimeService = null;
  private ModelAnimationActionService modelAnimationActionService = null;

  public SpigotActionAdapter(Plugin plugin, BukkitAudiences adventure) {
    this.plugin = plugin;
    this.adventure = adventure;
  }

  public void setModelRuntimeService(ModelRuntimeService modelRuntimeService) {
    this.modelRuntimeService = modelRuntimeService;
  }

  public void setModelAnimationActionService(
      ModelAnimationActionService modelAnimationActionService) {
    this.modelAnimationActionService = modelAnimationActionService;
  }

  /** Delivers a mailbox snapshot only if the complete snapshot fits in the player's inventory. */
  public CompletableFuture<Boolean> deliverRewardItems(Player player, List<RewardItem> items) {
    if (player == null || !player.isOnline()) {
      return CompletableFuture.completedFuture(false);
    }
    List<RewardItem> snapshot = List.copyOf(items);
    return executeSync(
        () -> {
          if (!player.isOnline()) {
            throw new IllegalStateException("Player went offline before reward delivery");
          }
          List<ItemStack> stacks = mailboxItemStacks(snapshot);
          Inventory inventory = player.getInventory();
          if (!canContainAll(inventory, stacks)) {
            throw new IllegalStateException("Player inventory cannot contain the reward claim");
          }
          Map<Integer, ItemStack> leftovers = inventory.addItem(stacks.toArray(ItemStack[]::new));
          if (!leftovers.isEmpty()) {
            throw new IllegalStateException("Player inventory changed during reward delivery");
          }
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleOpenSharedLoot(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.OpenSharedLootAction action,
      ExecutionContext context) {
    if (!(context.actor() instanceof Player player))
      return CompletableFuture.completedFuture(false);
    Inventory inventory =
        Bukkit.createInventory(new SpigotSharedLootHolder(instance.id()), 27, action.title());
    state
        .sharedLootSnapshot()
        .forEach(
            (slot, loot) -> {
              Material material = Material.matchMaterial(loot.material().replace("minecraft:", ""));
              inventory.setItem(
                  slot,
                  new ItemStack(material == null ? Material.DIAMOND : material, loot.amount()));
            });
    player.openInventory(inventory);
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public void setFatalActionHandler(BiConsumer<EventInstanceId, Throwable> handler) {
    // Spigot adapter currently does not execute async actions that require fatal error handling
  }

  public void registerRecoveredEntity(EventInstanceId instanceId, UUID entityId) {
    ownedEntities
        .computeIfAbsent(instanceId, ignored -> new java.util.concurrent.CopyOnWriteArrayList<>())
        .add(entityId);
  }

  @Override
  public void cleanupEvent(EventInstanceId instanceId) {
    Bukkit.getOnlinePlayers().stream()
        .filter(
            player ->
                player.getOpenInventory().getTopInventory().getHolder()
                        instanceof SpigotSharedLootHolder holder
                    && holder.instanceId().equals(instanceId))
        .forEach(Player::closeInventory);
    if (modelAnimationActionService != null) modelAnimationActionService.stopForEvent(instanceId);
    if (modelRuntimeService != null) {
      for (RenderedModelHandle handle : List.copyOf(modelRuntimeService.getActiveInstances())) {
        if (instanceId.equals(handle.ownerEventId())) {
          modelRuntimeService.removeModel(handle.runtimeId());
        }
      }
    }

    List<UUID> entities = ownedEntities.remove(instanceId);
    if (entities != null) {
      for (World world : Bukkit.getWorlds()) {
        for (Entity entity : world.getEntities()) {
          if (entities.contains(entity.getUniqueId())) {
            entity.remove();
          }
        }
      }
    }

    BossBar bar = bossBars.remove(instanceId);
    if (bar != null) bar.removeAll();
    bossBarTemplates.remove(instanceId);

    Scoreboard board = scoreboards.remove(instanceId);
    if (board != null) {
      Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.getScoreboard().equals(board)) {
          player.setScoreboard(main);
        }
      }
    }
  }

  @Override
  public void cleanupAll() {
    if (modelAnimationActionService != null && modelRuntimeService != null) {
      for (RenderedModelHandle handle : modelRuntimeService.getActiveInstances()) {
        modelAnimationActionService.stopForModel(handle);
      }
      for (RenderedModelHandle handle : List.copyOf(modelRuntimeService.getActiveInstances())) {
        modelRuntimeService.removeModel(handle.runtimeId());
      }
    }
    for (EventInstanceId id : List.copyOf(ownedEntities.keySet())) {
      cleanupEvent(id);
    }
  }

  @Override
  public int resourceCount(EventInstanceId instanceId) {
    int count = 0;
    List<UUID> entities = ownedEntities.get(instanceId);
    if (entities != null) count += entities.size();
    if (bossBars.containsKey(instanceId)) count++;
    if (scoreboards.containsKey(instanceId)) count++;
    if (modelRuntimeService != null) {
      count +=
          modelRuntimeService.getActiveInstances().stream()
              .filter(handle -> instanceId.equals(handle.ownerEventId()))
              .mapToInt(handle -> handle.parts().size() + handle.interactions().size())
              .sum();
    }
    return count;
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public void refreshHud(EventInstance instance, EventRuntimeState state) {
    executeSync(
        () -> {
          BossBar bar = bossBars.get(instance.id());
          if (bar != null) {
            bar.setTitle(
                renderHudTitle(
                    bossBarTemplates.getOrDefault(instance.id(), bar.getTitle()), instance, state));
            bar.setProgress(Math.max(0.0, Math.min(1.0, calculateProgress(state, "health"))));
          }
        });
  }

  private Location resolveLocation(EventRuntimeState state) {
    Object stored = state.platformLocation().orElse(null);
    if (stored instanceof Location location) return location;
    if (stored instanceof EventLocation location) {
      World world = Bukkit.getWorld(location.world());
      if (world != null)
        return new Location(
            world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
    }
    return null;
  }

  private void requireLocation(Location loc, String actionName) {
    if (loc == null)
      throw new FatalActionException(actionName + " requires a valid platform location");
  }

  private void requireRange(int value, int min, int max, String fieldName) {
    if (value < min || value > max)
      throw new FatalActionException(fieldName + " must be between " + min + " and " + max);
  }

  @Override
  protected CompletableFuture<Boolean> handleSpawnModel(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SpawnModelAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "spawn_model");
    Location spawnLoc = baseLoc.clone().add(0, action.heightOffset(), 0);
    ModelId modelId = new ModelId(action.model());

    return executeSync(
        () -> {
          if (modelRuntimeService == null)
            throw new FatalActionException("Model runtime is unavailable on Spigot");
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
          if (handle == null)
            throw new FatalActionException(
                "Failed to spawn 3D model '" + modelId.value() + "' for instance " + instance.id());
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleMoveModel(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.MoveModelAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "move_model");
    if (modelRuntimeService == null)
      throw new FatalActionException("Model runtime is unavailable on Spigot");
    return executeSync(
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

  @Override
  protected CompletableFuture<Boolean> handlePlayAnimation(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.PlayAnimationAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "play_animation");
    if (modelAnimationActionService == null)
      throw new FatalActionException("Model animation runtime is unavailable on Spigot");
    return executeSync(
        () ->
            modelAnimationActionService.play(
                instance.id(), Map.of("animation", action.animation())));
  }

  @Override
  protected CompletableFuture<Boolean> handleRemoveModel(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveModelAction action,
      ExecutionContext context) {
    if (modelRuntimeService != null) {
      for (RenderedModelHandle handle : List.copyOf(modelRuntimeService.getActiveInstances())) {
        if (instance.id().equals(handle.ownerEventId())) {
          if (modelAnimationActionService != null) modelAnimationActionService.stopForModel(handle);
          modelRuntimeService.removeModel(handle.runtimeId());
        }
      }
    }
    return CompletableFuture.completedFuture(true);
  }

  @Override
  protected CompletableFuture<Boolean> handlePlaySound(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.PlaySoundAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "play_sound");
    World world = baseLoc.getWorld();
    if (world == null) throw new FatalActionException("play_sound requires a loaded world");

    Sound sound = resolveSound(action.sound());
    return executeSync(
        () -> {
          if (sound != null) {
            world.playSound(baseLoc, sound, action.volume(), action.pitch());
          } else {
            world.playSound(baseLoc, action.sound(), action.volume(), action.pitch());
          }
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleSpawnParticles(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SpawnParticlesAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "spawn_particles");
    World world = baseLoc.getWorld();
    if (world == null) throw new FatalActionException("spawn_particles requires a loaded world");
    requireRange(action.count(), 0, MAX_PARTICLES_PER_ACTION, "spawn_particles.count");

    Particle particle = resolveParticle(action.particle());
    return executeSync(
        () -> {
          if (particle != null) world.spawnParticle(particle, baseLoc, action.count());
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleGiveItem(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.GiveItemAction action,
      ExecutionContext context) {
    if (context == null || !(context.actor() instanceof Player player))
      throw new FatalActionException("give_item requires a player interaction context");
    requireRange(action.amount(), 1, 64, "give_item.amount");

    Material mat = resolveMaterial(action.material());
    if (mat == null) throw new FatalActionException("Unknown item material: " + action.material());

    ItemStack finalStack = new ItemStack(mat, action.amount());
    return executeSync(() -> player.getInventory().addItem(finalStack));
  }

  @Override
  protected CompletableFuture<Boolean> handleSendMessage(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SendMessageAction action,
      ExecutionContext context) {
    if (context == null || !(context.actor() instanceof CommandSender sender))
      throw new FatalActionException("send_message requires a command-sender context");
    if (!action.message().isEmpty()) {
      var component = MiniMessage.miniMessage().deserialize(action.message());
      return executeSync(() -> adventure.sender(sender).sendMessage(component));
    }
    return CompletableFuture.completedFuture(true);
  }

  @Override
  protected CompletableFuture<Boolean> handleBroadcastMessage(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.BroadcastMessageAction action,
      ExecutionContext context) {
    if (!action.message().isEmpty()) {
      return executeSync(
          () -> {
            var component = MiniMessage.miniMessage().deserialize(action.message());
            adventure.all().sendMessage(component);
          });
    }
    return CompletableFuture.completedFuture(true);
  }

  @Override
  protected CompletableFuture<Boolean> handleShowTitle(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowTitleAction action,
      ExecutionContext context) {
    if (action.title().isBlank()) throw new FatalActionException("show_title requires a title");
    if (action.fadeIn() < 0 || action.stay() < 1 || action.fadeOut() < 0)
      throw new FatalActionException(
          "show_title timings must be non-negative and stay must be positive");

    Title rendered =
        Title.title(
            MiniMessage.miniMessage().deserialize(action.title()),
            MiniMessage.miniMessage().deserialize(action.subtitle()),
            Title.Times.times(
                Duration.ofMillis(action.fadeIn() * 50L),
                Duration.ofMillis(action.stay() * 50L),
                Duration.ofMillis(action.fadeOut() * 50L)));

    return executeSync(() -> adventure.all().showTitle(rendered));
  }

  @Override
  protected CompletableFuture<Boolean> handleSpawnBoss(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SpawnBossAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "spawn_boss");
    Location spawnLoc = baseLoc.clone().add(action.offsetX(), action.offsetY(), action.offsetZ());
    EntityType entityType = resolveEntityType(action.entityType());

    return executeSync(
        () -> {
          World world = spawnLoc.getWorld();
          if (world == null) return;
          Entity entity = world.spawnEntity(spawnLoc, entityType);

          // Basic Custom Name for Spigot
          entity.setCustomNameVisible(true);
          entity.setCustomName(
              action.name().replaceAll("<[^>]*>", "")); // Strip mini-message tags for legacy Spigot

          entity
              .getPersistentDataContainer()
              .set(
                  SpigotPdcKeys.EVENT_INSTANCE_ID,
                  PersistentDataType.STRING,
                  instance.id().toString());
          state.setBossEntityId(entity.getUniqueId());
          ownedEntities
              .computeIfAbsent(
                  instance.id(), ignored -> new java.util.concurrent.CopyOnWriteArrayList<>())
              .add(entity.getUniqueId());
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleDropLoot(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.DropLootAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "drop_loot");
    World world = baseLoc.getWorld();
    if (world == null) throw new FatalActionException("drop_loot requires a loaded world");
    if (action.items().size() > MAX_LOOT_ENTRIES)
      throw new FatalActionException(
          "drop_loot exceeds the limit of " + MAX_LOOT_ENTRIES + " item entries");
    requireRange((int) Math.ceil(action.radius()), 0, 128, "drop_loot.radius");

    return executeSync(
        () -> {
          ThreadLocalRandom rng = ThreadLocalRandom.current();
          for (PlatformActions.LootItem item : action.items()) {
            requireRange(item.amount(), 1, 64, "drop_loot.items.amount");
            requireRange(item.chance(), 0, 100, "drop_loot.items.chance");
            if (rng.nextInt(100) < item.chance()) {
              Material mat = resolveMaterial(item.material());
              if (mat != null) {
                double offsetX = (rng.nextDouble() - 0.5) * action.radius();
                double offsetZ = (rng.nextDouble() - 0.5) * action.radius();
                world.dropItemNaturally(
                    baseLoc.clone().add(offsetX, 0.5, offsetZ), new ItemStack(mat, item.amount()));
              }
            }
          }
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleReleaseGroundLoot(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ReleaseGroundLootAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "release_ground_loot");
    World world = baseLoc.getWorld();
    if (world == null)
      throw new FatalActionException("release_ground_loot requires a loaded world");
    requireRange((int) Math.ceil(action.radius()), 0, 128, "release_ground_loot.radius");
    return executeSync(
        () ->
            state
                .sharedLootSnapshot()
                .forEach(
                    (slot, loot) -> {
                      if (groundLootExists(world, baseLoc, instance.id(), slot, action.radius()))
                        return;
                      Material material = resolveMaterial(loot.material());
                      if (material == null) return;
                      double offsetX =
                          (ThreadLocalRandom.current().nextDouble() - 0.5d) * action.radius();
                      double offsetZ =
                          (ThreadLocalRandom.current().nextDouble() - 0.5d) * action.radius();
                      Item item =
                          world.dropItemNaturally(
                              baseLoc.clone().add(offsetX, 0.5d, offsetZ),
                              new ItemStack(material, loot.amount()));
                      item.getPersistentDataContainer()
                          .set(
                              SpigotPdcKeys.EVENT_INSTANCE_ID,
                              PersistentDataType.STRING,
                              instance.id().toString());
                      item.getPersistentDataContainer()
                          .set(SpigotPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, slot);
                    }));
  }

  private boolean groundLootExists(
      World world, Location center, EventInstanceId instanceId, int slot, double radius) {
    return world.getNearbyEntities(center, radius + 2.0d, 3.0d, radius + 2.0d).stream()
        .anyMatch(
            entity ->
                instanceId
                        .toString()
                        .equals(
                            entity
                                .getPersistentDataContainer()
                                .get(SpigotPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING))
                    && Integer.valueOf(slot)
                        .equals(
                            entity
                                .getPersistentDataContainer()
                                .get(SpigotPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER)));
  }

  @Override
  protected CompletableFuture<Boolean> handleAwardPodium(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.AwardPodiumAction action,
      ExecutionContext context) {
    EventZone zone =
        state
            .eventZone()
            .orElseThrow(() -> new FatalActionException("award_podium requires an event zone"));
    return executeSync(
        () -> {
          Set<UUID> eligible = ConcurrentHashMap.newKeySet();
          for (Player player : Bukkit.getOnlinePlayers()) {
            Location location = player.getLocation();
            if (location.getWorld() != null
                && zone.contains(
                    new EventLocation(
                        location.getWorld().getName(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        location.getYaw(),
                        location.getPitch()))) {
              eligible.add(player.getUniqueId());
            }
          }
          List<UUID> podium =
              ContributionRanking.podium(
                  state.contribution(),
                  eligible,
                  state.minimumContribution(),
                  state.contributionThresholdMillis());
          adventure
              .all()
              .sendMessage(Component.text("[SpectraEvents] Metin podium: " + podiumLabel(podium)));
          for (int place = 0; place < podium.size(); place++) {
            Player winner = Bukkit.getPlayer(podium.get(place));
            if (winner != null) {
              deliverPodiumPool(winner, action.pools().get(place));
            }
          }
        });
  }

  private void deliverPodiumPool(Player player, PlatformActions.PodiumPool pool) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (PlatformActions.LootItem item : pool.items()) {
      requireRange(item.amount(), 1, 64, "award_podium.items.amount");
      requireRange(item.chance(), 0, 100, "award_podium.items.chance");
      if (random.nextInt(100) < item.chance()) {
        Material material = resolveMaterial(item.material());
        if (material != null) {
          Map<Integer, ItemStack> leftover =
              player.getInventory().addItem(new ItemStack(material, item.amount()));
          leftover
              .values()
              .forEach(value -> player.getWorld().dropItemNaturally(player.getLocation(), value));
        }
      }
    }
  }

  private String podiumLabel(List<UUID> podium) {
    if (podium.isEmpty()) {
      return "no eligible finalists";
    }
    List<String> names = new java.util.ArrayList<>();
    for (int i = 0; i < podium.size(); i++) {
      Player player = Bukkit.getPlayer(podium.get(i));
      names.add((i + 1) + ". " + (player != null ? player.getName() : podium.get(i)));
    }
    return String.join(", ", names);
  }

  @Override
  protected CompletableFuture<Boolean> handleSpawnMobs(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SpawnMobsAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "spawn_mobs");
    int totalMobs = 0;
    for (PlatformActions.MobSpawn mob : action.mobs()) {
      requireRange(mob.amount(), 1, MAX_MOBS_PER_ACTION, "spawn_mobs.mobs.amount");
      totalMobs = Math.addExact(totalMobs, mob.amount());
    }
    if (totalMobs > MAX_MOBS_PER_ACTION)
      throw new FatalActionException(
          "spawn_mobs exceeds the limit of " + MAX_MOBS_PER_ACTION + " entities per action");
    if (!action.waveId().isBlank()) {
      state.beginWave(action.waveId());
    }

    return executeSync(
        () -> {
          World world = baseLoc.getWorld();
          if (world == null) return;
          ThreadLocalRandom rng = ThreadLocalRandom.current();
          for (PlatformActions.MobSpawn mob : action.mobs()) {
            requireRange((int) Math.ceil(mob.radius()), 0, 128, "spawn_mobs.mobs.radius");
            EntityType type = resolveEntityType(mob.entityType());
            for (int i = 0; i < mob.amount(); i++) {
              double offsetX = (rng.nextDouble() - 0.5) * mob.radius() * 2;
              double offsetZ = (rng.nextDouble() - 0.5) * mob.radius() * 2;
              Location spawnLoc = baseLoc.clone().add(offsetX, 0, offsetZ);
              Entity entity = world.spawnEntity(spawnLoc, type);
              entity.setCustomNameVisible(true);
              entity.setCustomName(mob.name().replaceAll("<[^>]*>", ""));
              entity
                  .getPersistentDataContainer()
                  .set(
                      SpigotPdcKeys.EVENT_INSTANCE_ID,
                      PersistentDataType.STRING,
                      instance.id().toString());
              if (!action.waveId().isBlank()) {
                entity
                    .getPersistentDataContainer()
                    .set(SpigotPdcKeys.WAVE_ID, PersistentDataType.STRING, action.waveId());
                state.trackWaveEntity(action.waveId(), entity.getUniqueId());
              }
              ownedEntities
                  .computeIfAbsent(
                      instance.id(), ignored -> new java.util.concurrent.CopyOnWriteArrayList<>())
                  .add(entity.getUniqueId());
            }
          }
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleShowBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowBossbarAction action,
      ExecutionContext context) {
    return executeSync(
        () -> {
          if (!bossBars.containsKey(instance.id())
              && bossBars.size() >= MAX_VISIBLE_EVENT_BOSS_BARS) {
            return;
          }
          BarColor color;
          try {
            color = BarColor.valueOf(action.color().toUpperCase(Locale.ROOT));
          } catch (Exception e) {
            color = BarColor.PURPLE;
          }

          BarStyle style;
          try {
            String upper = action.style().toUpperCase(Locale.ROOT);
            if (upper.startsWith("NOTCH_")) upper = upper.replace("NOTCH_", "SEGMENTED_");
            else if (upper.startsWith("NOTCHES_")) upper = upper.replace("NOTCHES_", "SEGMENTED_");
            else if (upper.equals("PROGRESS")) upper = "SOLID";
            style = BarStyle.valueOf(upper);
          } catch (Exception e) {
            style = BarStyle.SOLID;
          }

          double progress =
              Math.max(0.0, Math.min(1.0, calculateProgress(state, action.progress())));
          BossBar bar =
              Bukkit.createBossBar(renderHudTitle(action.title(), instance, state), color, style);
          bar.setProgress(progress);
          for (Player player : Bukkit.getOnlinePlayers()) {
            bar.addPlayer(player);
          }
          bossBars.put(instance.id(), bar);
          bossBarTemplates.put(instance.id(), action.title());
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleUpdateBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.UpdateBossbarAction action,
      ExecutionContext context) {
    return executeSync(
        () -> {
          BossBar bar = bossBars.get(instance.id());
          if (bar != null) {
            bossBarTemplates.put(instance.id(), action.title());
            bar.setTitle(renderHudTitle(action.title(), instance, state));
            bar.setProgress(
                Math.max(0.0, Math.min(1.0, calculateProgress(state, action.progress()))));
          }
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleRemoveBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveBossbarAction action,
      ExecutionContext context) {
    return executeSync(
        () -> {
          BossBar bar = bossBars.remove(instance.id());
          if (bar != null) bar.removeAll();
          bossBarTemplates.remove(instance.id());
        });
  }

  @SuppressWarnings("deprecation")
  @Override
  protected CompletableFuture<Boolean> handleShowScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowScoreboardAction action,
      ExecutionContext context) {
    return executeSync(
        () -> {
          Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
          Objective obj =
              board.registerNewObjective(
                  "event_" + instance.id().toString().substring(0, 8),
                  "dummy",
                  action.title().replaceAll("<[^>]*>", ""));
          obj.setDisplaySlot(DisplaySlot.SIDEBAR);

          List<String> lines = action.lines();
          for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int score = lines.size() - i;
            obj.getScore(line.replaceAll("<[^>]*>", "")).setScore(score);
          }

          scoreboards.put(instance.id(), board);
          for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(board);
          }
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleUpdateScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.UpdateScoreboardAction action,
      ExecutionContext context) {
    return CompletableFuture.completedFuture(true);
  }

  @Override
  protected CompletableFuture<Boolean> handleRemoveScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveScoreboardAction action,
      ExecutionContext context) {
    return executeSync(
        () -> {
          Scoreboard board = scoreboards.remove(instance.id());
          if (board != null) {
            Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Player player : Bukkit.getOnlinePlayers()) {
              if (player.getScoreboard().equals(board)) {
                player.setScoreboard(main);
              }
            }
          }
        });
  }

  private Material resolveMaterial(String name) {
    String formatted = name.replace("minecraft:", "").toUpperCase(Locale.ROOT);
    Material material = Material.matchMaterial(formatted);
    if (material == null) throw new FatalActionException("Unknown material: " + name);
    return material;
  }

  private List<ItemStack> mailboxItemStacks(List<RewardItem> items) {
    List<ItemStack> stacks = new ArrayList<>();
    for (RewardItem item : items) {
      int remaining = item.amount();
      while (remaining > 0) {
        Material material = resolveMaterial(item.material());
        int amount = Math.min(remaining, material.getMaxStackSize());
        stacks.add(new ItemStack(material, amount));
        remaining -= amount;
      }
    }
    return stacks;
  }

  private boolean canContainAll(Inventory inventory, List<ItemStack> items) {
    ItemStack[] contents = inventory.getStorageContents();
    for (int slot = 0; slot < contents.length; slot++) {
      if (contents[slot] != null) {
        contents[slot] = contents[slot].clone();
      }
    }
    for (ItemStack item : items) {
      int remaining = item.getAmount();
      for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
        ItemStack existing = contents[slot];
        if (existing == null || existing.getType().isAir() || !existing.isSimilar(item)) {
          continue;
        }
        int accepted = Math.min(existing.getMaxStackSize() - existing.getAmount(), remaining);
        existing.setAmount(existing.getAmount() + accepted);
        remaining -= accepted;
      }
      for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
        ItemStack existing = contents[slot];
        if (existing != null && !existing.getType().isAir()) {
          continue;
        }
        int accepted = Math.min(item.getMaxStackSize(), remaining);
        ItemStack placed = item.clone();
        placed.setAmount(accepted);
        contents[slot] = placed;
        remaining -= accepted;
      }
      if (remaining > 0) {
        return false;
      }
    }
    return true;
  }

  private EntityType resolveEntityType(String name) {
    try {
      return EntityType.valueOf(name.replace("minecraft:", "").toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new FatalActionException("Unknown entity type: " + name, e);
    }
  }

  @SuppressWarnings({"removal", "deprecation"})
  private Sound resolveSound(String name) {
    try {
      return Sound.valueOf(
          name.replace("minecraft:", "").replace('.', '_').toUpperCase(Locale.ROOT));
    } catch (Exception e) {
      return Sound.ENTITY_GENERIC_EXPLODE;
    }
  }

  private Particle resolveParticle(String name) {
    try {
      return Particle.valueOf(name.replace("minecraft:", "").toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new FatalActionException("Unknown particle: " + name, e);
    }
  }

  @SuppressWarnings("EmptyCatch")
  private String renderHudTitle(String template, EventInstance instance, EventRuntimeState state) {
    int maximum = Math.max(1, state.maxHealth());
    int percent = (int) (((double) state.currentHealth() / maximum) * 100.0d);
    String location =
        state
            .platformLocation()
            .filter(EventLocation.class::isInstance)
            .map(EventLocation.class::cast)
            .map(
                value ->
                    value.world()
                        + " "
                        + Math.round(value.x())
                        + ", "
                        + Math.round(value.y())
                        + ", "
                        + Math.round(value.z()))
            .orElse("unknown");
    return template
        .replace("%health%", String.valueOf(state.currentHealth()))
        .replace("%max_health%", String.valueOf(maximum))
        .replace("%health_percent%", String.valueOf(percent))
        .replace("%phase%", instance.currentPhase().map(phase -> phase.value()).orElse("active"))
        .replace("%location%", location)
        .replace("%event%", instance.definitionId().value())
        .replaceAll("<[^>]*>", "");
  }

  private double calculateProgress(EventRuntimeState state, String progressStr) {
    if ("hits".equalsIgnoreCase(progressStr) && state.hitCounter() != null) {
      var counter = state.hitCounter();
      return (double) counter.current() / (double) counter.maximum();
    }
    try {
      return Double.parseDouble(progressStr);
    } catch (NumberFormatException ignored) {
      // Ignored
    }
    if (state.maxHealth() > 0) {
      return (double) state.currentHealth() / (double) state.maxHealth();
    }
    return 1.0;
  }

  private CompletableFuture<Boolean> executeSync(Runnable action) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (Bukkit.isPrimaryThread()) {
      runGuarded(action, future);
    } else {
      Bukkit.getScheduler().runTask(plugin, () -> runGuarded(action, future));
    }
    return future;
  }

  private void runGuarded(Runnable action, CompletableFuture<Boolean> future) {
    try {
      action.run();
      future.complete(true);
    } catch (Exception e) {
      LOGGER.log(java.util.logging.Level.SEVERE, "Action error", e);
      future.complete(false);
      throw e;
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    for (BossBar bar : bossBars.values()) {
      bar.addPlayer(player);
    }
    if (!scoreboards.isEmpty()) {
      var it = scoreboards.values().iterator();
      if (it.hasNext()) {
        player.setScoreboard(it.next());
      }
    }
  }
}
