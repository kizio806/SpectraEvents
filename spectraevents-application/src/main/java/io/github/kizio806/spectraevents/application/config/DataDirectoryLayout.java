package io.github.kizio806.spectraevents.application.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** The stable on-disk layout for one single-server SpectraEvents installation. */
public record DataDirectoryLayout(Path root) {
  private static final DateTimeFormatter BACKUP_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
  private static final String DEFAULT_CONFIG =
      """
      schema-version: 1
      default-locale: en-US
      history-retention-days: 90
      overrides: {}
      locations: {}
      """;
  private static final String DEFAULT_SCHEDULES = "schema-version: 1\nschedules: []\n";

  public DataDirectoryLayout {
    root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
  }

  /** Creates required directories and safely relocates the pre-1.0 SQLite file when present. */
  public static DataDirectoryLayout prepare(Path root) throws IOException {
    DataDirectoryLayout layout = new DataDirectoryLayout(root);
    Files.createDirectories(layout.root());
    for (Path directory : layout.requiredDirectories()) {
      Files.createDirectories(directory);
    }
    layout.migrateLegacyDatabase();
    if (!Files.exists(layout.configFile())) {
      writeAtomically(layout.configFile(), DEFAULT_CONFIG);
    }
    if (!Files.exists(layout.schedulesFile())) {
      writeAtomically(layout.schedulesFile(), DEFAULT_SCHEDULES);
    }
    return layout;
  }

  public Path configFile() {
    return root.resolve("config.yml");
  }

  public Path eventsDirectory() {
    return root.resolve("events");
  }

  public Path modelsDirectory() {
    return root.resolve("models");
  }

  public Path lootDirectory() {
    return root.resolve("loot");
  }

  public Path schedulesFile() {
    return root.resolve("schedules.yml");
  }

  public Path localesDirectory() {
    return root.resolve("locales");
  }

  public Path databaseFile() {
    return root.resolve("data").resolve("spectraevents.db");
  }

  public Path backupsDirectory() {
    return root.resolve("backups");
  }

  public Path resourcePackCacheDirectory() {
    return root.resolve("cache").resolve("resource-pack");
  }

  private List<Path> requiredDirectories() {
    return List.of(
        eventsDirectory(),
        modelsDirectory(),
        lootDirectory(),
        localesDirectory(),
        databaseFile().getParent(),
        backupsDirectory(),
        resourcePackCacheDirectory());
  }

  private void migrateLegacyDatabase() throws IOException {
    Path legacyDatabase = root.resolve("spectraevents.db");
    if (!Files.isRegularFile(legacyDatabase) || Files.exists(databaseFile())) {
      return;
    }
    Path backupDirectory = createBackupDirectory();
    copyIfPresent(legacyDatabase, backupDirectory.resolve("spectraevents.db"));
    copyIfPresent(legacySidecar("-wal"), backupDirectory.resolve("spectraevents.db-wal"));
    copyIfPresent(legacySidecar("-shm"), backupDirectory.resolve("spectraevents.db-shm"));

    moveAtomically(legacyDatabase, databaseFile());
    moveIfPresent(legacySidecar("-wal"), databaseFile().resolveSibling("spectraevents.db-wal"));
    moveIfPresent(legacySidecar("-shm"), databaseFile().resolveSibling("spectraevents.db-shm"));
  }

  private Path legacySidecar(String suffix) {
    return root.resolve("spectraevents.db" + suffix);
  }

  private Path createBackupDirectory() throws IOException {
    String stem = ZonedDateTime.now(ZoneOffset.UTC).format(BACKUP_TIMESTAMP);
    for (int suffix = 0; suffix < 10_000; suffix++) {
      Path candidate = backupsDirectory().resolve(suffix == 0 ? stem : stem + "-" + suffix);
      try {
        return Files.createDirectory(candidate);
      } catch (java.nio.file.FileAlreadyExistsException ignored) {
        // A separate migration ran in the same second; choose a deterministic unused suffix.
      }
    }
    throw new IOException("Could not allocate a unique SpectraEvents backup directory");
  }

  private static void copyIfPresent(Path source, Path target) throws IOException {
    if (Files.isRegularFile(source)) {
      Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
    }
  }

  private static void moveIfPresent(Path source, Path target) throws IOException {
    if (Files.isRegularFile(source)) {
      moveAtomically(source, target);
    }
  }

  private static void writeAtomically(Path target, String content) throws IOException {
    Files.createDirectories(target.getParent());
    Path temporary =
        Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
    try {
      Files.writeString(temporary, content, StandardCharsets.UTF_8);
      moveAtomically(temporary, target);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void moveAtomically(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException ignored) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
