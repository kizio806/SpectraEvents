package io.github.kizio806.spectraevents.platform.spigot.action;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.action.AbstractBukkitActionAdapter;
import io.github.kizio806.spectraevents.platform.bukkit.bossbar.BukkitBossBarProgressCalculator;
import io.github.kizio806.spectraevents.platform.bukkit.common.EventDisplayPlaceholders;
import io.github.kizio806.spectraevents.platform.bukkit.compat.BukkitPlatformBridge;
import io.github.kizio806.spectraevents.platform.bukkit.concurrent.AsyncFutures;
import io.github.kizio806.spectraevents.platform.bukkit.integration.item.ItemsAdderItemProvider;
import io.github.kizio806.spectraevents.platform.bukkit.integration.item.NexoItemProvider;
import io.github.kizio806.spectraevents.platform.bukkit.integration.item.OraxenItemProvider;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootHolder;
import io.github.kizio806.spectraevents.platform.spigot.bossbar.SpigotBossBarManager;
import io.github.kizio806.spectraevents.platform.spigot.scoreboard.SpigotScoreboardManager;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

/**
 * Spigot implementation of {@link AbstractBukkitActionAdapter} with primary-thread scheduling and
 * Adventure Platform support.
 */
public final class SpigotActionAdapter extends AbstractBukkitActionAdapter implements Listener {

  private final Plugin plugin;
  private final BukkitAudiences adventure;

  private final Map<EventInstanceId, List<UUID>> ownedEntities = new ConcurrentHashMap<>();
  private final SpigotBossBarManager bossBarManager;
  private final SpigotScoreboardManager scoreboardManager;

  public SpigotActionAdapter(Plugin plugin, BukkitAudiences adventure, LocaleCatalog locales) {
    super(locales);
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.adventure = Objects.requireNonNull(adventure, "adventure");
    this.bossBarManager =
        new SpigotBossBarManager((tmpl, inst) -> renderLegacyTemplate(tmpl, inst, null));
    this.scoreboardManager =
        new SpigotScoreboardManager((tmpl, inst) -> renderLegacyTemplate(tmpl, inst, null));

    registerItemProvider(new NexoItemProvider());
    registerItemProvider(new OraxenItemProvider());
    registerItemProvider(new ItemsAdderItemProvider());
  }

  public SpigotBossBarManager getBossBarManager() {
    return bossBarManager;
  }

  public SpigotScoreboardManager getScoreboardManager() {
    return scoreboardManager;
  }

  public void registerRecoveredEntity(EventInstanceId instanceId, UUID entityId) {
    ownedEntities
        .computeIfAbsent(instanceId, ignored -> new java.util.concurrent.CopyOnWriteArrayList<>())
        .add(entityId);
  }

  @Override
  public void cleanupEvent(EventInstanceId instanceId) {
    closeSharedLootInventories(instanceId);
    stopModelAnimations(instanceId);
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

    bossBarManager.cleanup(instanceId);
    scoreboardManager.cleanup(instanceId);
  }

  @Override
  public void cleanupAll() {
    beginShutdown();
    for (EventInstanceId id : List.copyOf(ownedEntities.keySet())) {
      cleanupEvent(id);
    }
    bossBarManager.shutdown();
    scoreboardManager.shutdown();
  }

