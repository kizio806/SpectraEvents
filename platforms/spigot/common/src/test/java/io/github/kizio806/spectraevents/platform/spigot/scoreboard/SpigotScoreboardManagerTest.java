package io.github.kizio806.spectraevents.platform.spigot.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotScoreboardManagerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private SpigotScoreboardManager manager;

  @BeforeEach
  void setUp() {
    manager =
        new SpigotScoreboardManager(
            (template, instance) -> template.replace("%name%", instance.definitionId().value()));
  }

  @Test
  void rejectsNullRenderer() {
    assertThrows(NullPointerException.class, () -> new SpigotScoreboardManager(null));
  }

  @Test
  void shouldShowAndUpdateScoreboard() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test_event"));
    EventRuntimeState state = new EventRuntimeState(id);

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Steve");
    MockBukkitSetup.registerPlayer(player);

    try {
      assertFalse(manager.hasScoreboard(id));
      assertEquals(0, manager.activeCount());

      manager.showScoreboard(instance, state, "Title: %name%", List.of("Line 1", "Line 2"));
      assertTrue(manager.hasScoreboard(id));
      assertEquals(1, manager.activeCount());

      // Update lines resets previous scores and sets new ones
      manager.updateScoreboard(
          instance, state, List.of("Line 1 updated", "Line 2 updated", "Line 3"));
      assertTrue(manager.hasScoreboard(id));

      // Update with null lines
      manager.updateScoreboard(instance, state, null);

      // Clear the slot so obj is null on update
      player.getScoreboard().clearSlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
      manager.updateScoreboard(instance, state, List.of("Line after clear"));

      // Update non-existent scoreboard does nothing
      EventInstanceId nonExistent = EventInstanceId.generate();
      manager.updateScoreboard(
          EventInstance.create(nonExistent, new EventDefinitionId("absent")),
          new EventRuntimeState(nonExistent),
          List.of("Line"));

      // Player join attaches active board
      Player newPlayer = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Alex");
      manager.onPlayerJoin(newPlayer);
      assertNotNull(newPlayer.getScoreboard());

      // Remove scoreboard
      manager.removeScoreboard(id);
      assertFalse(manager.hasScoreboard(id));
      assertEquals(0, manager.activeCount());

      // Remove already removed scoreboard
      manager.removeScoreboard(id);
    } finally {
      MockBukkitSetup.unregisterPlayer(player.getUniqueId());
    }
  }

  @Test
  void shouldShowWithNullLines() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("event_null_lines"));
    EventRuntimeState state = new EventRuntimeState(id);

    manager.showScoreboard(instance, state, "Title", null);
    assertTrue(manager.hasScoreboard(id));
    manager.cleanup(id);
    assertFalse(manager.hasScoreboard(id));
  }

  @Test
  void shouldCleanupAndShutdownSafely() {
    EventInstanceId id1 = EventInstanceId.generate();
    EventInstance instance1 = EventInstance.create(id1, new EventDefinitionId("event_1"));
    EventRuntimeState state1 = new EventRuntimeState(id1);

    EventInstanceId id2 = EventInstanceId.generate();
    EventInstance instance2 = EventInstance.create(id2, new EventDefinitionId("event_2"));
    EventRuntimeState state2 = new EventRuntimeState(id2);

    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Player1");
    Player otherPlayer = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "Player2");
    MockBukkitSetup.registerPlayer(player);
    MockBukkitSetup.registerPlayer(otherPlayer);

    try {
      manager.showScoreboard(instance1, state1, "Title 1", List.of("Line 1"));
      manager.showScoreboard(instance2, state2, "Title 2", List.of("Line 2"));
      assertEquals(2, manager.activeCount());

      // Player 2 gets set to main scoreboard so player.getScoreboard().equals(board) is false for
      // player 2
      otherPlayer.setScoreboard(
          Objects.requireNonNull(org.bukkit.Bukkit.getScoreboardManager()).getMainScoreboard());

      manager.cleanup(id1);
      assertEquals(1, manager.activeCount());
      assertFalse(manager.hasScoreboard(id1));
      assertTrue(manager.hasScoreboard(id2));

      manager.shutdown();
      assertEquals(0, manager.activeCount());
    } finally {
      MockBukkitSetup.unregisterPlayer(player.getUniqueId());
      MockBukkitSetup.unregisterPlayer(otherPlayer.getUniqueId());
    }
  }

  @Test
  void shouldHandleNullScoreboardManager() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("event_null_sm"));
    EventRuntimeState state = new EventRuntimeState(id);

    try {
      MockBukkitSetup.setNullScoreboardManager(true);
      assertThrows(
          IllegalStateException.class,
          () -> manager.showScoreboard(instance, state, "Title", List.of("Line 1")));
    } finally {
      MockBukkitSetup.setNullScoreboardManager(false);
    }
  }

  @Test
  void shouldHandleNullScoreboardManagerDuringRemoveAndShutdown() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.create(id, new EventDefinitionId("event_null_sm_remove"));
    EventRuntimeState state = new EventRuntimeState(id);

    manager.showScoreboard(instance, state, "Title", List.of("Line 1"));
    assertTrue(manager.hasScoreboard(id));

    try {
      MockBukkitSetup.setNullScoreboardManager(true);
      // Remove when scoreboardManager is null
      manager.removeScoreboard(id);
      assertFalse(manager.hasScoreboard(id));

      // Shutdown when scoreboardManager is null
      manager.shutdown();
    } finally {
      MockBukkitSetup.setNullScoreboardManager(false);
    }
  }
}
