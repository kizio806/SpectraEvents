package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecoveryStressTest {
  private Path dbPath;

  @BeforeEach
  void setUp() throws ClassNotFoundException, java.io.IOException {
    Class.forName("org.sqlite.JDBC");
    dbPath = Files.createTempFile("recovery_test", ".db");
  }

  @AfterEach
  void tearDown() throws java.io.IOException {
    Files.deleteIfExists(dbPath);
  }

  @Test
  void testStateAndInstanceRecovery() {
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
      state.setHitCounter(new HitCounter(6, 20));
      state.setLockedUntilMillis(System.currentTimeMillis() + 50000);
      state.setPlatformLocation("world,100,64,100");
      assertTrue(state.tryClaim("player_123"));
      UUID contributor = UUID.fromString("00000000-0000-0000-0000-000000000001");
      state.setMinimumContribution(100);
      state.recordDamage(contributor, 120);

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
      assertNotNull(recoveredState.hitCounter());
      assertEquals(6, recoveredState.hitCounter().current());
      assertEquals(20, recoveredState.hitCounter().maximum());

      assertTrue(recoveredState.isLocked(), "Event should be locked");
      assertTrue(
          recoveredState.claimant() != null && recoveredState.claimant().equals("player_123"));
      assertEquals("world,100,64,100", recoveredState.platformLocation().orElse(""));
      assertEquals(
          120,
          recoveredState
              .contribution()
              .contributions()
              .get(UUID.fromString("00000000-0000-0000-0000-000000000001")));
      assertTrue(
          recoveredState
              .contributionThresholdMillis()
              .containsKey(UUID.fromString("00000000-0000-0000-0000-000000000001")));

      repo2.shutdown();
    }
  }

  @Test
  void durableStartIsVisibleToAnotherConnectionBeforeShutdown() {
    EventInstanceId id = EventInstanceId.generate();
    EventDefinitionId definitionId = new EventDefinitionId("durable_start");
    SQLiteEventInstanceRepository writer = new SQLiteEventInstanceRepository(dbPath);
    writer.initialize();
    EventInstance instance =
        EventInstance.reconstitute(
            id, definitionId, EventLifecycleState.RUNNING, new PhaseId("active"));
    EventRuntimeState state = new EventRuntimeState(id);
    assertTrue(state.tryClaim("player"));

    writer.saveWithStateDurably(instance, state);

    SQLiteEventInstanceRepository reader = new SQLiteEventInstanceRepository(dbPath);
    reader.initialize();
    assertTrue(reader.findById(id).isPresent());
    assertEquals("player", reader.findState(id).orElseThrow().claimant());
    reader.shutdown();
    writer.shutdown();
  }

  @Test
  void preservesAnInitializedButEmptyPublicLootSnapshotAcrossRestart() {
    EventInstanceId id = EventInstanceId.generate();
    SQLiteEventInstanceRepository writer = new SQLiteEventInstanceRepository(dbPath);
    writer.initialize();
    EventRuntimeState state = new EventRuntimeState(id);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 1)));
    assertTrue(state.takeSharedLootSlot(0).isPresent());
    writer.saveStateDurably(state);
    writer.shutdown();

    SQLiteEventInstanceRepository reader = new SQLiteEventInstanceRepository(dbPath);
    reader.initialize();
    EventRuntimeState recovered = reader.findState(id).orElseThrow();
    assertTrue(recovered.sharedLootInitialized());
    assertTrue(recovered.sharedLootEmpty());
    reader.shutdown();
  }

  @Test
  void preservesInteractionParticipantsAcrossRestart() {
    EventInstanceId id = EventInstanceId.generate();
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    SQLiteEventInstanceRepository writer = new SQLiteEventInstanceRepository(dbPath);
    writer.initialize();
    EventRuntimeState state = new EventRuntimeState(id);
    state.recordParticipant(first);
    state.recordParticipant(second);
    writer.saveStateDurably(state);
    writer.shutdown();

    SQLiteEventInstanceRepository reader = new SQLiteEventInstanceRepository(dbPath);
    reader.initialize();
    assertEquals(
        java.util.Set.of(first, second), reader.findState(id).orElseThrow().participants());
    reader.shutdown();
  }
}
