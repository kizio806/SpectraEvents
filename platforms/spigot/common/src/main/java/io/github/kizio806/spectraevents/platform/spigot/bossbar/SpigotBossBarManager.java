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

  private final Map<EventInstanceId, BossBar> bossBars = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, String> bossBarTemplates = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, String> bossBarProgressModes = new ConcurrentHashMap<>();
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
    String renderedTitle = templateRenderer.apply(title, instance);
    BossBar bar = Bukkit.createBossBar(renderedTitle, color, style);
    bar.setProgress(progress);
    for (Player player : Bukkit.getOnlinePlayers()) {
      bar.addPlayer(player);
    }
    bossBars.put(instance.id(), bar);
    bossBarTemplates.put(instance.id(), title != null ? title : "");
    bossBarProgressModes.put(instance.id(), progressStr != null ? progressStr : "");
  }

  public void updateBossBar(
      EventInstance instance,
      EventRuntimeState state,
      String title,
      String progressStr,
      float calculatedProgress) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    BossBar bar = bossBars.get(instance.id());
    if (bar != null) {
      bossBarTemplates.put(instance.id(), title != null ? title : "");
      bossBarProgressModes.put(instance.id(), progressStr != null ? progressStr : "");
      bar.setTitle(templateRenderer.apply(title, instance));
      bar.setProgress(Math.max(0.0, Math.min(1.0, calculatedProgress)));
    }
  }

  public void removeBossBar(EventInstanceId instanceId) {
    BossBar bar = bossBars.remove(instanceId);
    if (bar != null) {
      bar.removeAll();
    }
    bossBarTemplates.remove(instanceId);
    bossBarProgressModes.remove(instanceId);
  }

  public void onPlayerJoin(Player player) {
    for (BossBar bar : bossBars.values()) {
      bar.addPlayer(player);
    }
  }

  public void cleanup(EventInstanceId instanceId) {
    removeBossBar(instanceId);
  }

  public void shutdown() {
    for (BossBar bar : bossBars.values()) {
      bar.removeAll();
    }
    bossBars.clear();
    bossBarTemplates.clear();
    bossBarProgressModes.clear();
  }

  public boolean hasBossBar(EventInstanceId instanceId) {
    return bossBars.containsKey(instanceId);
  }

  public void refreshHud(
      EventInstance instance, EventRuntimeState state, float calculatedProgress) {
    BossBar bar = bossBars.get(instance.id());
    if (bar != null) {
      String template = bossBarTemplates.getOrDefault(instance.id(), bar.getTitle());
      bar.setTitle(templateRenderer.apply(template, instance));
      bar.setProgress(Math.max(0.0, Math.min(1.0, calculatedProgress)));
    }
  }
}
