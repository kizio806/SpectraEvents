package dev.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.spectraevents.application.execution.EventRuntimeState;
import dev.spectraevents.core.event.definition.EventDefinitionId;
import dev.spectraevents.core.event.phase.PhaseId;
import dev.spectraevents.core.event.runtime.EventInstance;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.core.event.runtime.EventLifecycleState;
import dev.spectraevents.core.gameplay.health.Health;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecoveryStressTest {
  private Path dbPath;

  @BeforeEach
  void setUp() throws Exception {
    Class.forName("org.sqlite.JDBC");
    dbPath = Files.createTempFile("recovery_test", ".db");
  }

  @AfterEach
  void tearDown() throws Exception {
    Files.deleteIfExists(dbPath);
  }

  @Test
  void testStateAndInstanceRecovery() throws Exception {
    EventInstanceId id = EventInstanceId.generate();
    EventDefinitionId defId = new EventDefinitionId("test_def");

    // Phase 1: Create and save data
    {
      SQLiteEventInstanceRepository repo1 = new SQLiteEventInstanceRepository(dbPath);
      repo1.initialize();

      EventInstance instance =
          EventInstance.reconstitute(id, defId, EventLifecycleState.RUNNING, new PhaseId("active"));
      repo1.save(instance);

      EventRuntimeState state = new EventRuntimeState(id);
      state.setHealth(new Health(50, 100));
      state.setLockedUntilMillis(System.currentTimeMillis() + 50000);
      state.setPlatformLocation("world,100,64,100");
      assertTrue(state.tryClaim("player_123"));

      repo1.saveState(state);

      // Must wait for background queue to flush before we "crash"
      repo1.shutdown();
    }

    // Phase 2: "Restart" plugin and recover
    {
      SQLiteEventInstanceRepository repo2 = new SQLiteEventInstanceRepository(dbPath);
      repo2.initialize();

      Optional<EventInstance> recoveredOpt = repo2.findById(id);
      assertTrue(recoveredOpt.isPresent(), "Instance should be recovered");
      EventInstance recovered = recoveredOpt.get();

      assertEquals(EventLifecycleState.RUNNING, recovered.state());
      assertEquals(new PhaseId("active"), recovered.currentPhase().orElseThrow());

      Optional<EventRuntimeState> stateOpt = repo2.findState(id);
      assertTrue(stateOpt.isPresent(), "State should be recovered");
      EventRuntimeState recoveredState = stateOpt.get();

      assertTrue(recoveredState.health().isPresent());
      assertEquals(50, recoveredState.health().get().current());
      assertEquals(100, recoveredState.health().get().max());

      assertTrue(recoveredState.isLocked(), "Event should be locked");
      assertTrue(
          recoveredState.claimant().isPresent()
              && recoveredState.claimant().get().equals("player_123"));
      assertEquals("world,100,64,100", recoveredState.platformLocation().orElse(""));

      repo2.shutdown();
    }
  }
}
