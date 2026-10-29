package io.github.kizio806.spectraevents.application.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataDirectoryLayoutTest {
  @TempDir Path temporaryDirectory;

  @Test
  void createsTheStableDirectoryLayoutAndMigratesTheLegacyDatabaseWithABackup() throws Exception {
    Path root = temporaryDirectory.resolve("SpectraEvents");
    Files.createDirectories(root);
    Files.writeString(root.resolve("spectraevents.db"), "legacy-db");

    DataDirectoryLayout layout = DataDirectoryLayout.prepare(root);

    assertTrue(Files.isDirectory(layout.eventsDirectory()));
    assertTrue(Files.isDirectory(layout.eventOverridesDirectory()));
    assertTrue(Files.isDirectory(layout.lootDirectory()));
    assertTrue(Files.isDirectory(layout.localesDirectory()));
    assertTrue(Files.isDirectory(layout.resourcePackCacheDirectory()));
    assertEquals("legacy-db", Files.readString(layout.databaseFile()));
    assertFalse(Files.exists(root.resolve("spectraevents.db")));
    try (var backups = Files.list(layout.backupsDirectory())) {
      Path backup = backups.findFirst().orElseThrow();
      assertEquals("legacy-db", Files.readString(backup.resolve("spectraevents.db")));
    }
    assertTrue(Files.readString(layout.configFile()).contains("schema-version: 2"));
    assertTrue(Files.readString(layout.configFile()).contains("locale: en-US"));
    assertTrue(Files.readString(layout.schedulesFile()).contains("schema-version: 1"));
    assertTrue(Files.readString(layout.locationsFile()).contains("locations: {}"));
  }
}
