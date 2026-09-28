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
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of EventInstanceRepository with schema migration and write-through cache.
 * Fully platform-neutral, depending only on Java SQL APIs and SpectraEvents domain ports.
 * Implements a Single-Writer Persistent Connection design to maximize concurrency.
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class SQLiteEventInstanceRepository
    implements EventInstanceRepository, RewardClaimRepository {
  private static final Logger LOGGER =
      Logger.getLogger(SQLiteEventInstanceRepository.class.getName());

  private final Path dbPath;
  private final Map<EventInstanceId, EventInstance> cache = new ConcurrentHashMap<>();
  private final Map<EventInstanceId, EventRuntimeState> stateCache = new ConcurrentHashMap<>();
  private final String jdbcUrl;

  private Connection writerConnection;
  private SingleWriterPersistenceExecutor executor;

  public SQLiteEventInstanceRepository(Path dbPath) {
    this.dbPath = dbPath;
    this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    Path parent = dbPath.getParent();
    if (parent != null) {
      try {
        java.nio.file.Files.createDirectories(parent);
      } catch (java.io.IOException e) {
        throw new java.io.UncheckedIOException(e);
      }
    }
  }

  public void initialize() {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException ignored) {
      // Driver auto-registers on modern JVMs
    }

    try (Connection conn = getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("PRAGMA journal_mode=WAL;");
        stmt.execute("PRAGMA synchronous=NORMAL;");
        stmt.execute("PRAGMA busy_timeout=5000;");
      }
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Failed to apply PRAGMAs: " + e.getMessage());
    }

    try {
      writerConnection = getConnection();
      writerConnection.setAutoCommit(false); // Using explicit transactions

      try (Statement stmt = writerConnection.createStatement()) {
        stmt.execute("PRAGMA busy_timeout=5000;"); // Set on writer too
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS spectra_schema_version (version INTEGER PRIMARY KEY);");
        stmt.execute(
            """
            CREATE TABLE IF NOT EXISTS spectra_instances (
              instance_id TEXT PRIMARY KEY,
              definition_id TEXT NOT NULL,
              state TEXT NOT NULL,
              phase TEXT,
              created_at INTEGER NOT NULL,
              last_updated INTEGER NOT NULL
            );
            """);

        int currentVer = 0;
        try (ResultSet rs = stmt.executeQuery("SELECT MAX(version) FROM spectra_schema_version")) {
          if (rs.next()) {
            currentVer = rs.getInt(1);
          }
        }

        final int v2 = 2;
        if (currentVer < v2) {
          stmt.execute(
              """
              CREATE TABLE IF NOT EXISTS spectra_instance_state (
                instance_id TEXT PRIMARY KEY,
                health_current INTEGER,
                health_max INTEGER,
                locked_until INTEGER,
                claimant TEXT,
                platform_location TEXT,
                boss_entity_id TEXT,
                last_updated INTEGER NOT NULL
              );
              """);
          stmt.executeUpdate("INSERT OR REPLACE INTO spectra_schema_version (version) VALUES (2);");
        }

        final int v3 = 3;
        if (currentVer < v3) {
          stmt.execute("ALTER TABLE spectra_instance_state ADD COLUMN timer_deadline INTEGER;");
          stmt.executeUpdate("INSERT OR REPLACE INTO spectra_schema_version (version) VALUES (3);");
        }

        final int v4 = 4;
        if (currentVer < v4) {
          stmt.execute(
              """
              CREATE TABLE IF NOT EXISTS spectra_active_animations (
                playback_id TEXT PRIMARY KEY,
                model_runtime_id TEXT NOT NULL,
                model_definition_id TEXT NOT NULL,
                animation_id TEXT NOT NULL,
                state TEXT NOT NULL,
                current_time_nanos INTEGER NOT NULL,
                speed REAL NOT NULL,
                loop_mode TEXT NOT NULL,
                current_loop INTEGER NOT NULL,
                recovery_policy TEXT NOT NULL,
                last_updated INTEGER NOT NULL
              );
              """);
          stmt.executeUpdate("INSERT OR REPLACE INTO spectra_schema_version (version) VALUES (4);");
        }

        final int v5 = 5;
        if (currentVer < v5) {
          stmt.execute("ALTER TABLE spectra_instance_state ADD COLUMN hit_count INTEGER;");
          stmt.execute("ALTER TABLE spectra_instance_state ADD COLUMN hit_max INTEGER;");
          stmt.executeUpdate("INSERT OR REPLACE INTO spectra_schema_version (version) VALUES (5);");
        }

        final int v6 = 6;
        if (currentVer < v6) {
          stmt.execute("ALTER TABLE spectra_instance_state ADD COLUMN encounter_deadline INTEGER;");
          stmt.execute("ALTER TABLE spectra_instance_state ADD COLUMN zone_radius REAL;");
          stmt.execute(
              "ALTER TABLE spectra_instance_state ADD COLUMN minimum_contribution INTEGER;");
          stmt.execute(
              """
              CREATE TABLE IF NOT EXISTS spectra_contributions (
                instance_id TEXT NOT NULL,
                player_uuid TEXT NOT NULL,
                damage INTEGER NOT NULL,
                threshold_attained INTEGER,
                PRIMARY KEY (instance_id, player_uuid)
              );
              """);
          stmt.executeUpdate("INSERT OR REPLACE INTO spectra_schema_version (version) VALUES (6);");
        }
        final int v7 = 7;
        if (currentVer < v7) {
          stmt.execute(
              "CREATE TABLE IF NOT EXISTS spectra_shared_loot (instance_id TEXT NOT NULL, slot INTEGER NOT NULL, material TEXT NOT NULL, amount INTEGER NOT NULL, PRIMARY KEY (instance_id, slot));");
          stmt.executeUpdate("INSERT OR REPLACE INTO spectra_schema_version (version) VALUES (7);");
        }
        final int v8 = 8;
        if (currentVer < v8) {
          stmt.execute(
              """
              CREATE TABLE IF NOT EXISTS spectra_reward_claims (
                claim_id TEXT PRIMARY KEY,
                instance_id TEXT NOT NULL,
                player_uuid TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                delivered_at INTEGER
              );
              """);
          stmt.execute(
              """
              CREATE TABLE IF NOT EXISTS spectra_reward_items (
                claim_id TEXT NOT NULL,
                slot INTEGER NOT NULL,
                material TEXT NOT NULL,
                amount INTEGER NOT NULL,
                PRIMARY KEY (claim_id, slot),
                FOREIGN KEY (claim_id) REFERENCES spectra_reward_claims(claim_id) ON DELETE CASCADE
              );
              """);
          stmt.execute(
              "CREATE INDEX IF NOT EXISTS spectra_reward_claims_player_status ON spectra_reward_claims(player_uuid, status, created_at);");
          stmt.executeUpdate("INSERT OR REPLACE INTO spectra_schema_version (version) VALUES (8);");
        }
        final int v9 = 9;
        if (currentVer < v9) {
          stmt.execute(
              "CREATE TABLE IF NOT EXISTS spectra_participants (instance_id TEXT NOT NULL, player_uuid TEXT NOT NULL, PRIMARY KEY (instance_id, player_uuid));");
          stmt.executeUpdate("INSERT OR REPLACE INTO spectra_schema_version (version) VALUES (9);");
        }
        writerConnection.commit();
      } catch (SQLException e) {
        writerConnection.rollback();
        throw e;
      }

      loadAllFromDb();
      loadAllStatesFromDb();
      loadAllContributionsFromDb();
      loadAllParticipantsFromDb();
      loadAllSharedLoot();

      executor = new SingleWriterPersistenceExecutor(10000);
      executor.start();

      LOGGER.fine(
          "SQLite storage initialized at " + dbPath + " (Loaded " + cache.size() + " instances)");
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize SQLite storage: " + e.getMessage(), e);
      shutdown();
      throw new IllegalStateException("Failed to initialize SQLite storage", e);
    }
  }

  public void shutdown() {
    if (executor != null) {
      try {
        executor.shutdown();
      } catch (RuntimeException exception) {
        LOGGER.log(
            Level.SEVERE, "Persistence writes did not drain; keeping SQLite open", exception);
        return;
      }
    }
    if (writerConnection != null) {
      try {
        writerConnection.close();
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Failed to close persistent connection", e);
      }
    }
  }

  @Override
  public void close() {
    shutdown();
  }

  private Connection getConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl);
  }

  private void loadAllFromDb() {
    String sql = "SELECT instance_id, definition_id, state, phase FROM spectra_instances";
    try (Connection conn = getConnection();
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
        } catch (Exception e) {
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
    try (Connection conn = getConnection();
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
        } catch (Exception e) {
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
    try (Connection conn = getConnection();
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
    try (Connection conn = getConnection();
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
    try (Connection conn = getConnection();
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
    executor.enqueue(() -> persistInstance(eventInstance, true));
  }

  @Override
  public void saveState(EventRuntimeState state) {
    stateCache.put(state.instanceId(), state);
    StateSnapshot snapshot = snapshot(state);
    executor.enqueue(() -> persistState(snapshot, true));
  }

  @Override
  public void saveStateDurably(EventRuntimeState state) {
    stateCache.put(state.instanceId(), state);
    StateSnapshot snapshot = snapshot(state);
    executor.executeAndWait(() -> persistState(snapshot, true));
  }

  @Override
  public void saveWithStateDurably(EventInstance eventInstance, EventRuntimeState state) {
    cache.put(eventInstance.id(), eventInstance);
    stateCache.put(state.instanceId(), state);
    StateSnapshot snapshot = snapshot(state);
    executor.executeAndWait(
        () -> {
          try {
            persistInstance(eventInstance, false);
            persistState(snapshot, false);
            commitOrThrow();
          } catch (RuntimeException exception) {
            rollbackQuietly();
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
    try (PreparedStatement stmt = writerConnection.prepareStatement(sql)) {
      stmt.setString(1, eventInstance.id().toString());
      stmt.setString(2, eventInstance.definitionId().value());
      stmt.setString(3, eventInstance.state().name());
      stmt.setString(4, eventInstance.currentPhase().map(PhaseId::value).orElse(null));
      stmt.setLong(5, now);
      stmt.setLong(6, now);
      stmt.executeUpdate();
      if (commit) {
        writerConnection.commit();
      }
    } catch (SQLException exception) {
      rollbackQuietly();
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
        writerConnection.commit();
      }
    } catch (SQLException exception) {
      rollbackQuietly();
      throw new IllegalStateException("Failed to save event runtime state", exception);
    }
  }

  private StateSnapshot snapshot(EventRuntimeState state) {
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

  private void rollbackQuietly() {
    try {
      writerConnection.rollback();
    } catch (SQLException ignored) {
      // Preserve the original persistence failure.
    }
  }

  private void commitOrThrow() {
    try {
      writerConnection.commit();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to commit SQLite transaction", exception);
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

  @Override
  public void savePendingDurably(RewardClaim claim) {
    Objects.requireNonNull(claim, "claim");
    if (claim.status() != RewardClaimStatus.PENDING) {
      throw new IllegalArgumentException("Only pending reward claims may be created");
    }
    executor.executeAndWait(
        () -> {
          String claimSql =
              "INSERT OR IGNORE INTO spectra_reward_claims (claim_id, instance_id, player_uuid, status, created_at, delivered_at) VALUES (?, ?, ?, ?, ?, ?)";
          String itemSql =
              "INSERT INTO spectra_reward_items (claim_id, slot, material, amount) VALUES (?, ?, ?, ?)";
          try (PreparedStatement claimStatement = writerConnection.prepareStatement(claimSql);
              PreparedStatement itemStatement = writerConnection.prepareStatement(itemSql)) {
            claimStatement.setString(1, claim.id().toString());
            claimStatement.setString(2, claim.instanceId().toString());
            claimStatement.setString(3, claim.playerId().toString());
            claimStatement.setString(4, claim.status().name());
            claimStatement.setLong(5, claim.createdAt().toEpochMilli());
            setNullableLong(
                claimStatement,
                6,
                claim.deliveredAt() == null ? null : claim.deliveredAt().toEpochMilli());
            if (claimStatement.executeUpdate() == 0) {
              commitOrThrow();
              return;
            }
            int slot = 0;
            for (RewardItem item : claim.items()) {
              itemStatement.setString(1, claim.id().toString());
              itemStatement.setInt(2, slot++);
              itemStatement.setString(3, item.material());
              itemStatement.setInt(4, item.amount());
              itemStatement.addBatch();
            }
            itemStatement.executeBatch();
            commitOrThrow();
          } catch (SQLException exception) {
            rollbackQuietly();
            throw new IllegalStateException("Failed to persist reward claim", exception);
          }
        });
  }

  @Override
  public List<RewardClaim> findPending(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    String sql =
        "SELECT claim_id, instance_id, player_uuid, status, created_at, delivered_at FROM spectra_reward_claims WHERE player_uuid = ? AND status = ? ORDER BY created_at";
    List<RewardClaim> claims = new ArrayList<>();
    try (Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, playerId.toString());
      statement.setString(2, RewardClaimStatus.PENDING.name());
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          claims.add(readRewardClaim(connection, resultSet));
        }
      }
      return List.copyOf(claims);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to load pending reward claims", exception);
    }
  }

  @Override
  public boolean beginDelivery(UUID claimId) {
    return transitionRewardClaim(
        Objects.requireNonNull(claimId, "claimId"),
        RewardClaimStatus.PENDING,
        RewardClaimStatus.DELIVERING);
  }

  @Override
  public void returnToPending(UUID claimId) {
    transitionRewardClaim(
        Objects.requireNonNull(claimId, "claimId"),
        RewardClaimStatus.DELIVERING,
        RewardClaimStatus.PENDING);
  }

  @Override
  public boolean markDelivered(UUID claimId) {
    UUID nonNullClaimId = Objects.requireNonNull(claimId, "claimId");
    java.util.concurrent.atomic.AtomicBoolean updated =
        new java.util.concurrent.atomic.AtomicBoolean();
    executor.executeAndWait(
        () -> {
          String sql =
              "UPDATE spectra_reward_claims SET status = ?, delivered_at = ? WHERE claim_id = ? AND status = ?";
          try (PreparedStatement statement = writerConnection.prepareStatement(sql)) {
            statement.setString(1, RewardClaimStatus.DELIVERED.name());
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, nonNullClaimId.toString());
            statement.setString(4, RewardClaimStatus.DELIVERING.name());
            updated.set(statement.executeUpdate() == 1);
            commitOrThrow();
          } catch (SQLException exception) {
            rollbackQuietly();
            throw new IllegalStateException("Failed to mark reward claim delivered", exception);
          }
        });
    return updated.get();
  }

  private boolean transitionRewardClaim(
      UUID claimId, RewardClaimStatus expected, RewardClaimStatus next) {
    java.util.concurrent.atomic.AtomicBoolean updated =
        new java.util.concurrent.atomic.AtomicBoolean();
    executor.executeAndWait(
        () -> {
          String sql =
              "UPDATE spectra_reward_claims SET status = ? WHERE claim_id = ? AND status = ?";
          try (PreparedStatement statement = writerConnection.prepareStatement(sql)) {
            statement.setString(1, next.name());
            statement.setString(2, claimId.toString());
            statement.setString(3, expected.name());
            updated.set(statement.executeUpdate() == 1);
            commitOrThrow();
          } catch (SQLException exception) {
            rollbackQuietly();
            throw new IllegalStateException("Failed to transition reward claim", exception);
          }
        });
    return updated.get();
  }

  private RewardClaim readRewardClaim(Connection connection, ResultSet resultSet)
      throws SQLException {
    UUID claimId = UUID.fromString(resultSet.getString("claim_id"));
    List<RewardItem> items = new ArrayList<>();
    try (PreparedStatement itemStatement =
        connection.prepareStatement(
            "SELECT material, amount FROM spectra_reward_items WHERE claim_id = ? ORDER BY slot")) {
      itemStatement.setString(1, claimId.toString());
      try (ResultSet itemResults = itemStatement.executeQuery()) {
        while (itemResults.next()) {
          items.add(
              new RewardItem(itemResults.getString("material"), itemResults.getInt("amount")));
        }
      }
    }
    long deliveredAt = resultSet.getLong("delivered_at");
    boolean deliveredAtMissing = resultSet.wasNull();
    return new RewardClaim(
        claimId,
        new EventInstanceId(UUID.fromString(resultSet.getString("instance_id"))),
        UUID.fromString(resultSet.getString("player_uuid")),
        items,
        RewardClaimStatus.valueOf(resultSet.getString("status")),
        Instant.ofEpochMilli(resultSet.getLong("created_at")),
        deliveredAtMissing ? null : Instant.ofEpochMilli(deliveredAt));
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

      executor.enqueue(
          () -> {
            try (PreparedStatement stmt1 = writerConnection.prepareStatement(sql1);
                PreparedStatement stmt2 = writerConnection.prepareStatement(sql2);
                PreparedStatement stmt3 = writerConnection.prepareStatement(sql3)) {
              stmt1.setString(1, idStr);
              stmt1.executeUpdate();
              stmt2.setString(1, idStr);
              stmt2.executeUpdate();
              stmt3.setString(1, idStr);
              stmt3.executeUpdate();
              writerConnection.commit();
            } catch (SQLException e) {
              try {
                writerConnection.rollback();
              } catch (SQLException e2) {
                LOGGER.log(Level.WARNING, "Failed to rollback: " + e2.getMessage(), e2);
              }
              LOGGER.log(Level.SEVERE, "Failed to remove instance from DB: " + e.getMessage(), e);
            }
          });
    }
    return removed;
  }
}
