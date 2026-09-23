package io.github.kizio806.spectraevents.platform.paper.scoreboard;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.platform.paper.common.EventDisplayPlaceholders;
import io.github.kizio806.spectraevents.platform.paper.integration.MiniPlaceholdersIntegration;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
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

  private static final Logger LOGGER = Logger.getLogger(EventScoreboardManager.class.getName());

  private static final class ScoreboardHolder {
    final Scoreboard scoreboard;
    final Objective objective;
    final List<String> renderedEntries = new ArrayList<>();
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
  private final RegionTaskScheduler scheduler;
  private volatile boolean scoreboardsSupported = true;

  public EventScoreboardManager(RegionTaskScheduler scheduler) {
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
  }

  public void showScoreboard(
      EventInstance instance, EventRuntimeState state, Map<String, Object> params) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    UUID instanceId = instance.id().value();
    if (!scoreboardsSupported) {
      logUnsupported(instanceId);
      return;
    }
    if (activeScoreboards.keySet().stream().anyMatch(id -> !id.equals(instanceId))) {
      throw new IllegalStateException(
          "A Minecraft client can display only one sidebar; another event already owns it");
    }
    String titleTemplate = getString(params, "title", "<gold>SpectraEvents");
    List<String> lineTemplates = getList(params, "lines");

    Scoreboard scoreboard;
    Objective objective;
    try {
      scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
      objective =
          scoreboard.registerNewObjective(
              "se_" + instanceId.toString().substring(0, 8),
              Criteria.DUMMY,
              renderComponent(titleTemplate, instance, state));
    } catch (UnsupportedOperationException unsupportedScoreboards) {
      // Folia deliberately leaves Bukkit scoreboard creation and objective registration
      // unsupported. Treat this presentation capability as explicitly unavailable rather than
      // failing the event or pretending it was displayed.
      scoreboardsSupported = false;
      logUnsupported(instanceId);
      return;
    }
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    ScoreboardHolder holder =
        new ScoreboardHolder(scoreboard, objective, titleTemplate, lineTemplates);
    activeScoreboards.put(instanceId, holder);

    renderLines(holder, instance, state);

    for (Player player : Bukkit.getOnlinePlayers()) {
      scheduler.executeFor(player, () -> player.setScoreboard(holder.scoreboard));
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
        scheduler.executeFor(
            player,
            () -> {
              if (player.getScoreboard().equals(holder.scoreboard)) {
                player.setScoreboard(mainScoreboard);
              }
            });
      }
      holder.objective.unregister();
    }
  }

  public void removeAll() {
    for (ScoreboardHolder holder : activeScoreboards.values()) {
      Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
      for (Player player : Bukkit.getOnlinePlayers()) {
        scheduler.executeFor(
            player,
            () -> {
              if (player.getScoreboard().equals(holder.scoreboard)) {
                player.setScoreboard(mainScoreboard);
              }
            });
      }
      holder.objective.unregister();
    }
    activeScoreboards.clear();
  }

  public void attachPlayer(Player player) {
    if (!activeScoreboards.isEmpty()) {
      ScoreboardHolder holder = activeScoreboards.values().iterator().next();
      scheduler.executeFor(player, () -> player.setScoreboard(holder.scoreboard));
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    attachPlayer(event.getPlayer());
  }

  private void renderLines(
      ScoreboardHolder holder, EventInstance instance, EventRuntimeState state) {
    Objective objective = holder.objective;

    for (String entry : holder.renderedEntries) {
      objective.getScore(entry).resetScore();
    }
    holder.renderedEntries.clear();

    int score = holder.lineTemplates.size();
    for (String lineTemplate : holder.lineTemplates) {
      Component rendered = renderComponent(lineTemplate, instance, state);
      String legacyStr =
          net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
              .serialize(rendered);
      objective.getScore(legacyStr).setScore(score--);
      holder.renderedEntries.add(legacyStr);
    }
  }

  private Component renderComponent(
      String template, EventInstance instance, EventRuntimeState state) {
    return MiniPlaceholdersIntegration.getMiniMessage()
        .deserialize(EventDisplayPlaceholders.resolve(template, instance, state));
  }

  private void logUnsupported(UUID instanceId) {
    LOGGER.warning(
        "Scoreboard action is unsupported by this server implementation; event "
            + instanceId
            + " continues without a scoreboard. Use a bossbar for Folia-compatible UI.");
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
