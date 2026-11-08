package io.github.kizio806.spectraevents.platform.spigot.bossbar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotBossBarManagerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private SpigotBossBarManager manager;

  @BeforeEach
  void setUp() {
    manager = new SpigotBossBarManager((template, instance) -> template != null ? template : "");
  }

  @Test
  void rejectsNullTemplateRenderer() {
    assertThrows(NullPointerException.class, () -> new SpigotBossBarManager(null));
  }

  @Test
  void reportsFalseForNonExistentBossBar() {
    EventInstanceId id = EventInstanceId.generate();
    assertFalse(manager.hasBossBar(id));
  }

  @Test
  void returnsDefaultProgressModeWhenAbsent() {
    EventInstanceId id = EventInstanceId.generate();
    assertEquals("1.0", manager.getProgressMode(id));
  }

  @Test
  void handlesGracefulShutdownAndUnknownRemovals() {
    EventInstanceId id = EventInstanceId.generate();
    assertDoesNotThrow(() -> manager.removeBossBar(id));
    assertDoesNotThrow(() -> manager.cleanup(id));
    assertDoesNotThrow(manager::shutdown);
    assertFalse(manager.hasBossBar(id));
  }

  @Test
  void onPlayerJoinWithNoActiveBarsDoesNotThrow() {
    Player dummyPlayer =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> null);

    assertDoesNotThrow(() -> manager.onPlayerJoin(dummyPlayer));
  }

  @Test
  void validatesNullArgumentsOnShowAndUpdate() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);

    assertThrows(
        NullPointerException.class,
        () -> manager.showBossBar(null, state, "title", "PURPLE", "SOLID", "1.0", 1.0f));
    assertThrows(
        NullPointerException.class,
        () ->
            manager.showBossBar(
                EventInstance.create(id, new EventDefinitionId("test")),
                null,
                "title",
                "PURPLE",
                "SOLID",
                "1.0",
                1.0f));

    assertThrows(
        NullPointerException.class, () -> manager.updateBossBar(null, state, "title", "1.0", 1.0f));
    assertThrows(
        NullPointerException.class,
        () ->
            manager.updateBossBar(
                EventInstance.create(id, new EventDefinitionId("test")),
                null,
                "title",
                "1.0",
                1.0f));
  }

  @Test
  void testShowBossBarVariantsAndLimits() {
    Player mockPlayer = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "BossBarViewer");
    MockBukkitSetup.registerPlayer(mockPlayer);

    try {
      EventInstanceId id1 = EventInstanceId.generate();
      EventInstance inst1 = EventInstance.create(id1, new EventDefinitionId("event1"));
      EventRuntimeState state1 = new EventRuntimeState(id1);

      // 1. Show with normal color and solid style
      manager.showBossBar(inst1, state1, "Boss 1", "RED", "SOLID", "countdown", 0.5f);
      assertTrue(manager.hasBossBar(id1));
      assertEquals("countdown", manager.getProgressMode(id1));

      // 2. Show with null color, null style, null title, null progressStr
      EventInstanceId id2 = EventInstanceId.generate();
      EventInstance inst2 = EventInstance.create(id2, new EventDefinitionId("event2"));
      EventRuntimeState state2 = new EventRuntimeState(id2);
      manager.showBossBar(inst2, state2, null, null, null, null, -0.5f);
      assertTrue(manager.hasBossBar(id2));
      assertEquals("", manager.getProgressMode(id2));

      // 3. Show with style transformations: NOTCH_6, NOTCHES_10, PROGRESS, invalid
      EventInstanceId id3 = EventInstanceId.generate();
      EventInstance inst3 = EventInstance.create(id3, new EventDefinitionId("event3"));
      EventRuntimeState state3 = new EventRuntimeState(id3);
      manager.showBossBar(inst3, state3, "Boss 3", "INVALID_COLOR", "NOTCH_6", "1.0", 1.5f);
      assertTrue(manager.hasBossBar(id3));

      // 4. At MAX_VISIBLE_EVENT_BOSS_BARS (3) limit: adding 4th event returns early
      EventInstanceId id4 = EventInstanceId.generate();
      EventInstance inst4 = EventInstance.create(id4, new EventDefinitionId("event4"));
      EventRuntimeState state4 = new EventRuntimeState(id4);
      manager.showBossBar(inst4, state4, "Boss 4", "BLUE", "SOLID", "1.0", 1.0f);
      assertFalse(manager.hasBossBar(id4));

      // Updating existing inst1 while at limit works
      manager.showBossBar(inst1, state1, "Boss 1 Updated", "BLUE", "NOTCHES_10", "1.0", 0.8f);
      assertTrue(manager.hasBossBar(id1));

      // 5. Update boss bar with title and progress mode
      manager.updateBossBar(inst1, state1, "Boss 1 Final", "final-mode", 0.9f);
      assertEquals("final-mode", manager.getProgressMode(id1));

      // Update boss bar with null title and null progress mode
      manager.updateBossBar(inst1, state1, null, null, 1.0f);

      // Update non-existent boss bar
      manager.updateBossBar(inst4, state4, "Boss 4", "mode", 0.5f);

      // 6. Refresh HUD
      manager.refreshHud(inst1, 0.4f);
      manager.refreshHud(inst4, 0.4f);

      // 7. On player join adds to all active boss bars
      Player newPlayer = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Joiner");
      manager.onPlayerJoin(newPlayer);

      // 8. Remove boss bar & cleanup
      manager.removeBossBar(id1);
      assertFalse(manager.hasBossBar(id1));
      manager.cleanup(id2);
      assertFalse(manager.hasBossBar(id2));

      // 9. Style tests: PROGRESS and invalid style
      EventInstanceId id5 = EventInstanceId.generate();
      EventInstance inst5 = EventInstance.create(id5, new EventDefinitionId("event5"));
      EventRuntimeState state5 = new EventRuntimeState(id5);
      manager.showBossBar(inst5, state5, "Progress Style", "GREEN", "PROGRESS", "1.0", 0.5f);
      assertTrue(manager.hasBossBar(id5));

      EventInstanceId id6 = EventInstanceId.generate();
      EventInstance inst6 = EventInstance.create(id6, new EventDefinitionId("event6"));
      EventRuntimeState state6 = new EventRuntimeState(id6);
      manager.showBossBar(inst6, state6, "Invalid Style", "GREEN", "TOTALLY_INVALID", "1.0", 0.5f);
      assertTrue(manager.hasBossBar(id6));

      // Shutdown cleans up all
      manager.shutdown();
      assertFalse(manager.hasBossBar(id3));
      assertFalse(manager.hasBossBar(id5));
      assertFalse(manager.hasBossBar(id6));
    } finally {
      MockBukkitSetup.unregisterPlayer(mockPlayer.getUniqueId());
    }
  }
}
