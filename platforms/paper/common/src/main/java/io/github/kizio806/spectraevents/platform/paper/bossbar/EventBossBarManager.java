package io.github.kizio806.spectraevents.platform.paper.bossbar;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.platform.paper.common.EventDisplayPlaceholders;
import io.github.kizio806.spectraevents.platform.paper.integration.MiniPlaceholdersIntegration;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Manages Kyori Adventure BossBars for active SpectraEvents instances on Paper platform. */
public final class EventBossBarManager implements Listener {
  private static final int MAX_VISIBLE_EVENT_BOSS_BARS = 3;

  private static final class BossBarHolder {
    final BossBar bossBar;
    String titleTemplate;
    String progressMode;

    BossBarHolder(BossBar bossBar, String titleTemplate, String progressMode) {
      this.bossBar = bossBar;
      this.titleTemplate = titleTemplate;
      this.progressMode = progressMode;
    }
  }

  private final Map<UUID, BossBarHolder> activeBossBars = new ConcurrentHashMap<>();
  private final RegionTaskScheduler scheduler;
  private final LocaleCatalog locales;
  private volatile boolean shuttingDown;

  public EventBossBarManager(RegionTaskScheduler scheduler, LocaleCatalog locales) {
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    this.locales = Objects.requireNonNull(locales, "locales");
  }

  public void showBossBar(
      EventInstance instance, EventRuntimeState state, Map<String, Object> params) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    UUID instanceId = instance.id().value();
    if (!activeBossBars.containsKey(instanceId)
        && activeBossBars.size() >= MAX_VISIBLE_EVENT_BOSS_BARS) {
      return;
    }
    String titleTemplate =
        getString(params, "title", locales.message("messages.default-bossbar-title"));
    BossBar.Color color = parseColor(getString(params, "color", "PURPLE"));
    BossBar.Overlay overlay = parseOverlay(getString(params, "style", "PROGRESS"));
    String progressMode = getString(params, "progress", "1.0");

    float progress = calculateProgress(state, params);
    Component titleComponent = renderTitle(titleTemplate, instance, state);

    BossBar bossBar = BossBar.bossBar(titleComponent, progress, color, overlay);
    BossBarHolder holder = new BossBarHolder(bossBar, titleTemplate, progressMode);
    activeBossBars.put(instanceId, holder);

    for (Player player : Bukkit.getOnlinePlayers()) {
      scheduler.executeFor(player, () -> player.showBossBar(bossBar));
    }
  }

  public void updateBossBar(
      EventInstance instance, EventRuntimeState state, Map<String, Object> params) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    UUID instanceId = instance.id().value();
    BossBarHolder holder = activeBossBars.get(instanceId);
    if (holder == null) {
      showBossBar(instance, state, params);
      return;
    }

    if (params.containsKey("title")) {
      holder.titleTemplate = getString(params, "title", holder.titleTemplate);
    }
    if (params.containsKey("progress")) {
      holder.progressMode = getString(params, "progress", holder.progressMode);
    }
    if (params.containsKey("color")) {
      holder.bossBar.color(parseColor(getString(params, "color", "PURPLE")));
    }
    if (params.containsKey("style")) {
      holder.bossBar.overlay(parseOverlay(getString(params, "style", "PROGRESS")));
    }

    float progress = calculateProgress(state, params);
    holder.bossBar.progress(Math.max(0.0f, Math.min(1.0f, progress)));
    holder.bossBar.name(renderTitle(holder.titleTemplate, instance, state));
  }

  /**
   * Refreshes the live value from runtime state; callers invoke this at a bounded one-second rate.
   */
  public void refreshBossBar(EventInstance instance, EventRuntimeState state) {
    BossBarHolder holder = activeBossBars.get(instance.id().value());
    if (holder == null) {
      return;
    }
    holder.bossBar.progress(calculateProgress(state, Map.of("progress", holder.progressMode)));
    holder.bossBar.name(renderTitle(holder.titleTemplate, instance, state));
  }

  public void removeBossBar(UUID instanceId) {
    if (instanceId == null) return;
    BossBarHolder holder = activeBossBars.remove(instanceId);
    if (holder != null) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (shuttingDown) {
          player.hideBossBar(holder.bossBar);
        } else {
          scheduler.executeFor(player, () -> player.hideBossBar(holder.bossBar));
        }
      }
    }
  }

  public void removeAll() {
    for (UUID id : activeBossBars.keySet()) {
      removeBossBar(id);
    }
    activeBossBars.clear();
  }

  /** Switches cleanup to direct server-shutdown operations after scheduler registration closes. */
  public void beginShutdown() {
    shuttingDown = true;
  }

  public void attachPlayer(Player player) {
    for (BossBarHolder holder : activeBossBars.values()) {
      scheduler.executeFor(player, () -> player.showBossBar(holder.bossBar));
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    attachPlayer(event.getPlayer());
  }

  private Component renderTitle(String template, EventInstance instance, EventRuntimeState state) {
    return MiniPlaceholdersIntegration.getMiniMessage()
        .deserialize(
            EventDisplayPlaceholders.resolve(locales.resolveTemplate(template), instance, state));
  }

  private float calculateProgress(EventRuntimeState state, Map<String, Object> params) {
    if ("hits".equalsIgnoreCase(String.valueOf(params.get("progress")))
        && state.hitCounter() != null) {
      var counter = state.hitCounter();
      return (float) counter.current() / (float) counter.maximum();
    }
    if (params.containsKey("progress")) {
      try {
        return Float.parseFloat(String.valueOf(params.get("progress")));
      } catch (NumberFormatException ignored) {
        // Ignored, fallback to health calculation
      }
    }
    if (state.maxHealth() > 0) {
      return (float) state.currentHealth() / (float) state.maxHealth();
    }
    return 1.0f;
  }

  private BossBar.Color parseColor(String colorStr) {
    try {
      return BossBar.Color.valueOf(colorStr.toUpperCase(java.util.Locale.ROOT));
    } catch (Exception e) {
      return BossBar.Color.PURPLE;
    }
  }

  private BossBar.Overlay parseOverlay(String styleStr) {
    try {
      String upper = styleStr.toUpperCase(java.util.Locale.ROOT);
      if (upper.startsWith("NOTCH")) {
        return BossBar.Overlay.valueOf(upper.replace("NOTCH_", "NOTCHES_"));
      }
      return BossBar.Overlay.valueOf(upper);
    } catch (Exception e) {
      return BossBar.Overlay.PROGRESS;
    }
  }

  private String getString(Map<String, Object> params, String key, String defaultValue) {
    Object val = params.get(key);
    return val != null ? String.valueOf(val) : defaultValue;
  }
}
