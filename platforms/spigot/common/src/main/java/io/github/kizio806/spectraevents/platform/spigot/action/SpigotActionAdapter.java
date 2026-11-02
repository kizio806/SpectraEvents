package io.github.kizio806.spectraevents.platform.spigot.action;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.action.AbstractBukkitActionAdapter;
import io.github.kizio806.spectraevents.platform.bukkit.common.EventDisplayPlaceholders;
import io.github.kizio806.spectraevents.platform.bukkit.concurrent.AsyncFutures;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootHolder;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
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
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Spigot implementation of {@link AbstractBukkitActionAdapter} with primary-thread scheduling and
 * Adventure Platform support.
 */
public final class SpigotActionAdapter extends AbstractBukkitActionAdapter implements Listener {

  private static final int MAX_VISIBLE_EVENT_BOSS_BARS = 3;

  private final Plugin plugin;
  private final BukkitAudiences adventure;

  private final Map<EventInstanceId, List<UUID>> ownedEntities = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, BossBar> bossBars = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, String> bossBarTemplates = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, String> bossBarProgressModes = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, Scoreboard> scoreboards = new ConcurrentHashMap<>();

  public SpigotActionAdapter(Plugin plugin, BukkitAudiences adventure, LocaleCatalog locales) {
    super(locales);
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.adventure = Objects.requireNonNull(adventure, "adventure");
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

    BossBar bar = bossBars.remove(instanceId);
    if (bar != null) {
      bar.removeAll();
    }
    bossBarTemplates.remove(instanceId);
    bossBarProgressModes.remove(instanceId);

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
    beginShutdown();
    for (EventInstanceId id : List.copyOf(ownedEntities.keySet())) {
      cleanupEvent(id);
    }
  }

  @Override
  public int resourceCount(EventInstanceId instanceId) {
    int count = 0;
    List<UUID> entities = ownedEntities.get(instanceId);
    if (entities != null) {
      count += entities.size();
    }
    if (bossBars.containsKey(instanceId)) {
      count++;
    }
    if (scoreboards.containsKey(instanceId)) {
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
              BossBar bar = bossBars.get(instance.id());
              if (bar != null) {
                bar.setTitle(
                    renderLegacyTemplate(
                        bossBarTemplates.getOrDefault(instance.id(), bar.getTitle()),
                        instance,
                        state));
                bar.setProgress(
                    Math.max(
                        0.0,
                        Math.min(
                            1.0,
                            calculateProgress(
                                state, bossBarProgressModes.getOrDefault(instance.id(), "1.0")))));
              }
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
    entity.setCustomName(renderLegacyTemplate(template, instance, state));
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
    return Bukkit.createInventory(
        holder, size, renderLegacyTemplate(titleTemplate, instance, state));
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
    if (Bukkit.isPrimaryThread()) {
      runGuarded(null, action, future);
    } else {
      Bukkit.getScheduler().runTask(plugin, () -> runGuarded(null, action, future));
    }
    return future;
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
            color =
                action.color() != null
                    ? BarColor.valueOf(action.color().toUpperCase(Locale.ROOT))
                    : BarColor.PURPLE;
          } catch (IllegalArgumentException e) {
            color = BarColor.PURPLE;
          }

          BarStyle style;
          try {
            String upper =
                action.style() != null ? action.style().toUpperCase(Locale.ROOT) : "SOLID";
            if (upper.startsWith("NOTCH_")) {
              upper = upper.replace("NOTCH_", "SEGMENTED_");
            } else if (upper.startsWith("NOTCHES_")) {
              upper = upper.replace("NOTCHES_", "SEGMENTED_");
            } else if (upper.equals("PROGRESS")) {
              upper = "SOLID";
            }
            style = BarStyle.valueOf(upper);
          } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
          }

          double progress =
              Math.max(0.0, Math.min(1.0, calculateProgress(state, action.progress())));
          BossBar bar =
              Bukkit.createBossBar(
                  renderLegacyTemplate(action.title(), instance, state), color, style);
          bar.setProgress(progress);
          for (Player player : Bukkit.getOnlinePlayers()) {
            bar.addPlayer(player);
          }
          bossBars.put(instance.id(), bar);
          bossBarTemplates.put(instance.id(), action.title());
          bossBarProgressModes.put(instance.id(), action.progress());
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
            bossBarProgressModes.put(instance.id(), action.progress());
            bar.setTitle(renderLegacyTemplate(action.title(), instance, state));
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
          if (bar != null) {
            bar.removeAll();
          }
          bossBarTemplates.remove(instance.id());
          bossBarProgressModes.remove(instance.id());
        });
  }

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
                  org.bukkit.scoreboard.Criteria.DUMMY,
                  renderLegacyTemplate(action.title(), instance, state));
          obj.setDisplaySlot(DisplaySlot.SIDEBAR);

          List<String> lines = action.lines();
          for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int score = lines.size() - i;
            obj.getScore(renderLegacyTemplate(line, instance, state)).setScore(score);
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

  private double calculateProgress(EventRuntimeState state, String progressStr) {
    if ("hits".equalsIgnoreCase(progressStr) && state.hitCounter() != null) {
      var counter = state.hitCounter();
      return (double) counter.current() / (double) counter.maximum();
    }
    try {
      return Double.parseDouble(progressStr);
    } catch (NumberFormatException ignored) {
      // Fall through
    }
    if (state.maxHealth() > 0) {
      return (double) state.currentHealth() / (double) state.maxHealth();
    }
    return 1.0;
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
