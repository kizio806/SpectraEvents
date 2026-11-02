package io.github.kizio806.spectraevents.platform.paper.scoreboard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventScoreboardManagerTest {

  private RegionTaskScheduler noopScheduler;
  private LocaleCatalog locales;
  private EventScoreboardManager manager;

  @BeforeEach
  void setUp() {
    noopScheduler =
        new RegionTaskScheduler() {
          @Override
          public void executeGlobal(Runnable task) {
            task.run();
          }

          @Override
          public void executeAt(Location location, Runnable task) {
            task.run();
          }

          @Override
          public void executeFor(Entity entity, Runnable task) {
            task.run();
          }
        };
    locales = LocaleCatalog.of(Map.of("messages.default-scoreboard-title", "Default Title"));
    manager = new EventScoreboardManager(noopScheduler, locales);
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(NullPointerException.class, () -> new EventScoreboardManager(null, locales));
    assertThrows(NullPointerException.class, () -> new EventScoreboardManager(noopScheduler, null));
  }

  @Test
  void rejectsNullArgumentsOnShowAndUpdate() {
    assertThrows(
        NullPointerException.class, () -> manager.showScoreboard(null, null, "Title", List.of()));
    assertThrows(NullPointerException.class, () -> manager.updateScoreboard(null, null, List.of()));
  }

  @Test
  void handlesGracefulShutdownAndRemovals() {
    assertDoesNotThrow(() -> manager.removeScoreboard(null));
    assertDoesNotThrow(() -> manager.removeScoreboard(UUID.randomUUID()));
    assertDoesNotThrow(manager::beginShutdown);
  }
}
