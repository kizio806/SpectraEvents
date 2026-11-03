package io.github.kizio806.spectraevents.platform.spigot.bossbar;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/** Manages Bukkit BossBars for active SpectraEvents instances on the Spigot platform. */
public final class SpigotBossBarManager {

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

  private final Map<EventInstanceId, BossBarHolder> bossBars = new ConcurrentHashMap<>();
  private final BiFunction<String, EventInstance, String> templateRenderer;

  public SpigotBossBarManager(BiFunction<String, EventInstance, String> templateRenderer) {
    this.templateRenderer = Objects.requireNonNull(templateRenderer, "templateRenderer");
  }

  public void showBossBar(
      EventInstance instance,
      EventRuntimeState state,
      String title,
      String colorStr,
      String styleStr,
      String progressStr,
      float calculatedProgress) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    if (!bossBars.containsKey(instance.id()) && bossBars.size() >= MAX_VISIBLE_EVENT_BOSS_BARS) {
      return;
    }

    BarColor color;
    try {
      color =
          colorStr != null ? BarColor.valueOf(colorStr.toUpperCase(Locale.ROOT)) : BarColor.PURPLE;
    } catch (IllegalArgumentException e) {
      color = BarColor.PURPLE;
    }

    BarStyle style;
    try {
      String upper = styleStr != null ? styleStr.toUpperCase(Locale.ROOT) : "SOLID";
      if (upper.startsWith("NOTCH_")) {
        upper = upper.replace("NOTCH_", "SEGMENTED_");
      } else if (upper.startsWith("NOTCHES_")) {
        upper = upper.replace("NOTCHES_", "SEGMENTED_");
      } else if ("PROGRESS".equals(upper)) {
        upper = "SOLID";
      }
      style = BarStyle.valueOf(upper);
    } catch (IllegalArgumentException e) {
      style = BarStyle.SOLID;
    }

    double progress = Math.max(0.0, Math.min(1.0, calculatedProgress));
    String titleTemplate = title != null ? title : "";
    String renderedTitle = templateRenderer.apply(titleTemplate, instance);
    BossBar bar = Bukkit.createBossBar(renderedTitle, color, style);
    bar.setProgress(progress);
    for (Player player : Bukkit.getOnlinePlayers()) {
      bar.addPlayer(player);
    }
    bossBars.put(
        instance.id(),
        new BossBarHolder(bar, titleTemplate, progressStr != null ? progressStr : ""));
  }

  public void updateBossBar(
      EventInstance instance,
      EventRuntimeState state,
      String title,
      String progressStr,
      float calculatedProgress) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    BossBarHolder holder = bossBars.get(instance.id());
    if (holder != null) {
      if (title != null) {
        holder.titleTemplate = title;
      }
      if (progressStr != null) {
        holder.progressMode = progressStr;
      }
      holder.bossBar.setTitle(templateRenderer.apply(holder.titleTemplate, instance));
      holder.bossBar.setProgress(Math.max(0.0, Math.min(1.0, calculatedProgress)));
    }
  }

  public void removeBossBar(EventInstanceId instanceId) {
    BossBarHolder holder = bossBars.remove(instanceId);
    if (holder != null) {
      holder.bossBar.removeAll();
    }
  }

  public void onPlayerJoin(Player player) {
    for (BossBarHolder holder : bossBars.values()) {
      holder.bossBar.addPlayer(player);
    }
  }

  public void cleanup(EventInstanceId instanceId) {
    removeBossBar(instanceId);
  }

  public void shutdown() {
    for (BossBarHolder holder : bossBars.values()) {
      holder.bossBar.removeAll();
    }
    bossBars.clear();
  }

  public boolean hasBossBar(EventInstanceId instanceId) {
    return bossBars.containsKey(instanceId);
  }

  public String getProgressMode(EventInstanceId instanceId) {
    BossBarHolder holder = bossBars.get(instanceId);
    return holder != null ? holder.progressMode : "1.0";
  }

  public void refreshHud(
      EventInstance instance, EventRuntimeState state, float calculatedProgress) {
    BossBarHolder holder = bossBars.get(instance.id());
    if (holder != null) {
      holder.bossBar.setTitle(templateRenderer.apply(holder.titleTemplate, instance));
      holder.bossBar.setProgress(Math.max(0.0, Math.min(1.0, calculatedProgress)));
    }
  }
}
