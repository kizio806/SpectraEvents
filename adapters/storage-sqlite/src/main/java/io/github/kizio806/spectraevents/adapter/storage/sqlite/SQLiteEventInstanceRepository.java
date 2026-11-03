package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventZone;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.gameplay.contribution.DamageContribution;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of EventInstanceRepository with schema migration and write-through cache.
 * Fully platform-neutral, depending only on Java SQL APIs and SpectraEvents domain ports. Delegates
 * database lifecycle to {@link SQLiteDatabaseManager} and reward claims to {@link
 * SQLiteRewardClaimRepository}.
 */
public final class SQLiteEventInstanceRepository
    implements EventInstanceRepository, RewardClaimRepository {
  private static final Logger LOGGER =
      Logger.getLogger(SQLiteEventInstanceRepository.class.getName());

  private final SQLiteDatabaseManager database;
  private final SQLiteRewardClaimRepository rewardClaimRepository;
  private final Map<EventInstanceId, EventInstance> cache = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, EventRuntimeState> stateCache = new ConcurrentHashMap<>();

  public SQLiteEventInstanceRepository(Path dbPath) {
    this(new SQLiteDatabaseManager(dbPath));
  }

  public SQLiteEventInstanceRepository(SQLiteDatabaseManager database) {
    this.database = Objects.requireNonNull(database, "database");
    this.rewardClaimRepository = new SQLiteRewardClaimRepository(database);
  }

  public SQLiteDatabaseManager databaseManager() {
    return database;
  }

  public SQLiteRewardClaimRepository rewardClaimRepository() {
    return rewardClaimRepository;
  }

  public void initialize() {
    database.initialize();

    loadAllFromDb();
    loadAllStatesFromDb();
    loadAllContributionsFromDb();
    loadAllParticipantsFromDb();
    loadAllSharedLoot();

    LOGGER.fine(
        "SQLite storage initialized at "
            + database.dbPath()
            + " (Loaded "
            + cache.size()
            + " instances)");
  }

  public void shutdown() {
    database.shutdown();
  }

  @Override
  public void close() {
    shutdown();
  }

  private void loadAllFromDb() {
    String sql = "SELECT instance_id, definition_id, state, phase FROM spectra_instances";
    try (Connection conn = database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        try {
          EventInstanceId id = new EventInstanceId(UUID.fromString(rs.getString("instance_id")));
          EventDefinitionId defId = new EventDefinitionId(rs.getString("definition_id"));
          EventLifecycleState state = EventLifecycleState.valueOf(rs.getString("state"));
          String phaseStr = rs.getString("phase");
          PhaseId phaseId = phaseStr != null ? new PhaseId(phaseStr) : null;

          EventInstance restored = EventInstance.reconstitute(id, defId, state, phaseId);
          cache.put(id, restored);
        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
          LOGGER.warning("Failed to reconstitute instance row: " + e.getMessage());
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Failed to load instances from DB: " + e.getMessage(), e);
    }
  }

  private void loadAllStatesFromDb() {
    String sql =
        "SELECT instance_id, health_current, health_max, hit_count, hit_max, locked_until, claimant, platform_location, boss_entity_id, timer_deadline, encounter_deadline, zone_radius, minimum_contribution FROM spectra_instance_state";
    try (Connection conn = database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        try {
          EventInstanceId id = new EventInstanceId(UUID.fromString(rs.getString("instance_id")));
          EventRuntimeState state = new EventRuntimeState(id);

          int hCur = rs.getInt("health_current");
          if (!rs.wasNull()) {
            int hMax = rs.getInt("health_max");
            state.setHealth(new Health(hCur, hMax));
          }

          int hitCount = rs.getInt("hit_count");
          if (!rs.wasNull()) {
            int hitMaximum = rs.getInt("hit_max");
            state.setHitCounter(new HitCounter(hitCount, hitMaximum));
          }

          long lockedUntil = rs.getLong("locked_until");
          if (!rs.wasNull()) {
            state.setLockedUntilMillis(lockedUntil);
          }

          long timerDeadline = rs.getLong("timer_deadline");
          if (!rs.wasNull()) {
            state.setTimerDeadlineMillis(timerDeadline);
          }

          long encounterDeadline = rs.getLong("encounter_deadline");
          if (!rs.wasNull()) {
            state.setEncounterDeadlineMillis(encounterDeadline);
          }

          int minimumContribution = rs.getInt("minimum_contribution");
          if (!rs.wasNull()) {
            state.setMinimumContribution(minimumContribution);
          }

          String claimant = rs.getString("claimant");
          if (claimant != null) {
            state.tryClaim(claimant);
          }

          String platformLocation = rs.getString("platform_location");
          if (platformLocation != null) {
            state.setPlatformLocation(
                EventLocation.deserialize(platformLocation)
                    .<Object>map(location -> location)
                    .orElse(platformLocation));
          }

          double zoneRadius = rs.getDouble("zone_radius");
          if (!rs.wasNull()
              && state.platformLocation().filter(EventLocation.class::isInstance).isPresent()) {
            EventLocation location = (EventLocation) state.platformLocation().orElseThrow();
            state.setEventZone(EventZone.at(location, zoneRadius));
          }

          String bossEntityId = rs.getString("boss_entity_id");
          if (bossEntityId != null) {
            state.setBossEntityId(bossEntityId);
          }

          stateCache.put(id, state);
        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
          LOGGER.warning("Failed to reconstitute state row: " + e.getMessage());
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Failed to load states from DB: " + e.getMessage(), e);
    }
  }

  private void loadAllContributionsFromDb() {
    String sql =
        "SELECT instance_id, player_uuid, damage, threshold_attained FROM spectra_contributions";
    Map<EventInstanceId, Map<UUID, Integer>> damage = new HashMap<>();
    Map<EventInstanceId, Map<UUID, Long>> thresholds = new HashMap<>();
    try (Connection conn = database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        EventInstanceId id = new EventInstanceId(UUID.fromString(rs.getString("instance_id")));
        UUID player = UUID.fromString(rs.getString("player_uuid"));
        damage.computeIfAbsent(id, ignored -> new HashMap<>()).put(player, rs.getInt("damage"));
        long attained = rs.getLong("threshold_attained");
        if (!rs.wasNull()) {
          thresholds.computeIfAbsent(id, ignored -> new HashMap<>()).put(player, attained);
        }
      }
      damage.forEach(
          (id, values) -> {
            EventRuntimeState state = stateCache.get(id);
            if (state != null) {
              state.setContribution(new DamageContribution(values));
              state.setContributionThresholdMillis(thresholds.getOrDefault(id, Map.of()));
            }
          });
    } catch (SQLException | IllegalArgumentException exception) {
      LOGGER.log(
          Level.WARNING,
          "Failed to load contribution checkpoints: " + exception.getMessage(),
          exception);
    }
  }

  private void loadAllSharedLoot() {
    String sql = "SELECT instance_id, slot, material, amount FROM spectra_shared_loot";
    Map<EventInstanceId, Map<Integer, CoreActions.LootStack>> all = new HashMap<>();
    try (Connection conn = database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        EventInstanceId id = new EventInstanceId(UUID.fromString(rs.getString("instance_id")));
        Map<Integer, CoreActions.LootStack> slots =
            all.computeIfAbsent(id, ignored -> new HashMap<>());
        if (rs.getInt("slot") >= 0) {
          slots.put(
              rs.getInt("slot"),
              new CoreActions.LootStack(rs.getString("material"), rs.getInt("amount")));
        }
      }
      all.forEach(
          (id, slots) ->
              Optional.ofNullable(stateCache.get(id))
                  .ifPresent(state -> state.restoreSharedLoot(slots)));
    } catch (SQLException | IllegalArgumentException exception) {
      LOGGER.log(
          Level.WARNING, "Failed to restore shared loot: " + exception.getMessage(), exception);
    }
  }

  private void loadAllParticipantsFromDb() {
    String sql = "SELECT instance_id, player_uuid FROM spectra_participants";
    Map<EventInstanceId, java.util.Set<UUID>> participants = new HashMap<>();
    try (Connection conn = database.getConnection();
        PreparedStatement statement = conn.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        EventInstanceId instanceId =
            new EventInstanceId(UUID.fromString(resultSet.getString("instance_id")));
        participants
            .computeIfAbsent(instanceId, ignored -> new java.util.HashSet<>())
            .add(UUID.fromString(resultSet.getString("player_uuid")));
      }
      participants.forEach(
          (instanceId, playerIds) -> {
            EventRuntimeState state = stateCache.get(instanceId);
            if (state != null) {
              state.setParticipants(playerIds);
            }
          });
    } catch (SQLException | IllegalArgumentException exception) {
      LOGGER.log(Level.WARNING, "Failed to restore event participants", exception);
    }
  }

  @Override
  public void save(EventInstance eventInstance) {
    cache.put(eventInstance.id(), eventInstance);
    database.executor().enqueue(() -> persistInstance(eventInstance, true));
  }

  @Override
  public void saveState(EventRuntimeState state) {
    stateCache.put(state.instanceId(), state);
    StateSnapshot snapshot = snapshot(state);
    database.executor().enqueue(() -> persistState(snapshot, true));
  }

  @Override
  public void saveStateDurably(EventRuntimeState state) {
    stateCache.put(state.instanceId(), state);
    StateSnapshot snapshot = snapshot(state);
    database.executor().executeAndWait(() -> persistState(snapshot, true));
  }

  @Override
  public CompletableFuture<Void> saveStateDurablyAsync(EventRuntimeState state) {
    stateCache.put(state.instanceId(), state);
    StateSnapshot snapshot = snapshot(state);
    return database.executor().executeAsync(() -> persistState(snapshot, true));
  }

  @Override
  public void saveWithStateDurably(EventInstance eventInstance, EventRuntimeState state) {
    cache.put(eventInstance.id(), eventInstance);
    stateCache.put(state.instanceId(), state);
    StateSnapshot snapshot = snapshot(state);
    database
        .executor()
        .executeAndWait(
            () -> {
              try {
                persistInstance(eventInstance, false);
                persistState(snapshot, false);
                database.commitOrThrow();
              } catch (IllegalStateException exception) {
                database.rollbackQuietly();
                throw exception;
              }
            });
  }

  @Override
  public CompletableFuture<Void> saveWithStateDurablyAsync(
      EventInstance eventInstance, EventRuntimeState state) {
    cache.put(eventInstance.id(), eventInstance);
    stateCache.put(state.instanceId(), state);
    StateSnapshot snapshot = snapshot(state);
    return database
        .executor()
        .executeAsync(
            () -> {
              try {
                persistInstance(eventInstance, false);
                persistState(snapshot, false);
                database.commitOrThrow();
              } catch (IllegalStateException exception) {
                database.rollbackQuietly();
                throw exception;
              }
            });
  }

  private void persistInstance(EventInstance eventInstance, boolean commit) {
    String sql =
        """
        INSERT INTO spectra_instances (instance_id, definition_id, state, phase, created_at, last_updated)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(instance_id) DO UPDATE SET
          state = excluded.state,
          phase = excluded.phase,
          last_updated = excluded.last_updated;
        """;
    long now = System.currentTimeMillis();
    Connection writerConnection = database.writerConnection();
    try (PreparedStatement stmt = writerConnection.prepareStatement(sql)) {
      stmt.setString(1, eventInstance.id().toString());
      stmt.setString(2, eventInstance.definitionId().value());
      stmt.setString(3, eventInstance.state().name());
      stmt.setString(4, eventInstance.currentPhase().map(PhaseId::value).orElse(null));
      stmt.setLong(5, now);
      stmt.setLong(6, now);
      stmt.executeUpdate();
      if (commit) {
        database.commitOrThrow();
      }
    } catch (SQLException exception) {
      database.rollbackQuietly();
      throw new IllegalStateException("Failed to save event instance", exception);
    }
  }

  private void persistState(StateSnapshot state, boolean commit) {
    String sql =
        """
        INSERT INTO spectra_instance_state (instance_id, health_current, health_max, hit_count, hit_max, locked_until, claimant, platform_location, boss_entity_id, timer_deadline, encounter_deadline, zone_radius, minimum_contribution, last_updated)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(instance_id) DO UPDATE SET
          health_current = excluded.health_current,
          health_max = excluded.health_max,
          hit_count = excluded.hit_count,
          hit_max = excluded.hit_max,
          locked_until = excluded.locked_until,
          claimant = excluded.claimant,
          platform_location = excluded.platform_location,
          boss_entity_id = excluded.boss_entity_id,
          timer_deadline = excluded.timer_deadline,
          encounter_deadline = excluded.encounter_deadline,
          zone_radius = excluded.zone_radius,
          minimum_contribution = excluded.minimum_contribution,
          last_updated = excluded.last_updated;
        """;
    Connection writerConnection = database.writerConnection();
    try (PreparedStatement stmt = writerConnection.prepareStatement(sql)) {
      stmt.setString(1, state.instanceId());
      setNullableInteger(stmt, 2, state.healthCurrent());
      setNullableInteger(stmt, 3, state.healthMaximum());
      setNullableInteger(stmt, 4, state.hitCount());
      setNullableInteger(stmt, 5, state.hitMaximum());
      setNullableLong(stmt, 6, state.lockedUntil());
      stmt.setString(7, state.claimant());
      stmt.setString(8, state.platformLocation());
      stmt.setString(9, state.bossEntityId());
      setNullableLong(stmt, 10, state.timerDeadline());
      setNullableLong(stmt, 11, state.encounterDeadline());
      setNullableDouble(stmt, 12, state.zoneRadius());
      setNullableInteger(stmt, 13, state.minimumContribution());
      stmt.setLong(14, state.updatedAt());
      stmt.executeUpdate();
      persistContributions(state);
      persistParticipants(state);
      persistSharedLoot(state);
      if (commit) {
        database.commitOrThrow();
      }
    } catch (SQLException exception) {
      database.rollbackQuietly();
      throw new IllegalStateException("Failed to save event runtime state", exception);
    }
  }

  StateSnapshot snapshot(EventRuntimeState state) {
    return new StateSnapshot(
        state.instanceId().toString(),
        state.health().map(Health::current).orElse(null),
        state.health().map(Health::max).orElse(null),
        state.hitCounter() != null ? state.hitCounter().current() : null,
        state.hitCounter() != null ? state.hitCounter().maximum() : null,
        state.lockedUntilMillis() > 0 ? state.lockedUntilMillis() : null,
        state.claimant(),
        state
            .platformLocation()
            .map(
                location ->
                    location instanceof EventLocation eventLocation
                        ? eventLocation.serialize()
                        : location.toString())
            .orElse(null),
        state.bossEntityId().map(Object::toString).orElse(null),
        state.timerDeadlineMillis() > 0 ? state.timerDeadlineMillis() : null,
        state.encounterDeadlineMillis() > 0 ? state.encounterDeadlineMillis() : null,
        state.eventZone().map(EventZone::radius).orElse(null),
        state.minimumContribution(),
        state.contribution().contributions(),
        state.contributionThresholdMillis(),
        state.participants(),
        state.sharedLootSnapshot(),
        state.sharedLootInitialized(),
        System.currentTimeMillis());
  }

  private void persistContributions(StateSnapshot state) {
    String delete = "DELETE FROM spectra_contributions WHERE instance_id = ?";
    String insert =
        "INSERT INTO spectra_contributions (instance_id, player_uuid, damage, threshold_attained) VALUES (?, ?, ?, ?)";
    Connection writerConnection = database.writerConnection();
    try (PreparedStatement deleteStatement = writerConnection.prepareStatement(delete);
        PreparedStatement insertStatement = writerConnection.prepareStatement(insert)) {
      deleteStatement.setString(1, state.instanceId());
      deleteStatement.executeUpdate();
      for (Map.Entry<UUID, Integer> entry : state.contributions().entrySet()) {
        insertStatement.setString(1, state.instanceId());
        insertStatement.setString(2, entry.getKey().toString());
        insertStatement.setInt(3, entry.getValue());
        setNullableLong(insertStatement, 4, state.thresholdAttained().get(entry.getKey()));
        insertStatement.addBatch();
      }
      insertStatement.executeBatch();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to save contribution checkpoint", exception);
    }
  }

  private void persistSharedLoot(StateSnapshot state) {
    String delete = "DELETE FROM spectra_shared_loot WHERE instance_id = ?";
    String insert =
        "INSERT INTO spectra_shared_loot (instance_id, slot, material, amount) VALUES (?, ?, ?, ?)";
    Connection writerConnection = database.writerConnection();
    try (PreparedStatement deleteStatement = writerConnection.prepareStatement(delete);
        PreparedStatement insertStatement = writerConnection.prepareStatement(insert)) {
      deleteStatement.setString(1, state.instanceId());
      deleteStatement.executeUpdate();
      if (state.sharedLootInitialized()) {
        insertStatement.setString(1, state.instanceId());
        insertStatement.setInt(2, -1);
        insertStatement.setString(3, "__initialized__");
        insertStatement.setInt(4, 0);
        insertStatement.addBatch();
      }
      for (Map.Entry<Integer, CoreActions.LootStack> entry : state.sharedLoot().entrySet()) {
        insertStatement.setString(1, state.instanceId());
        insertStatement.setInt(2, entry.getKey());
        insertStatement.setString(3, entry.getValue().material());
        insertStatement.setInt(4, entry.getValue().amount());
        insertStatement.addBatch();
      }
      insertStatement.executeBatch();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to save shared loot snapshot", exception);
    }
  }

  private void persistParticipants(StateSnapshot state) {
    String delete = "DELETE FROM spectra_participants WHERE instance_id = ?";
    String insert = "INSERT INTO spectra_participants (instance_id, player_uuid) VALUES (?, ?)";
    Connection writerConnection = database.writerConnection();
    try (PreparedStatement deleteStatement = writerConnection.prepareStatement(delete);
        PreparedStatement insertStatement = writerConnection.prepareStatement(insert)) {
      deleteStatement.setString(1, state.instanceId());
      deleteStatement.executeUpdate();
      for (UUID participant : state.participants()) {
        insertStatement.setString(1, state.instanceId());
        insertStatement.setString(2, participant.toString());
        insertStatement.addBatch();
      }
      insertStatement.executeBatch();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to save event participants", exception);
    }
  }

  private void setNullableInteger(PreparedStatement statement, int index, Integer value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, java.sql.Types.INTEGER);
    } else {
      statement.setInt(index, value);
    }
  }

  private void setNullableLong(PreparedStatement statement, int index, Long value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, java.sql.Types.BIGINT);
    } else {
      statement.setLong(index, value);
    }
  }

  private void setNullableDouble(PreparedStatement statement, int index, Double value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, java.sql.Types.DOUBLE);
    } else {
      statement.setDouble(index, value);
    }
  }

  private record StateSnapshot(
      String instanceId,
      Integer healthCurrent,
      Integer healthMaximum,
      Integer hitCount,
      Integer hitMaximum,
      Long lockedUntil,
      String claimant,
      String platformLocation,
      String bossEntityId,
      Long timerDeadline,
      Long encounterDeadline,
      Double zoneRadius,
      Integer minimumContribution,
      Map<UUID, Integer> contributions,
      Map<UUID, Long> thresholdAttained,
      java.util.Set<UUID> participants,
      Map<Integer, CoreActions.LootStack> sharedLoot,
      boolean sharedLootInitialized,
      long updatedAt) {}

  @Override
  public Optional<EventRuntimeState> findState(EventInstanceId eventInstanceId) {
    return Optional.ofNullable(stateCache.get(eventInstanceId));
  }

  @Override
  public Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
    return Optional.ofNullable(cache.get(eventInstanceId));
  }

  @Override
  public List<EventInstance> findAll() {
    return List.copyOf(cache.values());
  }

  // --- RewardClaimRepository Delegation ---

  @Override
  public void savePendingDurably(RewardClaim claim) {
    rewardClaimRepository.savePendingDurably(claim);
  }

  @Override
  public CompletableFuture<Void> savePendingDurablyAsync(RewardClaim claim) {
    return rewardClaimRepository.savePendingDurablyAsync(claim);
  }

  @Override
  public List<RewardClaim> findPending(UUID playerId) {
    return rewardClaimRepository.findPending(playerId);
  }

  @Override
  public CompletableFuture<List<RewardClaim>> findPendingAsync(UUID playerId) {
    return rewardClaimRepository.findPendingAsync(playerId);
  }

  @Override
  public CompletableFuture<List<RewardClaim>> findByStatusAsync(RewardClaimStatus status) {
    return rewardClaimRepository.findByStatusAsync(status);
  }

  @Override
  public boolean beginDelivery(UUID claimId) {
    return rewardClaimRepository.beginDelivery(claimId);
  }

  @Override
  public CompletableFuture<Boolean> beginDeliveryAsync(UUID claimId) {
    return rewardClaimRepository.beginDeliveryAsync(claimId);
  }

  @Override
  public boolean returnToPending(UUID claimId) {
    return rewardClaimRepository.returnToPending(claimId);
  }

  @Override
  public CompletableFuture<Boolean> returnToPendingAsync(UUID claimId) {
    return rewardClaimRepository.returnToPendingAsync(claimId);
  }

  @Override
  public boolean markDelivered(UUID claimId) {
    return rewardClaimRepository.markDelivered(claimId);
  }

  @Override
  public CompletableFuture<Boolean> markDeliveredAsync(UUID claimId) {
    return rewardClaimRepository.markDeliveredAsync(claimId);
  }

  @Override
  public boolean remove(EventInstanceId eventInstanceId) {
    boolean removed = cache.remove(eventInstanceId) != null;
    stateCache.remove(eventInstanceId);

    if (removed) {
      String sql1 = "DELETE FROM spectra_instances WHERE instance_id = ?";
      String sql2 = "DELETE FROM spectra_instance_state WHERE instance_id = ?";
      String sql3 = "DELETE FROM spectra_shared_loot WHERE instance_id = ?";
      String idStr = eventInstanceId.toString();

      database
          .executor()
          .enqueue(
              () -> {
                Connection writerConnection = database.writerConnection();
                try (PreparedStatement stmt1 = writerConnection.prepareStatement(sql1);
                    PreparedStatement stmt2 = writerConnection.prepareStatement(sql2);
                    PreparedStatement stmt3 = writerConnection.prepareStatement(sql3)) {
                  stmt1.setString(1, idStr);
                  stmt1.executeUpdate();
                  stmt2.setString(1, idStr);
                  stmt2.executeUpdate();
                  stmt3.setString(1, idStr);
                  stmt3.executeUpdate();
                  database.commitOrThrow();
                } catch (SQLException e) {
                  database.rollbackQuietly();
                  LOGGER.log(
                      Level.SEVERE, "Failed to remove instance from DB: " + e.getMessage(), e);
                }
              });
    }
    return removed;
  }
}
