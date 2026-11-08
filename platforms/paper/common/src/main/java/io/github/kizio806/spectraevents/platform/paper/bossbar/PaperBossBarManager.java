package io.github.kizio806.spectraevents.platform.paper.bossbar;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.platform.bukkit.bossbar.BukkitBossBarHolder;
import io.github.kizio806.spectraevents.platform.bukkit.bossbar.BukkitBossBarProgressCalculator;
import io.github.kizio806.spectraevents.platform.bukkit.common.EventDisplayPlaceholders;
import io.github.kizio806.spectraevents.platform.paper.integration.MiniPlaceholdersIntegration;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import java.util.Locale;
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
public final class PaperBossBarManager implements Listener {
  private static final int MAX_VISIBLE_EVENT_BOSS_BARS = 3;

  private final Map<UUID, BukkitBossBarHolder<BossBar>> activeBossBars = new ConcurrentHashMap<>();
  private final RegionTaskScheduler scheduler;
  private final LocaleCatalog locales;
  private volatile boolean shuttingDown;

  public PaperBossBarManager(RegionTaskScheduler scheduler, LocaleCatalog locales) {
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    this.locales = Objects.requireNonNull(locales, "locales");
  }

  public void showBossBar(
      EventInstance instance,
      EventRuntimeState state,
      String title,
      String color,
      String style,
      String progress) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    UUID instanceId = instance.id().value();
    if (!activeBossBars.containsKey(instanceId)
        && activeBossBars.size() >= MAX_VISIBLE_EVENT_BOSS_BARS) {
      return;
    }
    String titleTemplate =
        (title != null && !title.isBlank())
            ? title
            : locales.message("messages.default-bossbar-title");
    BossBar.Color bossColor = parseColor(color);
    BossBar.Overlay overlay = parseOverlay(style);
    String progressMode = (progress != null && !progress.isBlank()) ? progress : "1.0";

    float progressValue = BukkitBossBarProgressCalculator.calculateFloat(state, progressMode);
    Component titleComponent = renderTitle(titleTemplate, instance, state);

    BossBar bossBar = BossBar.bossBar(titleComponent, progressValue, bossColor, overlay);
    BukkitBossBarHolder<BossBar> holder =
        new BukkitBossBarHolder<>(bossBar, titleTemplate, progressMode);
    activeBossBars.put(instanceId, holder);

    for (Player player : Bukkit.getOnlinePlayers()) {
      scheduler.executeFor(player, () -> player.showBossBar(bossBar));
    }
  }

  public void updateBossBar(
      EventInstance instance, EventRuntimeState state, String title, String progress) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    UUID instanceId = instance.id().value();
    BukkitBossBarHolder<BossBar> holder = activeBossBars.get(instanceId);
    if (holder == null) {
      showBossBar(instance, state, title, null, null, progress);
      return;
    }

    if (title != null && !title.isBlank()) {
      holder.setTitleTemplate(title);
    }
    if (progress != null && !progress.isBlank()) {
      holder.setProgressMode(progress);
    }

    float progressValue =
        BukkitBossBarProgressCalculator.calculateFloat(state, holder.progressMode());
    holder.bossBar().progress(Math.max(0.0f, Math.min(1.0f, progressValue)));
    holder.bossBar().name(renderTitle(holder.titleTemplate(), instance, state));
  }

  /**
   * Refreshes the live value from runtime state; callers invoke this at a bounded one-second rate.
   */
  public void refreshBossBar(EventInstance instance, EventRuntimeState state) {
    BukkitBossBarHolder<BossBar> holder = activeBossBars.get(instance.id().value());
    if (holder == null) {
      return;
    }
    holder
        .bossBar()
        .progress(BukkitBossBarProgressCalculator.calculateFloat(state, holder.progressMode()));
    holder.bossBar().name(renderTitle(holder.titleTemplate(), instance, state));
  }

  public void removeBossBar(UUID instanceId) {
    if (instanceId == null) {
      return;
    }
    BukkitBossBarHolder<BossBar> holder = activeBossBars.remove(instanceId);
    if (holder != null) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (shuttingDown) {
          player.hideBossBar(holder.bossBar());
        } else {
          scheduler.executeFor(player, () -> player.hideBossBar(holder.bossBar()));
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
    for (BukkitBossBarHolder<BossBar> holder : activeBossBars.values()) {
      scheduler.executeFor(player, () -> player.showBossBar(holder.bossBar()));
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

  private BossBar.Color parseColor(String colorStr) {
    if (colorStr == null) {
      return BossBar.Color.PURPLE;
    }
    try {
      return BossBar.Color.valueOf(colorStr.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return BossBar.Color.PURPLE;
    }
  }

  private BossBar.Overlay parseOverlay(String styleStr) {
    if (styleStr == null) {
      return BossBar.Overlay.PROGRESS;
    }
    try {
      String upper = styleStr.toUpperCase(Locale.ROOT);
      if (upper.startsWith("NOTCH")) {
        return BossBar.Overlay.valueOf(upper.replace("NOTCH_", "NOTCHES_"));
      }
      return BossBar.Overlay.valueOf(upper);
    } catch (IllegalArgumentException e) {
      return BossBar.Overlay.PROGRESS;
    }
  }
}
