package io.github.kizio806.spectraevents.platform.spigot.scoreboard;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Manages Bukkit Scoreboards for active SpectraEvents instances on the Spigot platform. */
public final class SpigotScoreboardManager {

  private final Map<EventInstanceId, Scoreboard> scoreboards = new ConcurrentHashMap<>();
  private final BiFunction<String, EventInstance, String> templateRenderer;

  public SpigotScoreboardManager(BiFunction<String, EventInstance, String> templateRenderer) {
    this.templateRenderer = Objects.requireNonNull(templateRenderer, "templateRenderer");
  }

  public void showScoreboard(
      EventInstance instance, EventRuntimeState state, String title, List<String> lines) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    Scoreboard board = scoreboardManager().getNewScoreboard();
    String renderedTitle = templateRenderer.apply(title, instance);
    Objective obj =
        io.github.kizio806.spectraevents.platform.bukkit.compat.BukkitPlatformBridge
            .registerNewObjective(
                board,
                "event_" + instance.id().toString().substring(0, 8),
                "dummy",
                renderedTitle,
                net.kyori.adventure.text.Component.text(renderedTitle));
    obj.setDisplaySlot(DisplaySlot.SIDEBAR);

    List<String> safeLines = lines != null ? lines : List.of();
    for (int i = 0; i < safeLines.size(); i++) {
      String line = safeLines.get(i);
      int score = safeLines.size() - i;
      obj.getScore(templateRenderer.apply(line, instance)).setScore(score);
    }

    scoreboards.put(instance.id(), board);
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.setScoreboard(board);
    }
  }

  public boolean hasScoreboard(EventInstanceId instanceId) {
    return scoreboards.containsKey(instanceId);
  }

  public void updateScoreboard(
      EventInstance instance, EventRuntimeState state, List<String> lines) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(state, "state");

    Scoreboard board = scoreboards.get(instance.id());
    if (board != null) {
      Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
      if (obj != null) {
        for (String entry : board.getEntries()) {
          board.resetScores(entry);
        }
        List<String> safeLines = lines != null ? lines : List.of();
        for (int i = 0; i < safeLines.size(); i++) {
          String line = safeLines.get(i);
          int score = safeLines.size() - i;
          obj.getScore(templateRenderer.apply(line, instance)).setScore(score);
        }
      }
    }
  }

  public void removeScoreboard(EventInstanceId instanceId) {
    Scoreboard board = scoreboards.remove(instanceId);
    if (board != null) {
      org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
      if (manager != null) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (player.getScoreboard().equals(board)) {
            player.setScoreboard(manager.getMainScoreboard());
          }
        }
      }
    }
  }

  public void onPlayerJoin(Player player) {
    for (Scoreboard board : scoreboards.values()) {
      player.setScoreboard(board);
    }
  }

  public void cleanup(EventInstanceId instanceId) {
    removeScoreboard(instanceId);
  }

  public void shutdown() {
    org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
    if (manager != null) {
      for (Scoreboard board : scoreboards.values()) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (player.getScoreboard().equals(board)) {
            player.setScoreboard(manager.getMainScoreboard());
          }
        }
      }
    }
    scoreboards.clear();
  }

  private org.bukkit.scoreboard.ScoreboardManager scoreboardManager() {
    org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
    if (manager == null) {
      throw new IllegalStateException("Bukkit ScoreboardManager is not available");
    }
    return manager;
  }

  public int activeCount() {
    return scoreboards.size();
  }
}
