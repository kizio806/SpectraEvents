package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
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
public final class SQLiteEventInstanceRepository implements EventInstanceRepository {
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
        writerConnection.commit();
      } catch (SQLException e) {
        writerConnection.rollback();
        throw e;
      }

      loadAllFromDb();
      loadAllStatesFromDb();

      executor = new SingleWriterPersistenceExecutor(10000);
      executor.start();

      LOGGER.info(
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
        "SELECT instance_id, health_current, health_max, locked_until, claimant, platform_location, boss_entity_id, timer_deadline FROM spectra_instance_state";
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

          long lockedUntil = rs.getLong("locked_until");
          if (!rs.wasNull()) {
            state.setLockedUntilMillis(lockedUntil);
          }

          long timerDeadline = rs.getLong("timer_deadline");
          if (!rs.wasNull()) {
            state.setTimerDeadlineMillis(timerDeadline);
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
        INSERT INTO spectra_instance_state (instance_id, health_current, health_max, locked_until, claimant, platform_location, boss_entity_id, timer_deadline, last_updated)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(instance_id) DO UPDATE SET
          health_current = excluded.health_current,
          health_max = excluded.health_max,
          locked_until = excluded.locked_until,
          claimant = excluded.claimant,
          platform_location = excluded.platform_location,
          boss_entity_id = excluded.boss_entity_id,
          timer_deadline = excluded.timer_deadline,
          last_updated = excluded.last_updated;
        """;
    try (PreparedStatement stmt = writerConnection.prepareStatement(sql)) {
      stmt.setString(1, state.instanceId());
      setNullableInteger(stmt, 2, state.healthCurrent());
      setNullableInteger(stmt, 3, state.healthMaximum());
      setNullableLong(stmt, 4, state.lockedUntil());
      stmt.setString(5, state.claimant());
      stmt.setString(6, state.platformLocation());
      stmt.setString(7, state.bossEntityId());
      setNullableLong(stmt, 8, state.timerDeadline());
      stmt.setLong(9, state.updatedAt());
      stmt.executeUpdate();
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
        state.lockedUntilMillis() > 0 ? state.lockedUntilMillis() : null,
        state.claimant().orElse(null),
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
        System.currentTimeMillis());
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
      Long lockedUntil,
      String claimant,
      String platformLocation,
      String bossEntityId,
      Long timerDeadline,
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
  public boolean remove(EventInstanceId eventInstanceId) {
    boolean removed = cache.remove(eventInstanceId) != null;
    stateCache.remove(eventInstanceId);

    if (removed) {
      String sql1 = "DELETE FROM spectra_instances WHERE instance_id = ?";
      String sql2 = "DELETE FROM spectra_instance_state WHERE instance_id = ?";
      String idStr = eventInstanceId.toString();

      executor.enqueue(
          () -> {
            try (PreparedStatement stmt1 = writerConnection.prepareStatement(sql1);
                PreparedStatement stmt2 = writerConnection.prepareStatement(sql2)) {
              stmt1.setString(1, idStr);
              stmt1.executeUpdate();
              stmt2.setString(1, idStr);
              stmt2.executeUpdate();
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
