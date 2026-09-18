package io.github.kizio806.spectraevents.platform.paper.bossbar;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.platform.paper.integration.MiniPlaceholdersIntegration;
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

  private static final class BossBarHolder {
    final BossBar bossBar;
    String titleTemplate;

    BossBarHolder(BossBar bossBar, String titleTemplate) {
      this.bossBar = bossBar;
      this.titleTemplate = titleTemplate;
    }
  }

  private final Map<UUID, BossBarHolder> activeBossBars = new ConcurrentHashMap<>();

  public void showBossBar(
      EventInstance instance, EventRuntimeState state, Map<String, Object> params) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    UUID instanceId = instance.id().value();
    String titleTemplate = getString(params, "title", "<gold>Event Active");
    BossBar.Color color = parseColor(getString(params, "color", "PURPLE"));
    BossBar.Overlay overlay = parseOverlay(getString(params, "style", "PROGRESS"));

    float progress = calculateProgress(state, params);
    Component titleComponent = renderTitle(titleTemplate, instance, state);

    BossBar bossBar = BossBar.bossBar(titleComponent, progress, color, overlay);
    BossBarHolder holder = new BossBarHolder(bossBar, titleTemplate);
    activeBossBars.put(instanceId, holder);

    for (Player player : Bukkit.getOnlinePlayers()) {
      player.showBossBar(bossBar);
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

  public void removeBossBar(UUID instanceId) {
    if (instanceId == null) return;
    BossBarHolder holder = activeBossBars.remove(instanceId);
    if (holder != null) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        player.hideBossBar(holder.bossBar);
      }
    }
  }

  public void removeAll() {
    for (UUID id : activeBossBars.keySet()) {
      removeBossBar(id);
    }
    activeBossBars.clear();
  }

  public void attachPlayer(Player player) {
    for (BossBarHolder holder : activeBossBars.values()) {
      player.showBossBar(holder.bossBar);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    attachPlayer(event.getPlayer());
  }

  private Component renderTitle(String template, EventInstance instance, EventRuntimeState state) {
    String currentPhase = instance.currentPhase().map(p -> p.value()).orElse("active");
    int hp = state.currentHealth();
    int maxHp = Math.max(1, state.maxHealth());
    int percent = (int) (((double) hp / maxHp) * 100);

    String rendered =
        template
            .replace("%health%", String.valueOf(hp))
            .replace("%max_health%", String.valueOf(maxHp))
            .replace("%health_percent%", String.valueOf(percent))
            .replace("%phase%", currentPhase)
            .replace("%event%", instance.definitionId().value());

    return MiniPlaceholdersIntegration.getMiniMessage().deserialize(rendered);
  }

  private float calculateProgress(EventRuntimeState state, Map<String, Object> params) {
    if (params.containsKey("progress")) {
      try {
        return Float.parseFloat(String.valueOf(params.get("progress")));
      } catch (Exception ignored) {
      }
    }
    if (state.maxHealth() > 0) {
      return (float) state.currentHealth() / (float) state.maxHealth();
    }
    return 1.0f;
  }

  private BossBar.Color parseColor(String colorStr) {
    try {
      return BossBar.Color.valueOf(colorStr.toUpperCase());
    } catch (Exception e) {
      return BossBar.Color.PURPLE;
    }
  }

  private BossBar.Overlay parseOverlay(String styleStr) {
    try {
      String upper = styleStr.toUpperCase();
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
