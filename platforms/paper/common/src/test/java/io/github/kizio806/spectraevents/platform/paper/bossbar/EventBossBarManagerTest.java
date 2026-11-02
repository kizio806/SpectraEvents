package io.github.kizio806.spectraevents.platform.paper.bossbar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventBossBarManagerTest {

  private RegionTaskScheduler noopScheduler;
  private LocaleCatalog locales;
  private EventBossBarManager manager;

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

          @Override
          public void executeFor(Player player, Runnable task) {
            task.run();
          }
        };
    locales = LocaleCatalog.of(Map.of("messages.default-bossbar-title", "Default Title"));
    manager = new EventBossBarManager(noopScheduler, locales);
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(NullPointerException.class, () -> new EventBossBarManager(null, locales));
    assertThrows(NullPointerException.class, () -> new EventBossBarManager(noopScheduler, null));
  }

  @Test
  void handlesGracefulShutdownAndNullRemovals() {
    assertDoesNotThrow(() -> manager.removeBossBar(null));
    assertDoesNotThrow(() -> manager.removeBossBar(UUID.randomUUID()));
    assertDoesNotThrow(() -> manager.beginShutdown());
  }
}
