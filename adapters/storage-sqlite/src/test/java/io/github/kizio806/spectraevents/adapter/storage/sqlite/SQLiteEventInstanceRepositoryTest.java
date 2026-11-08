package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
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
    EventLocation loc = new EventLocation("world", 100.0, 200.0, 30.0, 0.0f, 0.0f);
    state.setPlatformLocation(loc);
    state.setEventZone(EventZone.at(loc, 15.0));
    state.tryClaim("worker-node-1");
    state.setTimerDeadlineMillis(System.currentTimeMillis() + 60000);
    state.setEncounterDeadlineMillis(System.currentTimeMillis() + 120000);
    state.setMinimumContribution(50);
    state.setBossEntityId("boss-uuid-123");
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
    assertEquals("boss-uuid-123", reloadedState.get().bossEntityId().orElse(null));
    assertEquals(50, reloadedState.get().minimumContribution());
    assertTrue(reloadedState.get().timerDeadlineMillis() > 0);
    assertTrue(reloadedState.get().encounterDeadlineMillis() > 0);
    assertTrue(reloadedState.get().platformLocation().isPresent());
    assertTrue(reloadedState.get().eventZone().isPresent());
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

    try (SQLiteDatabaseManager db = repository.databaseManager()) {
      assertNotNull(db.dbPath());
      assertNotNull(db.executor());
      assertNotNull(db.writerConnection());

      db.rollbackQuietly();
      db.commitOrThrow();
    }
  }

  @Test
  void testRemoveNonExistentReturnsFalse() {
    assertFalse(repository.remove(new EventInstanceId(UUID.randomUUID())));
  }

  @Test
  void testReloadWithCorruptAndUnmatchedRows() throws Exception {
    Path dbFile = tempDir.resolve("test-corrupt-rows.db");
    SQLiteEventInstanceRepository repo = new SQLiteEventInstanceRepository(dbFile);
    repo.initialize();
    SQLiteDatabaseManager db = repo.databaseManager();

    // Insert corrupt instance row (invalid UUID and invalid enum)
    try (Connection conn = db.getConnection();
        Statement stmt = conn.createStatement();
        PreparedStatement psContrib =
            conn.prepareStatement(
                "INSERT INTO spectra_contributions (instance_id, player_uuid, damage, threshold_attained) VALUES (?, ?, ?, ?)");
        PreparedStatement psPart =
            conn.prepareStatement(
                "INSERT INTO spectra_participants (instance_id, player_uuid) VALUES (?, ?)")) {
      stmt.execute(
          "INSERT INTO spectra_instances (instance_id, definition_id, state, phase, created_at, last_updated) "
              + "VALUES ('not-a-uuid', 'def', 'NOT_A_STATE', null, 0, 0)");

      // Insert corrupt state row (invalid instance UUID)
      stmt.execute(
          "INSERT INTO spectra_instance_state (instance_id, health_current, health_max, hit_count, hit_max, locked_until, claimant, platform_location, boss_entity_id, timer_deadline, encounter_deadline, zone_radius, minimum_contribution, last_updated) "
              + "VALUES ('not-a-uuid', null, null, null, null, null, null, null, null, null, null, null, null, 0)");

      // Insert unmatched contribution row (random instance ID)
      psContrib.setString(1, UUID.randomUUID().toString());
      psContrib.setString(2, UUID.randomUUID().toString());
      psContrib.setInt(3, 10);
      psContrib.setLong(4, 1000L);
      psContrib.executeUpdate();

      // Insert unmatched participant row (random instance ID)
      psPart.setString(1, UUID.randomUUID().toString());
      psPart.setString(2, UUID.randomUUID().toString());
      psPart.executeUpdate();
    }
    repo.close();

    // Reopen to verify reload skips corrupt rows safely without throwing
    SQLiteEventInstanceRepository repoReloaded = new SQLiteEventInstanceRepository(dbFile);
    repoReloaded.initialize();
    assertTrue(repoReloaded.findAll().isEmpty());
    repoReloaded.close();
  }

  @Test
  void testNullArgumentValidations() {
    assertThrows(NullPointerException.class, () -> repository.save(null));
    assertThrows(NullPointerException.class, () -> repository.remove(null));
    assertThrows(
        NullPointerException.class,
        () -> {
          var res = repository.findById(null);
          assertNotNull(res);
        });
    assertThrows(
        NullPointerException.class,
        () -> {
          var res = repository.findState(null);
          assertNotNull(res);
        });
    assertThrows(NullPointerException.class, () -> repository.saveStateDurably(null));
    assertThrows(NullPointerException.class, () -> repository.saveStateDurablyAsync(null));
    assertThrows(NullPointerException.class, () -> repository.snapshot(null));

    EventInstance instance =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("meteor"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    EventRuntimeState state = new EventRuntimeState(instance.id());

    assertThrows(NullPointerException.class, () -> repository.saveWithStateDurably(null, state));
    assertThrows(NullPointerException.class, () -> repository.saveWithStateDurably(instance, null));
    assertThrows(
        NullPointerException.class, () -> repository.saveWithStateDurablyAsync(null, state));
    assertThrows(
        NullPointerException.class, () -> repository.saveWithStateDurablyAsync(instance, null));
  }

  @Test
  void testOperationsFailWithIllegalStateExceptionWhenTablesDropped() throws Exception {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            id, new EventDefinitionId("meteor"), EventLifecycleState.RUNNING, new PhaseId("fight"));
    EventRuntimeState state = new EventRuntimeState(id);

    // Drop instances tables to trigger SQLExceptions
    try (Connection conn = repository.databaseManager().getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("DROP TABLE spectra_contributions;");
      stmt.execute("DROP TABLE spectra_participants;");
      stmt.execute("DROP TABLE spectra_instance_state;");
      stmt.execute("DROP TABLE spectra_instances;");
    }

    assertThrows(IllegalStateException.class, () -> repository.saveStateDurably(state));
    assertThrows(
        IllegalStateException.class, () -> repository.saveWithStateDurably(instance, state));

    CompletionException ex1 =
        assertThrows(
            CompletionException.class, () -> repository.saveStateDurablyAsync(state).join());
    assertTrue(ex1.getCause() instanceof IllegalStateException);

    CompletionException ex2 =
        assertThrows(
            CompletionException.class,
            () -> repository.saveWithStateDurablyAsync(instance, state).join());
    assertTrue(ex2.getCause() instanceof IllegalStateException);
  }

  @Test
  void testSaveAndFindInstanceWithPhaseNull() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            id, new EventDefinitionId("meteor"), EventLifecycleState.CREATED, null);

    repository.save(instance);
    var found = repository.findById(id);
    assertTrue(found.isPresent());
    assertTrue(found.get().currentPhase().isEmpty());
  }
}
