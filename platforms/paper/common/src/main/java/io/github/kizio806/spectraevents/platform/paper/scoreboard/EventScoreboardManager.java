package io.github.kizio806.spectraevents.platform.paper.scoreboard;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.platform.paper.integration.MiniPlaceholdersIntegration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Manages sidebar Scoreboards for active SpectraEvents instances on Paper platform. */
public final class EventScoreboardManager implements Listener {

  private static final class ScoreboardHolder {
    final Scoreboard scoreboard;
    final Objective objective;
    String titleTemplate;
    List<String> lineTemplates;

    ScoreboardHolder(
        Scoreboard scoreboard,
        Objective objective,
        String titleTemplate,
        List<String> lineTemplates) {
      this.scoreboard = scoreboard;
      this.objective = objective;
      this.titleTemplate = titleTemplate;
      this.lineTemplates = lineTemplates;
    }
  }

  private final Map<UUID, ScoreboardHolder> activeScoreboards = new ConcurrentHashMap<>();

  public void showScoreboard(
      EventInstance instance, EventRuntimeState state, Map<String, Object> params) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    UUID instanceId = instance.id().value();
    String titleTemplate = getString(params, "title", "<gold>SpectraEvents");
    List<String> lineTemplates = getList(params, "lines");

    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    Objective objective =
        scoreboard.registerNewObjective(
            "se_" + instanceId.toString().substring(0, 8),
            Criteria.DUMMY,
            renderComponent(titleTemplate, instance, state));
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    ScoreboardHolder holder =
        new ScoreboardHolder(scoreboard, objective, titleTemplate, lineTemplates);
    activeScoreboards.put(instanceId, holder);

    renderLines(holder, instance, state);

    for (Player player : Bukkit.getOnlinePlayers()) {
      player.setScoreboard(scoreboard);
    }
  }

  public void updateScoreboard(
      EventInstance instance, EventRuntimeState state, Map<String, Object> params) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    UUID instanceId = instance.id().value();
    ScoreboardHolder holder = activeScoreboards.get(instanceId);
    if (holder == null) {
      showScoreboard(instance, state, params);
      return;
    }

    if (params.containsKey("title")) {
      holder.titleTemplate = getString(params, "title", holder.titleTemplate);
      holder.objective.displayName(renderComponent(holder.titleTemplate, instance, state));
    }
    if (params.containsKey("lines")) {
      holder.lineTemplates = getList(params, "lines");
    }

    renderLines(holder, instance, state);
  }

  public void removeScoreboard(UUID instanceId) {
    if (instanceId == null) return;
    ScoreboardHolder holder = activeScoreboards.remove(instanceId);
    if (holder != null) {
      Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.getScoreboard().equals(holder.scoreboard)) {
          player.setScoreboard(mainScoreboard);
        }
      }
    }
  }

  public void removeAll() {
    Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    for (ScoreboardHolder holder : activeScoreboards.values()) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.getScoreboard().equals(holder.scoreboard)) {
          player.setScoreboard(mainScoreboard);
        }
      }
    }
    activeScoreboards.clear();
  }

  public void attachPlayer(Player player) {
    if (!activeScoreboards.isEmpty()) {
      ScoreboardHolder holder = activeScoreboards.values().iterator().next();
      player.setScoreboard(holder.scoreboard);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    attachPlayer(event.getPlayer());
  }

  private void renderLines(
      ScoreboardHolder holder, EventInstance instance, EventRuntimeState state) {
    Scoreboard scoreboard = holder.scoreboard;
    Objective objective = holder.objective;

    for (String entry : scoreboard.getEntries()) {
      scoreboard.resetScores(entry);
    }

    int score = holder.lineTemplates.size();
    for (String lineTemplate : holder.lineTemplates) {
      Component rendered = renderComponent(lineTemplate, instance, state);
      String legacyStr =
          net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
              .serialize(rendered);
      objective.getScore(legacyStr).setScore(score--);
    }
  }

  private Component renderComponent(
      String template, EventInstance instance, EventRuntimeState state) {
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

  private String getString(Map<String, Object> params, String key, String defaultValue) {
    Object val = params.get(key);
    return val != null ? String.valueOf(val) : defaultValue;
  }

  @SuppressWarnings("unchecked")
  private List<String> getList(Map<String, Object> params, String key) {
    Object val = params.get(key);
    if (val instanceof List<?> l) {
      List<String> list = new ArrayList<>();
      for (Object o : l) {
        list.add(String.valueOf(o));
      }
      return list;
    }
    return List.of();
  }
}
