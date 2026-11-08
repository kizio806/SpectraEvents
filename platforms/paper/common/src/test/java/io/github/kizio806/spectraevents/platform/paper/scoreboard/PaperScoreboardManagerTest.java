package io.github.kizio806.spectraevents.platform.paper.scoreboard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperScoreboardManagerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private RegionTaskScheduler noopScheduler;
  private LocaleCatalog locales;
  private PaperScoreboardManager manager;

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
    manager = new PaperScoreboardManager(noopScheduler, locales);
  }

  @AfterEach
  void tearDown() {
    MockBukkitSetup.resetCustomDelegates();
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(NullPointerException.class, () -> new PaperScoreboardManager(null, locales));
    assertThrows(NullPointerException.class, () -> new PaperScoreboardManager(noopScheduler, null));
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

  @Test
  void testShowUpdateAndRemoveScoreboard() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            id,
            new EventDefinitionId("boss_event"),
            EventLifecycleState.RUNNING,
            new PhaseId("phase_1"));
    EventRuntimeState state = new EventRuntimeState(id);

    Player mockPlayer = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "ScoreboardPlayer");
    MockBukkitSetup.registerPlayer(mockPlayer);

    // Empty scoreboard attach test
    manager.attachPlayer(mockPlayer);

    // Show with custom title
    manager.showScoreboard(instance, state, "Boss Battle", List.of("Line 1", "Line 2"));

    // Attempting to show for another event when one is already active throws IllegalStateException
    EventInstance anotherInstance =
        EventInstance.create(EventInstanceId.generate(), new EventDefinitionId("other_event"));
    assertThrows(
        IllegalStateException.class,
        () ->
            manager.showScoreboard(
                anotherInstance, new EventRuntimeState(anotherInstance.id()), "Other", List.of()));

    // Update existing scoreboard
    manager.updateScoreboard(instance, state, List.of("Line 1 updated", "Line 3 added"));

    // Attach player when scoreboard is active
    manager.attachPlayer(mockPlayer);

    PlayerJoinEvent joinEvent =
        new PlayerJoinEvent(mockPlayer, net.kyori.adventure.text.Component.text("joined"));
    manager.onPlayerJoin(joinEvent);

    // Remove scoreboard
    manager.removeScoreboard(id.value());

    // Update non-existing scoreboard (delegates to showScoreboard)
    manager.updateScoreboard(instance, state, List.of("Restarted Line"));

    // Begin shutdown and remove
    manager.beginShutdown();
    manager.removeScoreboard(id.value());

    // Show with default title and null lines, then removeAll during shutdown
    manager.showScoreboard(instance, state, null, null);
    manager.removeAll();

    MockBukkitSetup.unregisterPlayer(mockPlayer.getUniqueId());
  }

  @Test
  void testFoliaUnsupportedScoreboardsHandling() {
    ScoreboardManager failingManager =
        (ScoreboardManager)
            Proxy.newProxyInstance(
                ScoreboardManager.class.getClassLoader(),
                new Class<?>[] {ScoreboardManager.class},
                (proxy, method, args) -> {
                  if ("getNewScoreboard".equals(method.getName())) {
                    throw new UnsupportedOperationException("Folia does not support scoreboards");
                  }
                  return null;
                });
    MockBukkitSetup.setCustomScoreboardManager(failingManager);

    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("folia_event"));
    EventRuntimeState state = new EventRuntimeState(id);

    // First call catches UnsupportedOperationException, sets scoreboardsSupported = false
    assertDoesNotThrow(
        () -> manager.showScoreboard(instance, state, "Folia Title", List.of("Line")));

    // Second call branches to if (!scoreboardsSupported) { logUnsupported(instanceId); return; }
    assertDoesNotThrow(
        () -> manager.showScoreboard(instance, state, "Folia Title 2", List.of("Line 2")));
  }
}