  @Override
  public int resourceCount(EventInstanceId instanceId) {
    int count = 0;
    List<UUID> entities = ownedEntities.get(instanceId);
    if (entities != null) {
      count += entities.size();
    }
    if (bossBarManager.hasBossBar(instanceId)) {
      count++;
    }
    if (scoreboardManager.hasScoreboard(instanceId)) {
      count++;
    }
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
  public void refreshHud(EventInstance instance, EventRuntimeState state) {
    AsyncFutures.discard(
        executeSync(
            () -> {
              String progressMode = bossBarManager.getProgressMode(instance.id());
              bossBarManager.refreshHud(
                  instance,
                  state,
                  BukkitBossBarProgressCalculator.calculateFloat(state, progressMode));
            }));
  }

  private String resolveDisplayTemplate(
      String template, EventInstance instance, EventRuntimeState state) {
    return EventDisplayPlaceholders.resolve(locales.resolveTemplate(template), instance, state);
  }

  private String renderLegacyTemplate(
      String template, EventInstance instance, EventRuntimeState state) {
    return resolveDisplayTemplate(template, instance, state).replaceAll("<[^>]*>", "");
  }

  @Override
  protected void setEntityCustomName(
      Entity entity, String template, EventInstance instance, EventRuntimeState state) {
    String rendered = renderLegacyTemplate(template, instance, state);
    Component component =
        MiniMessage.miniMessage().deserialize(resolveDisplayTemplate(template, instance, state));
    BukkitPlatformBridge.setCustomName(entity, rendered, component, plugin.getLogger());
    entity.setCustomNameVisible(true);
  }

  @Override
  protected void sendPlayerMessage(
      CommandSender sender,
      String messageTemplate,
      EventInstance instance,
      EventRuntimeState state) {
    var component =
        MiniMessage.miniMessage()
            .deserialize(resolveDisplayTemplate(messageTemplate, instance, state));
    adventure.sender(sender).sendMessage(component);
  }

  @Override
  protected void broadcastMessage(
      String messageTemplate, EventInstance instance, EventRuntimeState state) {
    var component =
        MiniMessage.miniMessage()
            .deserialize(resolveDisplayTemplate(messageTemplate, instance, state));
    adventure.all().sendMessage(component);
  }

  @Override
  protected void broadcastRawMessage(String rawMessage) {
    adventure.all().sendMessage(Component.text(rawMessage));
  }

  @Override
  protected void showTitle(
      String titleTemplate,
      String subtitleTemplate,
      int fadeIn,
      int stay,
      int fadeOut,
      EventInstance instance,
      EventRuntimeState state) {
    Title rendered =
        Title.title(
            MiniMessage.miniMessage()
                .deserialize(resolveDisplayTemplate(titleTemplate, instance, state)),
            MiniMessage.miniMessage()
                .deserialize(resolveDisplayTemplate(subtitleTemplate, instance, state)),
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)));
    adventure.all().showTitle(rendered);
  }

  @Override
  protected Inventory createSharedLootInventory(
      BukkitSharedLootHolder holder,
      int size,
      String titleTemplate,
      EventInstance instance,
      EventRuntimeState state) {
    String rendered = renderLegacyTemplate(titleTemplate, instance, state);
    Component component =
        MiniMessage.miniMessage()
            .deserialize(resolveDisplayTemplate(titleTemplate, instance, state));
    return BukkitPlatformBridge.createInventory(holder, size, rendered, component);
  }

  @Override
  protected void registerSpawnedEntity(EventInstanceId instanceId, Entity entity) {
    ownedEntities
        .computeIfAbsent(instanceId, ignored -> new java.util.concurrent.CopyOnWriteArrayList<>())
        .add(entity.getUniqueId());
  }

  @Override
  protected CompletableFuture<Boolean> executeAt(
      EventInstanceId instanceId, Location location, Runnable action) {
    return executeSync(action);
  }

  @Override
  protected CompletableFuture<Boolean> executeGlobal(EventInstanceId instanceId, Runnable action) {
    return executeSync(action);
  }

  @Override
  protected CompletableFuture<Boolean> executeFor(
      EventInstanceId instanceId, Player player, Runnable action) {
    return executeSync(action);
  }

  private CompletableFuture<Boolean> executeSync(Runnable action) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    if (shuttingDown) {
      future.complete(false);
      return future;
    }
    org.bukkit.Server server = org.bukkit.Bukkit.getServer();
    if (server != null && !server.isPrimaryThread()) {
      server.getScheduler().runTask(plugin, () -> runGuarded(null, action, future));
    } else {
      runGuarded(null, action, future);
    }
    return future;
  }

  @Override
  protected CompletableFuture<Boolean> handleShowBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowBossbarAction action,
      ExecutionContext context) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    return executeSync(
        () ->
            bossBarManager.showBossBar(
                instance,
                state,
                action.title(),
                action.color(),
                action.style(),
                action.progress(),
                BukkitBossBarProgressCalculator.calculateFloat(state, action.progress())));
  }

  @Override
  protected CompletableFuture<Boolean> handleUpdateBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.UpdateBossbarAction action,
      ExecutionContext context) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    return executeSync(
        () ->
            bossBarManager.updateBossBar(
                instance,
                state,
                action.title(),
                action.progress(),
                BukkitBossBarProgressCalculator.calculateFloat(state, action.progress())));
  }

  @Override
  protected CompletableFuture<Boolean> handleRemoveBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveBossbarAction action,
      ExecutionContext context) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    return executeSync(() -> bossBarManager.removeBossBar(instance.id()));
  }

  @Override
  protected CompletableFuture<Boolean> handleShowScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowScoreboardAction action,
      ExecutionContext context) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    return executeSync(
        () -> scoreboardManager.showScoreboard(instance, state, action.title(), action.lines()));
  }

  @Override
  protected CompletableFuture<Boolean> handleUpdateScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.UpdateScoreboardAction action,
      ExecutionContext context) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    return executeSync(() -> scoreboardManager.updateScoreboard(instance, state, action.lines()));
  }

  @Override
  protected CompletableFuture<Boolean> handleRemoveScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveScoreboardAction action,
      ExecutionContext context) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    return executeSync(() -> scoreboardManager.removeScoreboard(instance.id()));
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    bossBarManager.onPlayerJoin(player);
    scoreboardManager.onPlayerJoin(player);
  }
}
