package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SQLiteDatabaseManagerTest {

  @TempDir Path tempDir;

  private Path dbPath;
  private SQLiteDatabaseManager dbManager;

  @BeforeEach
  void setUp() {
    dbPath = tempDir.resolve("sub/dir/test.db");
    dbManager = new SQLiteDatabaseManager(dbPath);
  }

  @AfterEach
  void tearDown() {
    if (dbManager != null) {
      dbManager.shutdown();
    }
  }

  @Test
  void initializesDatabaseAndDirectoriesCleanly() throws SQLException {
    assertTrue(Files.exists(dbPath.getParent()));
    assertFalse(dbManager.isInitialized());

    dbManager.initialize();
    assertTrue(dbManager.isInitialized());
    assertNotNull(dbManager.executor());
    assertNotNull(dbManager.writerConnection());
    assertEquals(dbPath, dbManager.dbPath());

    // Calling initialize again should be idempotent
    dbManager.initialize();
    assertTrue(dbManager.isInitialized());

    try (Connection conn = dbManager.getConnection();
        Statement stmt = conn.createStatement()) {
      assertTrue(stmt.execute("SELECT 1;"));
    }
  }

  @Test
  void handlesCommitAndRollbackOperations() throws SQLException {
    dbManager.initialize();

    // Verify writer connection auto-commit is false
    assertFalse(dbManager.writerConnection().getAutoCommit());

    // Commit and rollback with valid connection
    dbManager.commitOrThrow();
    dbManager.rollbackQuietly();

    // After shutdown, operations on writerConnection should be handled safely
    dbManager.shutdown();
    assertNull(dbManager.writerConnection());
    assertFalse(dbManager.isInitialized());

    // commitOrThrow and rollbackQuietly when writerConnection is null should not throw
    dbManager.commitOrThrow();
    dbManager.rollbackQuietly();
  }

  @Test
  void testCommitOrThrowAndRollbackQuietlyWithClosedConnection() throws SQLException {
    dbManager.initialize();
    try (Connection writer = dbManager.writerConnection()) {
      // Closed upon exiting try-with-resources
      assertFalse(writer.isClosed());
    }

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> dbManager.commitOrThrow());
    assertTrue(ex.getMessage().contains("Failed to commit"));

    // rollbackQuietly handles closed connection without throwing
    dbManager.rollbackQuietly();
  }

  @Test
  void closeInvokesShutdown() {
    dbManager.initialize();
    assertTrue(dbManager.isInitialized());

    dbManager.close();
    assertFalse(dbManager.isInitialized());
    assertNull(dbManager.executor());
  }

  @Test
  void rejectsNullPath() {
    assertThrows(NullPointerException.class, () -> new SQLiteDatabaseManager(null));
  }

  @Test
  void handlesParentDirectoryCreationFailure() {
    // Attempting to create parent where a file blocks the directory creation
    File blocker = tempDir.resolve("file-blocker").toFile();
    try {
      assertTrue(blocker.createNewFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    Path blockedPath = blocker.toPath().resolve("nested.db");
    assertThrows(UncheckedIOException.class, () -> new SQLiteDatabaseManager(blockedPath));
  }

  @Test
  void handlesRelativePathWithoutParent() {
    try (SQLiteDatabaseManager relativeMgr = new SQLiteDatabaseManager(Path.of("no_parent.db"))) {
      assertEquals(Path.of("no_parent.db"), relativeMgr.dbPath());
    }
  }

  @Test
  void testInitializeFailsWhenDbPathIsDirectory() throws Exception {
    Path dir = tempDir.resolve("dir_as_db");
    Files.createDirectories(dir);
    try (SQLiteDatabaseManager mgr = new SQLiteDatabaseManager(dir)) {
      IllegalStateException ex = assertThrows(IllegalStateException.class, mgr::initialize);
      assertTrue(ex.getMessage().contains("Failed to initialize SQLite storage"));
    }
  }
}
