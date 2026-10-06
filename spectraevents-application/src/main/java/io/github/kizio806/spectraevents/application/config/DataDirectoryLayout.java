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
      # =============================================================================
      # SpectraEvents — Global Configuration
      # =============================================================================
      # This file contains plugin-wide settings only. Do not put event values here:
      #   events/<id>.yml           = the complete event definition
      #   events/overrides/<id>.yml = values changed through the admin panel or command
      #   locales/<locale>.yml      = player-facing text
      #   schedules.yml             = automatic starts
      #   locations.yml             = named event locations, created when first used
      #   data/                     = SQLite runtime state; do not edit while the server runs
      #
      # Use a bundled locale file name without the .yml suffix. English is the fallback
      # if a key is missing from the selected translation.
      schema-version: 2
      locale: en-US
      """;
  private static final String DEFAULT_SCHEDULES =
      """
      # =============================================================================
      # SpectraEvents — Scheduled Event Starts
      # =============================================================================
      # Add one schedule entry per automatic event start. Each entry is validated
      # independently, so an invalid entry does not disable valid siblings.
      # Give every entry an explicit IANA timezone. Optional parameters are scalar
      # per-run definition overrides, not a global settings layer.
      #
      # schedules:
      #   - id: weekday-meteor
      #     definition: meteor
      #     cron: "0 20 * * 1-5"
      #     timezone: Europe/Warsaw
      #     world: world
      #     x: 0
      #     y: 100
      #     z: 0
      #     parameters:
      #       health: 250
      # Use /spectraevents schedule list after editing and reload to apply changes.
      schema-version: 1
      schedules: []
      """;
  private static final String DEFAULT_LOCATIONS =
      """
      # =============================================================================
      # SpectraEvents — Named Event Locations
      # =============================================================================
      # The admin panel and /spectraevents location commands add entries here.
      # Coordinates are plugin-managed; edit only while the server is stopped.
      schema-version: 1
      locations: {}
      """;

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
    if (!Files.exists(layout.locationsFile())) {
      writeAtomically(layout.locationsFile(), DEFAULT_LOCATIONS);
    }
    return layout;
  }

  public Path configFile() {
    return root.resolve("config.yml");
  }

  public Path eventsDirectory() {
    return root.resolve("events");
  }

  public Path eventOverridesDirectory() {
    return eventsDirectory().resolve("overrides");
  }

  public Path locationsFile() {
    return root.resolve("locations.yml");
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

  /** Contains bundled, installable templates; its contents are never active definitions. */
  public Path templatesDirectory() {
    return root.resolve("templates");
  }

  private List<Path> requiredDirectories() {
    return List.of(
        eventsDirectory(),
        eventOverridesDirectory(),
        lootDirectory(),
        localesDirectory(),
        databaseFile().getParent(),
        backupsDirectory(),
        resourcePackCacheDirectory(),
        templatesDirectory());
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
