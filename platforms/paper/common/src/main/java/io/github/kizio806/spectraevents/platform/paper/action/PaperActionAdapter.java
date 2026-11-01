package io.github.kizio806.spectraevents.platform.paper.action;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.action.AbstractBukkitActionAdapter;
import io.github.kizio806.spectraevents.platform.bukkit.common.EventDisplayPlaceholders;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootHolder;
import io.github.kizio806.spectraevents.platform.paper.bossbar.EventBossBarManager;
import io.github.kizio806.spectraevents.platform.paper.integration.MiniPlaceholdersIntegration;
import io.github.kizio806.spectraevents.platform.paper.integration.item.ItemsAdderItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.NexoItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.OraxenItemProvider;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.github.kizio806.spectraevents.platform.paper.scoreboard.EventScoreboardManager;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Paper implementation of {@link AbstractBukkitActionAdapter} with region-aware scheduling and
 * native Adventure support.
 */
public final class PaperActionAdapter extends AbstractBukkitActionAdapter {

  private final RegionTaskScheduler regionScheduler;
  private final PaperResourceCleaner cleaner;
  private final EventBossBarManager bossBarManager;
  private final EventScoreboardManager scoreboardManager;

  public PaperActionAdapter(
      RegionTaskScheduler regionScheduler, PaperResourceCleaner cleaner, LocaleCatalog locales) {
    super(locales);
    this.regionScheduler = Objects.requireNonNull(regionScheduler, "regionScheduler");
    this.cleaner = Objects.requireNonNull(cleaner, "cleaner");
    this.bossBarManager = new EventBossBarManager(regionScheduler, locales);
    this.scoreboardManager = new EventScoreboardManager(regionScheduler, locales);

    registerItemProvider(new NexoItemProvider());
    registerItemProvider(new OraxenItemProvider());
    registerItemProvider(new ItemsAdderItemProvider());
  }

  public EventBossBarManager bossBarManager() {
    return bossBarManager;
  }

  public EventScoreboardManager scoreboardManager() {
    return scoreboardManager;
  }

  @Override
  public void cleanupEvent(EventInstanceId instanceId) {
    closeSharedLootInventories(instanceId);
    stopModelAnimations(instanceId);
    cleaner.cleanup(instanceId);
  }

  @Override
  public void cleanupAll() {
    beginShutdown();
    bossBarManager.beginShutdown();
    scoreboardManager.beginShutdown();
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

  private Component renderTemplate(
      String template, EventInstance instance, EventRuntimeState state) {
    return MiniPlaceholdersIntegration.getMiniMessage()
        .deserialize(
            EventDisplayPlaceholders.resolve(locales.resolveTemplate(template), instance, state));
  }

  @Override
  protected void setEntityCustomName(
      Entity entity, String template, EventInstance instance, EventRuntimeState state) {
    entity.customName(renderTemplate(template, instance, state));
    entity.setCustomNameVisible(true);
  }

  @Override
  protected void sendPlayerMessage(
      CommandSender sender,
      String messageTemplate,
      EventInstance instance,
      EventRuntimeState state) {
    sender.sendMessage(renderTemplate(messageTemplate, instance, state));
  }

  @Override
  protected void broadcastMessage(
      String messageTemplate, EventInstance instance, EventRuntimeState state) {
    Component component = renderTemplate(messageTemplate, instance, state);
    for (Player p : Bukkit.getOnlinePlayers()) {
      p.sendMessage(component);
    }
    Bukkit.getConsoleSender().sendMessage(component);
  }

  @Override
  protected void broadcastRawMessage(String rawMessage) {
    Bukkit.broadcast(Component.text(rawMessage));
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
            renderTemplate(titleTemplate, instance, state),
            renderTemplate(subtitleTemplate, instance, state),
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)));
    Bukkit.getOnlinePlayers().forEach(player -> player.showTitle(rendered));
  }

  @Override
  protected Inventory createSharedLootInventory(
      BukkitSharedLootHolder holder,
      int size,
      String titleTemplate,
      EventInstance instance,
      EventRuntimeState state) {
    return Bukkit.createInventory(holder, size, renderTemplate(titleTemplate, instance, state));
  }

  @Override
  protected void registerSpawnedEntity(EventInstanceId instanceId, Entity entity) {
    cleaner.registerCustomCleanup(
        instanceId,
        () -> {
          if (entity.isValid()) {
            entity.remove();
          }
        });
  }

  @Override
  protected void registerSpawnedModel(EventInstanceId instanceId, RenderedModelHandle handle) {
    cleaner.registerCustomCleanup(
        instanceId,
        () -> {
          if (modelAnimationActionService != null) {
            modelAnimationActionService.stopForModel(handle);
          }
          if (modelRuntimeService != null) {
            modelRuntimeService.removeModel(handle.runtimeId());
          }
        });
  }

  @Override
  protected CompletableFuture<Boolean> executeAt(
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

  @Override
  protected CompletableFuture<Boolean> executeGlobal(EventInstanceId instanceId, Runnable action) {
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

  @Override
  protected CompletableFuture<Boolean> executeFor(
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
              instance, state, action.title(), action.color(), action.style(), action.progress());
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
        () -> bossBarManager.updateBossBar(instance, state, action.title(), action.progress()));
  }

  @Override
  protected CompletableFuture<Boolean> handleRemoveBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveBossbarAction action,
      ExecutionContext context) {
    return executeGlobal(instance.id(), () -> bossBarManager.removeBossBar(instance.id().value()));
  }

  @Override
  protected CompletableFuture<Boolean> handleShowScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowScoreboardAction action,
      ExecutionContext context) {
    return executeGlobal(
        instance.id(),
        () -> {
          scoreboardManager.showScoreboard(instance, state, action.title(), action.lines());
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
        instance.id(), () -> scoreboardManager.updateScoreboard(instance, state, action.lines()));
  }

  @Override
  protected CompletableFuture<Boolean> handleRemoveScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveScoreboardAction action,
      ExecutionContext context) {
    return executeGlobal(
        instance.id(), () -> scoreboardManager.removeScoreboard(instance.id().value()));
  }
}
