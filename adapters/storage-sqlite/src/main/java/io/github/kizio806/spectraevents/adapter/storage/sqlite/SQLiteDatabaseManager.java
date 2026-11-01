package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages SQLite database connection lifecycles, WAL pragmas, schema migrations, and single-writer
 * persistence.
 */
public final class SQLiteDatabaseManager implements Closeable {
  private static final Logger LOGGER = Logger.getLogger(SQLiteDatabaseManager.class.getName());
  private static final int DEFAULT_EXECUTOR_CAPACITY = 10000;

  private final Path dbPath;
  private final String jdbcUrl;
  private volatile Connection writerConnection;
  private volatile SingleWriterPersistenceExecutor executor;
  private volatile boolean initialized;

  public SQLiteDatabaseManager(Path dbPath) {
    this.dbPath = Objects.requireNonNull(dbPath, "dbPath");
    this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    Path parent = dbPath.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  public synchronized void initialize() {
    if (initialized) {
      return;
    }
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
      writerConnection.setAutoCommit(false);

      try (Statement stmt = writerConnection.createStatement()) {
        stmt.execute("PRAGMA busy_timeout=5000;");
        applySchemaMigrations(stmt);
        writerConnection.commit();
      } catch (SQLException e) {
        writerConnection.rollback();
        throw e;
      }

      executor = new SingleWriterPersistenceExecutor(DEFAULT_EXECUTOR_CAPACITY);
      executor.start();
      initialized = true;
      LOGGER.fine("SQLite database initialized at " + dbPath);
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize SQLite storage: " + e.getMessage(), e);
      shutdown();
      throw new IllegalStateException("Failed to initialize SQLite storage", e);
    }
  }

  private void applySchemaMigrations(Statement stmt) throws SQLException {
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
      stmt.execute("ALTER TABLE spectra_instance_state ADD COLUMN minimum_contribution INTEGER;");
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
  }

  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl);
  }

  public Connection writerConnection() {
    return writerConnection;
  }

  public SingleWriterPersistenceExecutor executor() {
    return executor;
  }

  public Path dbPath() {
    return dbPath;
  }

  public void commitOrThrow() {
    try {
      Connection conn = writerConnection;
      if (conn != null) {
        conn.commit();
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to commit SQLite transaction", exception);
    }
  }

  public void rollbackQuietly() {
    try {
      Connection conn = writerConnection;
      if (conn != null) {
        conn.rollback();
      }
    } catch (SQLException ignored) {
      // Preserve the original persistence failure.
    }
  }

  public void shutdown() {
    SingleWriterPersistenceExecutor currentExecutor;
    synchronized (this) {
      currentExecutor = executor;
      executor = null;
      initialized = false;
    }

    if (currentExecutor != null) {
      try {
        currentExecutor.shutdown();
      } catch (RuntimeException exception) {
        LOGGER.log(
            Level.SEVERE, "Persistence writes did not drain; keeping SQLite open", exception);
      }
    }

    Connection currentWriter;
    synchronized (this) {
      currentWriter = writerConnection;
      writerConnection = null;
    }

    if (currentWriter != null) {
      try {
        if (!currentWriter.isClosed()) {
          currentWriter.close();
        }
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Error closing writer connection: " + e.getMessage());
      }
    }
  }

  @Override
  public void close() {
    shutdown();
  }
}
