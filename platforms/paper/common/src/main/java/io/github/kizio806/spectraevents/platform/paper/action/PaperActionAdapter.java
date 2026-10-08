package io.github.kizio806.spectraevents.platform.paper.action;

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
import io.github.kizio806.spectraevents.platform.paper.bossbar.EventBossBarManager;
import io.github.kizio806.spectraevents.platform.paper.common.EventDisplayPlaceholders;
import io.github.kizio806.spectraevents.platform.paper.integration.MiniPlaceholdersIntegration;
import io.github.kizio806.spectraevents.platform.paper.integration.item.CustomItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.ItemsAdderItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.NexoItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.OraxenItemProvider;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import io.github.kizio806.spectraevents.platform.paper.loot.PaperSharedLootHolder;
import io.github.kizio806.spectraevents.platform.paper.metadata.SpectraPdcKeys;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.github.kizio806.spectraevents.platform.paper.scoreboard.EventScoreboardManager;
import java.time.Duration;
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
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
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

@SuppressWarnings("StringConcatToTextBlock")
public final class PaperActionAdapter extends AbstractPlatformActionAdapter {
  private static final Logger LOGGER = Logger.getLogger(PaperActionAdapter.class.getName());
  private static final int MAX_PARTICLES_PER_ACTION = 10_000;
  private static final int MAX_MOBS_PER_ACTION = 128;
  private static final int MAX_LOOT_ENTRIES = 64;

  private final RegionTaskScheduler regionScheduler;
  private final PaperResourceCleaner cleaner;
  private final LocaleCatalog locales;
  private final EventBossBarManager bossBarManager;
  private final EventScoreboardManager scoreboardManager;
  private final List<CustomItemProvider> itemProviders = new ArrayList<>();
  private ModelRuntimeService modelRuntimeService;
  private ModelAnimationActionService modelAnimationActionService;
  private volatile boolean shuttingDown;
  private BiConsumer<EventInstanceId, Throwable> fatalActionHandler =
      (instanceId, throwable) ->
          LOGGER.severe(
              "Asynchronous action failed for " + instanceId + ": " + throwable.getMessage());

