package io.github.kizio806.spectraevents.platform.paper.bossbar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperBossBarManagerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private RegionTaskScheduler noopScheduler;
  private LocaleCatalog locales;
  private PaperBossBarManager manager;

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
    locales =
        LocaleCatalog.of(
            Map.of(
                "messages.default-bossbar-title", "Default Title",
                "custom-title", "Custom Event Title"));
    manager = new PaperBossBarManager(noopScheduler, locales);
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(NullPointerException.class, () -> new PaperBossBarManager(null, locales));
    assertThrows(NullPointerException.class, () -> new PaperBossBarManager(noopScheduler, null));
  }

  @Test
  void handlesGracefulShutdownAndNullRemovals() {
    assertDoesNotThrow(() -> manager.removeBossBar(null));
    assertDoesNotThrow(() -> manager.removeBossBar(UUID.randomUUID()));
    assertDoesNotThrow(() -> manager.beginShutdown());
  }

  @Test
  void testShowBossBarVariantsAndLimits() {
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Alice");
    MockBukkitSetup.registerPlayer(player);

    EventInstance inst1 =
        EventInstance.create(EventInstanceId.generate(), new EventDefinitionId("meteor"));
    EventRuntimeState state1 = new EventRuntimeState(inst1.id());

    // Show with default title, null color, null style, null progress
    assertDoesNotThrow(() -> manager.showBossBar(inst1, state1, null, null, null, null));

    // Show with blank title, invalid color, notch overlay, custom progress
    EventInstance inst2 =
        EventInstance.create(EventInstanceId.generate(), new EventDefinitionId("dragon"));
    EventRuntimeState state2 = new EventRuntimeState(inst2.id());
    assertDoesNotThrow(
        () -> manager.showBossBar(inst2, state2, "   ", "UNKNOWN_COLOR", "NOTCH_6", "0.75"));

    // Show with custom title, valid color, valid style
    EventInstance inst3 =
        EventInstance.create(EventInstanceId.generate(), new EventDefinitionId("boss"));
    EventRuntimeState state3 = new EventRuntimeState(inst3.id());
    assertDoesNotThrow(
        () -> manager.showBossBar(inst3, state3, "custom-title", "RED", "PROGRESS", "0.5"));

    // Max visible is 3: 4th instance should be skipped cleanly
    EventInstance inst4 =
        EventInstance.create(EventInstanceId.generate(), new EventDefinitionId("extra"));
    EventRuntimeState state4 = new EventRuntimeState(inst4.id());
    assertDoesNotThrow(() -> manager.showBossBar(inst4, state4, "extra", "BLUE", "SOLID", "1.0"));

    // Attach player and handle onPlayerJoin
    assertDoesNotThrow(() -> manager.attachPlayer(player));
    assertDoesNotThrow(() -> manager.onPlayerJoin(new PlayerJoinEvent(player, Component.empty())));

    // Update existing boss bar
    assertDoesNotThrow(() -> manager.updateBossBar(inst1, state1, "Updated Title", "0.2"));
    assertDoesNotThrow(() -> manager.updateBossBar(inst1, state1, null, null));

    // Refresh existing boss bar
    assertDoesNotThrow(() -> manager.refreshBossBar(inst1, state1));

    // Refresh non-existent boss bar
    assertDoesNotThrow(() -> manager.refreshBossBar(inst4, state4));

    // Update non-existing boss bar (delegates to showBossBar)
    EventInstance inst5 =
        EventInstance.create(EventInstanceId.generate(), new EventDefinitionId("new_event"));
    assertDoesNotThrow(
        () -> manager.updateBossBar(inst5, new EventRuntimeState(inst5.id()), "New Event", "0.9"));

    // Remove boss bar while running
    assertDoesNotThrow(() -> manager.removeBossBar(inst1.id().value()));

    // Begin shutdown and remove boss bar
    manager.beginShutdown();
    assertDoesNotThrow(() -> manager.removeBossBar(inst2.id().value()));

    // Remove all remaining
    assertDoesNotThrow(() -> manager.removeAll());

    MockBukkitSetup.unregisterPlayer(player.getUniqueId());
  }

  @Test
  void testParseOverlayAndColorFallbacks() {
    EventInstance inst =
        EventInstance.create(EventInstanceId.generate(), new EventDefinitionId("fallback_test"));
    EventRuntimeState state = new EventRuntimeState(inst.id());

    // Test overlay with invalid style
    assertDoesNotThrow(
        () -> manager.showBossBar(inst, state, "Title", "GREEN", "INVALID_STYLE", "1.0"));
    manager.removeAll();

    // Test overlay with NOTCHES_10
    assertDoesNotThrow(
        () -> manager.showBossBar(inst, state, "Title", "YELLOW", "NOTCH_10", "1.0"));
    manager.removeAll();
  }
}
