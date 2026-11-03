package io.github.kizio806.spectraevents.platform.bukkit.action;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
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
import io.github.kizio806.spectraevents.platform.bukkit.concurrent.AsyncFutures;
import io.github.kizio806.spectraevents.platform.bukkit.integration.item.CustomItemProvider;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootHolder;
import io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.logging.Level;
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
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reusable, shared Bukkit platform action adapter providing unified handling for models, entities,
 * items, loot, particles, sounds, and messaging across Paper and Spigot.
 */
public abstract class AbstractBukkitActionAdapter extends AbstractPlatformActionAdapter {

  protected static final int MAX_PARTICLES_PER_ACTION = 10_000;
  protected static final int MAX_MOBS_PER_ACTION = 128;
  protected static final int MAX_LOOT_ENTRIES = 64;

  protected final Logger logger = Logger.getLogger(getClass().getName());
  protected final LocaleCatalog locales;
  protected final List<CustomItemProvider> itemProviders = new ArrayList<>();

  protected ModelRuntimeService modelRuntimeService;
  protected ModelAnimationActionService modelAnimationActionService;
  protected volatile boolean shuttingDown;
  protected BiConsumer<EventInstanceId, Throwable> fatalActionHandler =
      (instanceId, throwable) ->
          logger.severe(
              "Asynchronous action failed for " + instanceId + ": " + throwable.getMessage());

  protected AbstractBukkitActionAdapter(LocaleCatalog locales) {
    this.locales = Objects.requireNonNull(locales, "locales");
    this.shuttingDown = false;
  }

  public void beginShutdown() {
    this.shuttingDown = true;
    stopAllModelAnimations();
  }

  public boolean isShuttingDown() {
    return shuttingDown;
  }

  public void setModelRuntimeService(ModelRuntimeService modelRuntimeService) {
    this.modelRuntimeService = modelRuntimeService;
  }

  public void setModelAnimationActionService(
      ModelAnimationActionService modelAnimationActionService) {
    this.modelAnimationActionService = modelAnimationActionService;
  }

  public ModelRuntimeService modelRuntimeService() {
    return modelRuntimeService;
  }

  public ModelAnimationActionService modelAnimationActionService() {
    return modelAnimationActionService;
  }

  @Override
  public void setFatalActionHandler(BiConsumer<EventInstanceId, Throwable> handler) {
    this.fatalActionHandler = Objects.requireNonNull(handler, "handler");
  }

  public void registerItemProvider(CustomItemProvider provider) {
    itemProviders.add(Objects.requireNonNull(provider, "provider"));
  }

  // --- Abstract Platform Hooks ---

  protected abstract CompletableFuture<Boolean> executeAt(
      EventInstanceId instanceId, Location location, Runnable action);

  protected abstract CompletableFuture<Boolean> executeGlobal(
      EventInstanceId instanceId, Runnable action);

  protected abstract CompletableFuture<Boolean> executeFor(
      EventInstanceId instanceId, Player player, Runnable action);

  protected abstract void registerSpawnedEntity(EventInstanceId instanceId, Entity entity);

  protected void registerSpawnedModel(EventInstanceId instanceId, RenderedModelHandle handle) {}

  protected abstract void setEntityCustomName(
      Entity entity, String template, EventInstance instance, EventRuntimeState state);

  protected abstract void sendPlayerMessage(
      CommandSender sender,
      String messageTemplate,
      EventInstance instance,
      EventRuntimeState state);

  protected abstract void broadcastMessage(
      String messageTemplate, EventInstance instance, EventRuntimeState state);

  protected abstract void broadcastRawMessage(String rawMessage);

  protected abstract void showTitle(
      String titleTemplate,
      String subtitleTemplate,
      int fadeIn,
      int stay,
      int fadeOut,
      EventInstance instance,
      EventRuntimeState state);

  protected abstract Inventory createSharedLootInventory(
      BukkitSharedLootHolder holder,
      int size,
      String titleTemplate,
      EventInstance instance,
      EventRuntimeState state);

  // --- Shared Action Implementations ---

  @Override
  public CompletableFuture<Boolean> deliverRewardOrDrop(UUID playerId, List<RewardItem> items) {
    Player player = Bukkit.getPlayer(Objects.requireNonNull(playerId, "playerId"));
    return player == null
        ? CompletableFuture.completedFuture(false)
        : deliverRewardItems(player, items);
  }