  public PaperActionAdapter(
      RegionTaskScheduler regionScheduler, PaperResourceCleaner cleaner, LocaleCatalog locales) {
    this.regionScheduler = Objects.requireNonNull(regionScheduler, "regionScheduler");
    this.cleaner = Objects.requireNonNull(cleaner, "cleaner");
    this.locales = Objects.requireNonNull(locales, "locales");
    this.bossBarManager = new EventBossBarManager(regionScheduler, locales);
    this.scoreboardManager = new EventScoreboardManager(regionScheduler, locales);

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

  /** Delivers a complete reward snapshot on the player's owning region. */
  public CompletableFuture<Boolean> deliverRewardItems(Player player, List<RewardItem> items) {
    Objects.requireNonNull(player, "player");
    List<RewardItem> snapshot = List.copyOf(Objects.requireNonNull(items, "items"));
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (shuttingDown || !player.isOnline()) {
      future.complete(false);
      return future;
    }
    try {
      regionScheduler.executeFor(
          player,
          () -> {
            try {
              if (!player.isOnline()) {
                future.complete(false);
                return;
              }
              List<ItemStack> stacks = mailboxItemStacks(snapshot);
              Inventory inventory = player.getInventory();
              if (!canContainAll(inventory, stacks)) {
                for (ItemStack stack : stacks) {
                  player.getWorld().dropItem(player.getLocation(), stack);
                }
                future.complete(true);
                return;
              }
              Map<Integer, ItemStack> leftovers =
                  inventory.addItem(stacks.toArray(ItemStack[]::new));
              future.complete(leftovers.isEmpty());
            } catch (RuntimeException exception) {
              LOGGER.log(
                  java.util.logging.Level.WARNING,
                  "Could not deliver reward mailbox claim",
                  exception);
              future.complete(false);
            }
          });
    } catch (RuntimeException exception) {
      LOGGER.log(
          java.util.logging.Level.WARNING, "Could not schedule reward mailbox delivery", exception);
      future.complete(false);
    }
    return future;
  }

  @Override
  public CompletableFuture<Boolean> deliverRewardOrDrop(UUID playerId, List<RewardItem> items) {
    Player player = Bukkit.getPlayer(Objects.requireNonNull(playerId, "playerId"));
    return player == null
        ? CompletableFuture.completedFuture(false)
        : deliverRewardItems(player, items);
  }

  @Override
  public void setFatalActionHandler(BiConsumer<EventInstanceId, Throwable> handler) {
    this.fatalActionHandler = Objects.requireNonNull(handler, "handler");
  }

  @Override
  public void cleanupEvent(EventInstanceId instanceId) {
    Bukkit.getOnlinePlayers().stream()
        .filter(
            player ->
                player.getOpenInventory().getTopInventory().getHolder()
                        instanceof PaperSharedLootHolder holder
                    && holder.instanceId().equals(instanceId))
        .forEach(Player::closeInventory);
    if (modelAnimationActionService != null) modelAnimationActionService.stopForEvent(instanceId);
    cleaner.cleanup(instanceId);
  }

  @Override
  public void cleanupAll() {
    shuttingDown = true;
    bossBarManager.beginShutdown();
    scoreboardManager.beginShutdown();
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
  public void refreshHud(EventInstance instance, EventRuntimeState state) {
    if (shuttingDown) {
      return;
    }
    regionScheduler.executeGlobal(() -> bossBarManager.refreshBossBar(instance, state));
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

  private Component renderTemplate(
      String template, EventInstance instance, EventRuntimeState state) {
    return MiniPlaceholdersIntegration.getMiniMessage()
        .deserialize(
            EventDisplayPlaceholders.resolve(locales.resolveTemplate(template), instance, state));
  }

  @Override
  protected CompletableFuture<Boolean> handleOpenSharedLoot(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.OpenSharedLootAction action,
      ExecutionContext context) {
    if (!(context.actor() instanceof Player player))
      return CompletableFuture.completedFuture(false);
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    regionScheduler.executeFor(
        player,
        () -> {
          Inventory inventory =
              Bukkit.createInventory(
                  new PaperSharedLootHolder(instance.id()),
                  27,
                  renderTemplate(action.title(), instance, state));
          state
              .sharedLootSnapshot()
              .forEach(
                  (slot, loot) -> {
                    Material material =
                        Material.matchMaterial(loot.material().replace("minecraft:", ""));
                    inventory.setItem(
                        slot,
                        new ItemStack(
                            material == null ? Material.DIAMOND : material, loot.amount()));
                  });
          player.openInventory(inventory);
          result.complete(true);
        });
    return result;
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

    return executeAt(
        instance.id(),
        spawnLoc,
        () -> {
          if (modelRuntimeService == null)
            throw new FatalActionException("Model runtime is unavailable on Paper");
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
                  if (modelAnimationActionService != null)
                    modelAnimationActionService.stopForModel(handle);
                  modelRuntimeService.removeModel(handle.runtimeId());
                });
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
    if (modelRuntimeService == null)
      throw new FatalActionException("Model runtime is unavailable on Paper");
    return executeAt(
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

  @Override
  protected CompletableFuture<Boolean> handlePlayAnimation(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.PlayAnimationAction action,
      ExecutionContext context) {
    Location baseLoc = resolveLocation(state);
    requireLocation(baseLoc, "play_animation");
    if (modelAnimationActionService == null)
      throw new FatalActionException("Model animation runtime is unavailable on Paper");
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
    return executeAt(
        instance.id(),
        baseLoc,
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
    return executeAt(
        instance.id(),
        baseLoc,
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

    ItemStack itemStack = null;
    for (CustomItemProvider provider : itemProviders) {
      if (provider.isAvailable()) {
        ItemStack custom = provider.resolveItem(action.material(), action.amount());
        if (custom != null) {
          itemStack = custom;
          break;
        }
      }
    }

    if (itemStack == null) {
      Material mat = resolveMaterial(action.material());
      if (mat != null) itemStack = new ItemStack(mat, action.amount());
    }

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
    if (context == null || !(context.actor() instanceof CommandSender sender))
      throw new FatalActionException("send_message requires a command-sender context");
    if (!action.message().isEmpty()) {
      var component = renderTemplate(action.message(), instance, state);
      if (sender instanceof Player player) {
        return executeFor(instance.id(), player, () -> player.sendMessage(component));
      } else {
        sender.sendMessage(component);
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
          instance.id(),
          () -> {
            var component = renderTemplate(action.message(), instance, state);
            for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(component);
            Bukkit.getConsoleSender().sendMessage(component);
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
            renderTemplate(action.title(), instance, state),
            renderTemplate(action.subtitle(), instance, state),
            Title.Times.times(
                Duration.ofMillis(action.fadeIn() * 50L),
                Duration.ofMillis(action.stay() * 50L),
                Duration.ofMillis(action.fadeOut() * 50L)));

    return executeGlobal(
        instance.id(),
        () -> Bukkit.getOnlinePlayers().forEach(player -> player.showTitle(rendered)));
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
          if (world == null) return;
          Entity entity = world.spawnEntity(spawnLoc, entityType);
          entity.customName(renderTemplate(action.name(), instance, state));
          entity.setCustomNameVisible(true);
          entity
              .getPersistentDataContainer()
              .set(SpectraPdcKeys.INSTANCE_ID, PersistentDataType.STRING, instance.id().toString());
          state.setBossEntityId(entity.getUniqueId());
          cleaner.registerCustomCleanup(
              instance.id(),
              () -> {
                if (entity.isValid()) entity.remove();
              });
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
    if (world == null)
      throw new FatalActionException("release_ground_loot requires a loaded world");
    requireRange((int) Math.ceil(action.radius()), 0, 128, "release_ground_loot.radius");
    return executeAt(
        instance.id(),
        baseLoc,
        () ->
            state
                .sharedLootSnapshot()
                .forEach(
                    (slot, loot) -> {
                      if (groundLootExists(world, baseLoc, instance.id(), slot, action.radius()))
                        return;
                      ItemStack stack = resolveItemStack(loot.material(), loot.amount());
                      if (stack == null) return;
                      double offsetX =
                          (ThreadLocalRandom.current().nextDouble() - 0.5d) * action.radius();
                      double offsetZ =
                          (ThreadLocalRandom.current().nextDouble() - 0.5d) * action.radius();
                      Item item =
                          world.dropItemNaturally(
                              baseLoc.clone().add(offsetX, 0.5d, offsetZ), stack);
                      item.getPersistentDataContainer()
                          .set(
                              SpectraPdcKeys.INSTANCE_ID,
                              PersistentDataType.STRING,
                              instance.id().toString());
                      item.getPersistentDataContainer()
                          .set(SpectraPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER, slot);
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
                                .get(SpectraPdcKeys.INSTANCE_ID, PersistentDataType.STRING))
                    && Integer.valueOf(slot)
                        .equals(
                            entity
                                .getPersistentDataContainer()
                                .get(SpectraPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER)));
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
          regionScheduler.executeGlobal(
              () ->
                  Bukkit.broadcast(
                      Component.text(
                          locales.message(
                              "messages.metin-podium", Map.of("podium", podiumLabel(podium))))));
          List<CompletableFuture<Boolean>> deliveries = new ArrayList<>();
          for (int place = 0; place < podium.size(); place++) {
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

  private void deliverPodiumPool(Player player, PlatformActions.PodiumPool pool) {
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

  private String podiumLabel(List<UUID> podium) {
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

    return executeAt(
        instance.id(),
        baseLoc,
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
              entity.customName(renderTemplate(mob.name(), instance, state));
              entity.setCustomNameVisible(true);
              entity
                  .getPersistentDataContainer()
                  .set(
                      SpectraPdcKeys.INSTANCE_ID,
                      PersistentDataType.STRING,
                      instance.id().toString());
              if (!action.waveId().isBlank()) {
                entity
                    .getPersistentDataContainer()
                    .set(SpectraPdcKeys.WAVE_ID, PersistentDataType.STRING, action.waveId());
                state.trackWaveEntity(action.waveId(), entity.getUniqueId());
              }
              cleaner.registerCustomCleanup(
                  instance.id(),
                  () -> {
                    if (entity.isValid()) entity.remove();
                  });
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
    return executeGlobal(
        instance.id(),
        () -> {
          bossBarManager.showBossBar(
              instance,
              state,
              Map.of(
                  "color",
                  action.color(),
                  "style",
                  action.style(),
                  "title",
                  action.title(),
                  "progress",
                  action.progress()));
          cleaner.registerCustomCleanup(
              instance.id(), () -> bossBarManager.removeBossBar(instance.id().value()));
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleUpdateBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.UpdateBossbarAction action,
      ExecutionContext context) {
    return executeGlobal(
        instance.id(),
        () ->
            bossBarManager.updateBossBar(
                instance, state, Map.of("title", action.title(), "progress", action.progress())));
  }

  @Override
  protected CompletableFuture<Boolean> handleRemoveBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveBossbarAction action,
      ExecutionContext context) {
    return executeGlobal(instance.id(), () -> bossBarManager.removeBossBar(instance.id().value()));
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  @Override
  protected CompletableFuture<Boolean> handleShowScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowScoreboardAction action,
      ExecutionContext context) {
    return executeGlobal(
        instance.id(),
        () -> {
          scoreboardManager.showScoreboard(
              instance, state, Map.of("title", action.title(), "lines", action.lines()));
          cleaner.registerCustomCleanup(
              instance.id(), () -> scoreboardManager.removeScoreboard(instance.id().value()));
        });
  }

  @Override
  protected CompletableFuture<Boolean> handleUpdateScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.UpdateScoreboardAction action,
      ExecutionContext context) {
    return executeGlobal(
        instance.id(),
        () -> scoreboardManager.updateScoreboard(instance, state, Map.of("lines", action.lines())));
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  @Override
  protected CompletableFuture<Boolean> handleRemoveScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveScoreboardAction action,
      ExecutionContext context) {
    return executeGlobal(
        instance.id(), () -> scoreboardManager.removeScoreboard(instance.id().value()));
  }

  private Material resolveMaterial(String name) {
    String formatted = name.replace("minecraft:", "").toUpperCase(Locale.ROOT);
    Material material = Material.matchMaterial(formatted);
    if (material == null) throw new FatalActionException("Unknown material: " + name);
    return material;
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

  private List<ItemStack> mailboxItemStacks(List<RewardItem> items) {
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

  private EntityType resolveEntityType(String name) {
    try {
      return EntityType.valueOf(name.replace("minecraft:", "").toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new FatalActionException("Unknown entity type: " + name, e);
    }
  }

  @SuppressWarnings("removal")
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

  private CompletableFuture<Boolean> executeAt(
      EventInstanceId instanceId, Location location, Runnable action) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (shuttingDown) {
      future.complete(false);
      return future;
    }
    try {
      regionScheduler.executeAt(location, () -> runGuarded(instanceId, action, future));
    } catch (RuntimeException exception) {
      runFailureHandler(instanceId, exception);
      future.complete(false);
      throw exception;
    }
    return future;
  }

  private CompletableFuture<Boolean> executeGlobal(EventInstanceId instanceId, Runnable action) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (shuttingDown) {
      future.complete(false);
      return future;
    }
    try {
      regionScheduler.executeGlobal(() -> runGuarded(instanceId, action, future));
    } catch (RuntimeException exception) {
      runFailureHandler(instanceId, exception);
      future.complete(false);
      throw exception;
    }
    return future;
  }

  private CompletableFuture<Boolean> executeFor(
      EventInstanceId instanceId, Player player, Runnable action) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (shuttingDown) {
      future.complete(false);
      return future;
    }
    try {
      regionScheduler.executeFor(player, () -> runGuarded(instanceId, action, future));
    } catch (RuntimeException exception) {
      runFailureHandler(instanceId, exception);
      future.complete(false);
      throw exception;
    }
    return future;
  }

  private void runGuarded(
      EventInstanceId instanceId, Runnable action, CompletableFuture<Boolean> future) {
    try {
      action.run();
      if (future != null) future.complete(true);
    } catch (RuntimeException throwable) {
      runFailureHandler(instanceId, throwable);
      if (future != null) future.complete(false);
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
}
