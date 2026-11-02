package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventZone;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SQLiteEventInstanceRepositoryTest {

  @TempDir Path tempDir;

  private SQLiteEventInstanceRepository repository;

  @BeforeEach
  void setUp() {
    Path dbFile = tempDir.resolve("test-repo.db");
    repository = new SQLiteEventInstanceRepository(dbFile);
    repository.initialize();
  }

  @AfterEach
  void tearDown() {
    if (repository != null) {
      repository.shutdown();
    }
  }

  @Test
  void testSaveAndFindInstance() {
    EventInstanceId id = new EventInstanceId(UUID.randomUUID());
    EventDefinitionId defId = new EventDefinitionId("meteor");
    EventInstance instance =
        EventInstance.reconstitute(id, defId, EventLifecycleState.RUNNING, new PhaseId("fight"));

    repository.save(instance);

    var retrieved = repository.findById(id);
    assertTrue(retrieved.isPresent());
    assertEquals(id, retrieved.get().id());
    assertEquals(defId, retrieved.get().definitionId());
    assertEquals(EventLifecycleState.RUNNING, retrieved.get().state());

    var all = repository.findAll();
    assertFalse(all.isEmpty());
    assertTrue(all.stream().anyMatch(i -> i.id().equals(id)));
  }

  @Test
  void testSaveAndFindRuntimeState() {
    EventInstanceId id = new EventInstanceId(UUID.randomUUID());
    EventRuntimeState state = new EventRuntimeState(id);
    state.setHealth(new Health(100, 100));
    state.setHitCounter(new HitCounter(0, 10));
    state.setEventZone(new EventZone("world", 100.0, 200.0, 30.0));

    EventDefinitionId defId = new EventDefinitionId("meteor");
    EventInstance instance =
        EventInstance.reconstitute(id, defId, EventLifecycleState.RUNNING, new PhaseId("fight"));

    repository.saveWithStateDurably(instance, state);

    var retrieved = repository.findState(id);
    assertTrue(retrieved.isPresent());
    assertEquals(id, retrieved.get().instanceId());
    assertTrue(retrieved.get().eventZone().isPresent());

    // Asynchronous durability write
    repository.saveStateDurablyAsync(state).join();
    assertTrue(repository.findState(id).isPresent());

    // Asynchronous instance with state write
    EventInstanceId id2 = new EventInstanceId(UUID.randomUUID());
    EventInstance instance2 =
        EventInstance.reconstitute(id2, defId, EventLifecycleState.RUNNING, new PhaseId("fight"));
    EventRuntimeState state2 = new EventRuntimeState(id2);
    repository.saveWithStateDurablyAsync(instance2, state2).join();
    assertTrue(repository.findById(id2).isPresent());
    assertTrue(repository.findState(id2).isPresent());

    // Snapshot check
    assertNotNull(repository.snapshot(state2));
  }

  @Test
  void testRemoveInstance() {
    EventInstanceId id = new EventInstanceId(UUID.randomUUID());
    EventDefinitionId defId = new EventDefinitionId("meteor");
    EventInstance instance =
        EventInstance.reconstitute(id, defId, EventLifecycleState.COMPLETED, new PhaseId("done"));

    repository.save(instance);
    assertTrue(repository.findById(id).isPresent());

    assertTrue(repository.remove(id));
    assertFalse(repository.findById(id).isPresent());
  }

  @Test
  void testRestartLoadsAllStateAndLoot() {
    Path dbFile = tempDir.resolve("test-restart.db");
    SQLiteEventInstanceRepository repo1 = new SQLiteEventInstanceRepository(dbFile);
    repo1.initialize();

    EventInstanceId id = new EventInstanceId(UUID.randomUUID());
    EventDefinitionId defId = new EventDefinitionId("meteor");
    EventInstance instance =
        EventInstance.reconstitute(id, defId, EventLifecycleState.RUNNING, new PhaseId("fight"));
    EventRuntimeState state = new EventRuntimeState(id);
    state.setHealth(new Health(100, 100));
    state.setHitCounter(new HitCounter(0, 10));
    state.setEventZone(new EventZone("world", 100.0, 200.0, 30.0));
    state.tryClaim("worker-node-1");
    UUID playerId = UUID.randomUUID();
    state.recordParticipant(playerId);
    state.recordDamage(playerId, 25);
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 5)));

    repo1.saveWithStateDurably(instance, state);
    repo1.close();

    SQLiteEventInstanceRepository repo2 = new SQLiteEventInstanceRepository(dbFile);
    repo2.initialize();

    var reloadedInstance = repo2.findById(id);
    assertTrue(reloadedInstance.isPresent());
    assertEquals(EventLifecycleState.RUNNING, reloadedInstance.get().state());

    var reloadedState = repo2.findState(id);
    assertTrue(reloadedState.isPresent());
    assertEquals("worker-node-1", reloadedState.get().claimant());
    assertTrue(reloadedState.get().participants().contains(playerId));
    assertEquals(25, reloadedState.get().contribution().contributions().get(playerId));
    assertTrue(reloadedState.get().sharedLootInitialized());
    assertEquals("minecraft:diamond", reloadedState.get().sharedLootSnapshot().get(0).material());

    repo2.close();
  }

  @Test
  void testAccessorsAndClose() {
    assertNotNull(repository.databaseManager());
    assertNotNull(repository.rewardClaimRepository());

    SQLiteDatabaseManager db = repository.databaseManager();
    assertNotNull(db.dbPath());
    assertNotNull(db.executor());
    assertNotNull(db.writerConnection());

    db.rollbackQuietly();
    db.commitOrThrow();

    db.close();
  }
}