  public CompletableFuture<Boolean> deliverRewardItems(Player player, List<RewardItem> items) {
    Objects.requireNonNull(player, "player");
    List<RewardItem> snapshot = List.copyOf(Objects.requireNonNull(items, "items"));
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (shuttingDown || !player.isOnline()) {
      future.complete(false);
      return future;
    }
    try {
      return executeFor(
          null,
          player,
          () -> {
            if (!player.isOnline()) {
              return;
            }
            List<ItemStack> stacks = mailboxItemStacks(snapshot);
            Inventory inventory = player.getInventory();
            if (!canContainAll(inventory, stacks)) {
              for (ItemStack stack : stacks) {
                player.getWorld().dropItem(player.getLocation(), stack);
              }
              return;
            }
            Map<Integer, ItemStack> leftovers = inventory.addItem(stacks.toArray(ItemStack[]::new));
            if (!leftovers.isEmpty()) {
              for (ItemStack stack : leftovers.values()) {
                player.getWorld().dropItem(player.getLocation(), stack);
              }
            }
          });
    } catch (RuntimeException exception) {
      logger.log(Level.WARNING, "Could not schedule reward mailbox delivery", exception);
      future.complete(false);
      return future;
    }
  }

  @Override
  protected CompletableFuture<Boolean> handleOpenSharedLoot(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.OpenSharedLootAction action,
      ExecutionContext context) {
    if (!(context.actor() instanceof Player player)) {
      return CompletableFuture.completedFuture(false);
    }
    return executeFor(
        instance.id(),
        player,
        () -> {
          Inventory inventory =
              createSharedLootInventory(
                  new BukkitSharedLootHolder(instance.id()), 27, action.title(), instance, state);
          state
              .sharedLootSnapshot()
              .forEach(
                  (slot, loot) -> {
                    ItemStack stack = resolveLootItemStack(loot.material(), loot.amount());
                    inventory.setItem(slot, stack);
                  });
          player.openInventory(inventory);
        });
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

    return executeAt(
        instance.id(),
        spawnLoc,
        () -> {
          if (modelRuntimeService == null) {
            throw new FatalActionException("Model runtime is unavailable on this platform");
          }
          ModelAnchor anchor =
              new ModelAnchor(
                  Objects.requireNonNull(spawnLoc.getWorld(), "spawnLoc.world").getName(),
                  spawnLoc.getX(),
                  spawnLoc.getY(),
                  spawnLoc.getZ(),
                  spawnLoc.getYaw(),
                  spawnLoc.getPitch());
          RenderedModelHandle handle =
              modelRuntimeService.spawnModel(modelId, anchor, instance.id());
          if (handle != null) {
            registerSpawnedModel(instance.id(), handle);
          } else {
            throw new FatalActionException(
                "Failed to spawn 3D model '" + modelId.value() + "' for instance " + instance.id());
          }
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
    if (modelRuntimeService == null) {
      throw new FatalActionException("Model runtime is unavailable on this platform");
    }
    return executeAt(
        instance.id(),
        baseLoc,
        () -> {
          ModelAnchor anchor =
              new ModelAnchor(
                  Objects.requireNonNull(baseLoc.getWorld(), "baseLoc.world").getName(),
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
    if (modelAnimationActionService == null) {
      throw new FatalActionException("Model animation service is unavailable on this platform");
    }
    return executeAt(
        instance.id(),
        baseLoc,
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
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    if (modelRuntimeService != null) {
      for (RenderedModelHandle handle : List.copyOf(modelRuntimeService.getActiveInstances())) {
        if (instance.id().equals(handle.ownerEventId())) {
          if (modelAnimationActionService != null) {
            modelAnimationActionService.stopForModel(handle);
          }
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
    if (world == null) {
      throw new FatalActionException("play_sound requires a loaded world");
    }

    Sound sound = resolveSound(action.sound());
    return executeAt(
        instance.id(),
        baseLoc,
        () -> {
          try {
            if (sound != null) {
              world.playSound(baseLoc, sound, action.volume(), action.pitch());
            } else {
              world.playSound(baseLoc, action.sound(), action.volume(), action.pitch());
            }
          } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to play sound: " + action.sound(), e);
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
    if (world == null) {
      throw new FatalActionException("spawn_particles requires a loaded world");
    }
    requireRange(action.count(), 0, MAX_PARTICLES_PER_ACTION, "spawn_particles.count");

    Particle particle = resolveParticle(action.particle());
    return executeAt(
        instance.id(),
        baseLoc,
        () -> {
          try {
            if (particle != null) {
              world.spawnParticle(particle, baseLoc, action.count());
            }
          } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to spawn particle: " + action.particle(), e);
          }
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleGiveItem(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.GiveItemAction action,
      ExecutionContext context) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    if (context == null || !(context.actor() instanceof Player player)) {
      throw new FatalActionException("give_item requires a player interaction context");
    }
    requireRange(action.amount(), 1, 64, "give_item.amount");

    ItemStack itemStack = resolveItemStack(action.material(), action.amount());
    if (itemStack != null) {
      ItemStack finalStack = itemStack;
      return executeFor(instance.id(), player, () -> player.getInventory().addItem(finalStack));
    } else {
      throw new FatalActionException(
          "Unknown item material or provider item: " + action.material());
    }
  }

  @Override
  protected CompletableFuture<Boolean> handleSendMessage(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SendMessageAction action,
      ExecutionContext context) {
    if (context == null || !(context.actor() instanceof CommandSender sender)) {
      throw new FatalActionException("send_message requires a command-sender context");
    }
    if (!action.message().isEmpty()) {
      if (sender instanceof Player player) {
        return executeFor(
            instance.id(),
            player,
            () -> sendPlayerMessage(player, action.message(), instance, state));
      } else {
        sendPlayerMessage(sender, action.message(), instance, state);
      }
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
      return executeGlobal(
          instance.id(), () -> broadcastMessage(action.message(), instance, state));
    }
    return CompletableFuture.completedFuture(true);
  }

  @Override
  protected CompletableFuture<Boolean> handleShowTitle(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowTitleAction action,
      ExecutionContext context) {
    if (action.title().isBlank()) {
      throw new FatalActionException("show_title requires a title");
    }
    if (action.fadeIn() < 0 || action.stay() < 1 || action.fadeOut() < 0) {
      throw new FatalActionException(
          "show_title timings must be non-negative and stay must be positive");
    }
    return executeGlobal(
        instance.id(),
        () ->
            showTitle(
                action.title(),
                action.subtitle(),
                action.fadeIn(),
                action.stay(),
                action.fadeOut(),
                instance,
                state));
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

    return executeAt(
        instance.id(),
        spawnLoc,
        () -> {
          World world = spawnLoc.getWorld();
          if (world == null) {
            return;
          }
          Entity entity = world.spawnEntity(spawnLoc, entityType);
          setEntityCustomName(entity, action.name(), instance, state);
          entity.setCustomNameVisible(true);
          entity
              .getPersistentDataContainer()
              .set(
                  BukkitPdcKeys.EVENT_INSTANCE_ID,
                  PersistentDataType.STRING,
                  instance.id().toString());
          state.setBossEntityId(entity.getUniqueId());
          registerSpawnedEntity(instance.id(), entity);
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
    if (world == null) {
      throw new FatalActionException("drop_loot requires a loaded world");
    }
    if (action.items().size() > MAX_LOOT_ENTRIES) {
      throw new FatalActionException(
          "drop_loot exceeds the limit of " + MAX_LOOT_ENTRIES + " item entries");
    }
    requireRange((int) Math.ceil(action.radius()), 0, 128, "drop_loot.radius");

    return executeAt(
        instance.id(),
        baseLoc,
        () -> {
          ThreadLocalRandom rng = ThreadLocalRandom.current();
          for (PlatformActions.LootItem item : action.items()) {
            requireRange(item.amount(), 1, 64, "drop_loot.items.amount");
            requireRange(item.chance(), 0, 100, "drop_loot.items.chance");
            if (rng.nextInt(100) < item.chance()) {
              ItemStack stack = resolveItemStack(item.material(), item.amount());
              if (stack != null) {
                double offsetX = (rng.nextDouble() - 0.5) * action.radius();
                double offsetZ = (rng.nextDouble() - 0.5) * action.radius();
                world.dropItemNaturally(baseLoc.clone().add(offsetX, 0.5, offsetZ), stack);
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
    if (world == null) {
      throw new FatalActionException("release_ground_loot requires a loaded world");
    }
    requireRange((int) Math.ceil(action.radius()), 0, 128, "release_ground_loot.radius");
    return executeAt(
        instance.id(),
        baseLoc,
        () ->
            state
                .sharedLootSnapshot()
                .forEach(
                    (slot, loot) -> {
                      if (groundLootExists(world, baseLoc, instance.id(), slot, action.radius())) {
                        return;
                      }
                      ItemStack stack = resolveItemStack(loot.material(), loot.amount());
                      if (stack == null) {
                        return;
                      }
                      double offsetX =
                          (ThreadLocalRandom.current().nextDouble() - 0.5d) * action.radius();
                      double offsetZ =
                          (ThreadLocalRandom.current().nextDouble() - 0.5d) * action.radius();
                      Item item =
                          world.dropItemNaturally(
                              baseLoc.clone().add(offsetX, 0.5d, offsetZ), stack);
                      item.getPersistentDataContainer()
                          .set(
                              BukkitPdcKeys.EVENT_INSTANCE_ID,
                              PersistentDataType.STRING,
                              instance.id().toString());
                      item.getPersistentDataContainer()
                          .set(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, slot);
                    }));
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
    Set<UUID> eligible = ConcurrentHashMap.newKeySet();
    List<CompletableFuture<Boolean>> checks = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      checks.add(
          executeFor(
              instance.id(),
              player,
              () -> {
                Location location = player.getLocation();
                World world = location.getWorld();
                if (world != null
                    && zone.contains(
                        new EventLocation(
                            world.getName(),
                            location.getX(),
                            location.getY(),
                            location.getZ(),
                            location.getYaw(),
                            location.getPitch()))) {
                  eligible.add(player.getUniqueId());
                }
              }));
    }
    CompletableFuture<Void> inspected =
        CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new));
    return inspected.thenCompose(
        ignored -> {
          List<UUID> podium =
              ContributionRanking.podium(
                  state.contribution(),
                  eligible,
                  state.minimumContribution(),
                  state.contributionThresholdMillis());
          AsyncFutures.discard(executeGlobal(instance.id(), () -> broadcastPodium(podium)));
          List<CompletableFuture<Boolean>> deliveries = new ArrayList<>();
          for (int place = 0; place < podium.size() && place < action.pools().size(); place++) {
            Player winner = Bukkit.getPlayer(podium.get(place));
            if (winner == null) {
              continue;
            }
            PlatformActions.PodiumPool pool = action.pools().get(place);
            deliveries.add(
                executeFor(instance.id(), winner, () -> deliverPodiumPool(winner, pool)));
          }
          return CompletableFuture.allOf(deliveries.toArray(CompletableFuture[]::new))
              .thenApply(ignoredDeliveries -> true);
        });
  }

  protected void broadcastPodium(List<UUID> podium) {
    String message =
        locales.message("messages.metin-podium", Map.of("podium", podiumLabel(podium)));
    broadcastRawMessage(message);
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
    if (totalMobs > MAX_MOBS_PER_ACTION) {
      throw new FatalActionException(
          "spawn_mobs exceeds the limit of " + MAX_MOBS_PER_ACTION + " entities per action");
    }
    if (!action.waveId().isBlank()) {
      state.beginWave(action.waveId());
    }

    return executeAt(
        instance.id(),
        baseLoc,
        () -> {
          World world = baseLoc.getWorld();
          if (world == null) {
            return;
          }
          ThreadLocalRandom rng = ThreadLocalRandom.current();
          for (PlatformActions.MobSpawn mob : action.mobs()) {
            requireRange((int) Math.ceil(mob.radius()), 0, 128, "spawn_mobs.mobs.radius");
            EntityType type = resolveEntityType(mob.entityType());
            for (int i = 0; i < mob.amount(); i++) {
              double offsetX = (rng.nextDouble() - 0.5) * mob.radius() * 2;
              double offsetZ = (rng.nextDouble() - 0.5) * mob.radius() * 2;
              Location spawnLoc = baseLoc.clone().add(offsetX, 0, offsetZ);
              Entity entity = world.spawnEntity(spawnLoc, type);
              setEntityCustomName(entity, mob.name(), instance, state);
              entity.setCustomNameVisible(true);
              entity
                  .getPersistentDataContainer()
                  .set(
                      BukkitPdcKeys.EVENT_INSTANCE_ID,
                      PersistentDataType.STRING,
                      instance.id().toString());
              if (!action.waveId().isBlank()) {
                entity
                    .getPersistentDataContainer()
                    .set(BukkitPdcKeys.WAVE_ID, PersistentDataType.STRING, action.waveId());
                state.trackWaveEntity(action.waveId(), entity.getUniqueId());
              }
              registerSpawnedEntity(instance.id(), entity);
            }
          }
        });
  }

  // --- Utility & Cleanup Support ---

  protected void closeSharedLootInventories(EventInstanceId instanceId) {
    Bukkit.getOnlinePlayers().stream()
        .filter(
            player ->
                player.getOpenInventory().getTopInventory().getHolder()
                        instanceof BukkitSharedLootHolder holder
                    && holder.instanceId().equals(instanceId))
        .forEach(Player::closeInventory);
  }

  protected void stopModelAnimations(EventInstanceId instanceId) {
    if (modelAnimationActionService != null) {
      modelAnimationActionService.stopForEvent(instanceId);
    }
  }

  protected void stopAllModelAnimations() {
    if (modelAnimationActionService != null && modelRuntimeService != null) {
      for (RenderedModelHandle handle : modelRuntimeService.getActiveInstances()) {
        modelAnimationActionService.stopForModel(handle);
      }
    }
  }

  protected void runGuarded(
      EventInstanceId instanceId, Runnable action, CompletableFuture<Boolean> future) {
    try {
      action.run();
      if (future != null) {
        future.complete(true);
      }
    } catch (RuntimeException throwable) {
      runFailureHandler(instanceId, throwable);
      if (future != null) {
        future.complete(false);
      }
    }
  }

  protected void runFailureHandler(EventInstanceId instanceId, Throwable throwable) {
    logger.log(
        Level.SEVERE, "Asynchronous platform action failed for event " + instanceId, throwable);
    try {
      if (instanceId != null) {
        fatalActionHandler.accept(instanceId, throwable);
      }
    } catch (RuntimeException handlerFailure) {
      logger.severe(
          "Could not fail event "
              + instanceId
              + " after action error: "
              + handlerFailure.getMessage());
    }
  }

  protected Location resolveLocation(EventRuntimeState state) {
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

  protected void requireLocation(Location loc, String actionName) {
    if (loc == null) {
      throw new FatalActionException(actionName + " requires a valid platform location");
    }
  }

  protected void requireRange(int value, int min, int max, String fieldName) {
    if (value < min || value > max) {
      throw new FatalActionException(fieldName + " must be between " + min + " and " + max);
    }
  }

  protected Material resolveMaterial(String name) {
    String formatted = name.replace("minecraft:", "").toUpperCase(Locale.ROOT);
    Material material = Material.matchMaterial(formatted);
    if (material == null) {
      throw new FatalActionException("Unknown material: " + name);
    }
    return material;
  }

  protected ItemStack resolveItemStack(String materialName, int amount) {
    for (CustomItemProvider provider : itemProviders) {
      if (provider.isAvailable()) {
        ItemStack custom = provider.resolveItem(materialName, amount);
        if (custom != null) {
          return custom;
        }
      }
    }
    Material mat = resolveMaterial(materialName);
    return mat != null ? new ItemStack(mat, amount) : null;
  }

  protected ItemStack resolveLootItemStack(String materialName, int amount) {
    for (CustomItemProvider provider : itemProviders) {
      if (provider.isAvailable()) {
        ItemStack custom = provider.resolveItem(materialName, amount);
        if (custom != null) {
          return custom;
        }
      }
    }
    String formatted = materialName.replace("minecraft:", "").toUpperCase(Locale.ROOT);
    Material material = Material.matchMaterial(formatted);
    return new ItemStack(material == null ? Material.DIAMOND : material, amount);
  }

  protected List<ItemStack> mailboxItemStacks(List<RewardItem> items) {
    List<ItemStack> stacks = new ArrayList<>();
    for (RewardItem item : items) {
      int remaining = item.amount();
      while (remaining > 0) {
        ItemStack stack = resolveItemStack(item.material(), Math.min(remaining, 64));
        int amount = Math.min(remaining, stack.getMaxStackSize());
        stack.setAmount(amount);
        stacks.add(stack);
        remaining -= amount;
      }
    }
    return stacks;
  }

  protected boolean canContainAll(Inventory inventory, List<ItemStack> items) {
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
        int capacity = existing.getMaxStackSize() - existing.getAmount();
        int accepted = Math.min(capacity, remaining);
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

  protected EntityType resolveEntityType(String name) {
    try {
      return EntityType.valueOf(name.replace("minecraft:", "").toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new FatalActionException("Unknown entity type: " + name, e);
    }
  }

  protected Sound resolveSound(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    String raw = name.trim();
    String clean = raw.toLowerCase(Locale.ROOT);
    if (clean.startsWith("minecraft:")) {
      clean = clean.substring("minecraft:".length());
    }
    if (org.bukkit.Bukkit.getServer() != null) {
      try {
        org.bukkit.NamespacedKey key =
            org.bukkit.NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT));
        if (key != null && org.bukkit.Registry.SOUNDS != null) {
          Sound sound = org.bukkit.Registry.SOUNDS.get(key);
          if (sound != null) {
            return sound;
          }
        }
      } catch (IllegalArgumentException ignored) {
        // Fall through
      }
      try {
        if (org.bukkit.Registry.SOUNDS != null) {
          Sound sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(clean));
          if (sound != null) {
            return sound;
          }
        }
      } catch (IllegalArgumentException ignored) {
        // Fall through
      }
    }
    String fieldName = clean.replace('.', '_').toUpperCase(Locale.ROOT);
    try {
      java.lang.reflect.Field field = Sound.class.getField(fieldName);
      Object value = field.get(null);
      if (value instanceof Sound sound) {
        return sound;
      }
    } catch (ReflectiveOperationException | SecurityException | LinkageError ignored) {
      // Fall through
    }
    return null;
  }

  protected Particle resolveParticle(String name) {
    if (name == null || name.isBlank()) {
      return Particle.FLAME;
    }
    String clean = name.replace("minecraft:", "").replace('.', '_').toUpperCase(Locale.ROOT);
    try {
      return Particle.valueOf(clean);
    } catch (IllegalArgumentException ignored) {
      logger.log(Level.WARNING, "Unknown particle: {0}, falling back to FLAME", name);
      return Particle.FLAME;
    }
  }

  protected boolean groundLootExists(
      World world, Location center, EventInstanceId instanceId, int slot, double radius) {
    return world.getNearbyEntities(center, radius + 2.0d, 3.0d, radius + 2.0d).stream()
        .anyMatch(
            entity ->
                instanceId
                        .toString()
                        .equals(
                            entity
                                .getPersistentDataContainer()
                                .get(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING))
                    && Integer.valueOf(slot)
                        .equals(
                            entity
                                .getPersistentDataContainer()
                                .get(BukkitPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER)));
  }

  protected void deliverPodiumPool(Player player, PlatformActions.PodiumPool pool) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (PlatformActions.LootItem item : pool.items()) {
      requireRange(item.amount(), 1, 64, "award_podium.items.amount");
      requireRange(item.chance(), 0, 100, "award_podium.items.chance");
      if (random.nextInt(100) < item.chance()) {
        ItemStack stack = resolveItemStack(item.material(), item.amount());
        if (stack != null) {
          Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
          leftover
              .values()
              .forEach(value -> player.getWorld().dropItemNaturally(player.getLocation(), value));
        }
      }
    }
  }

  protected String podiumLabel(List<UUID> podium) {
    if (podium.isEmpty()) {
      return "no eligible finalists";
    }
    List<String> names = new ArrayList<>();
    for (int i = 0; i < podium.size(); i++) {
      Player player = Bukkit.getPlayer(podium.get(i));
      names.add((i + 1) + ". " + (player != null ? player.getName() : podium.get(i)));
    }
    return String.join(", ", names);
  }
}
