package io.github.kizio806.spectraevents.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemScheduleLoaderTest {
  @TempDir java.nio.file.Path temporaryDirectory;

  @Test
  void preservesValidEntriesWhenASeparateEntryIsInvalid() throws Exception {
    java.nio.file.Path file = temporaryDirectory.resolve("schedules.yml");
    Files.writeString(
        file,
        """
        schema-version: 1
        schedules:
          - id: valid-metin
            definition: metin
            cron: "0 20 * * *"
            timezone: Europe/Warsaw
            world: world
            x: 1
            y: 80
            z: 1
          - id: broken
            definition: metin
            cron: "not cron"
            timezone: Europe/Warsaw
            world: world
            x: 1
            y: 80
            z: 1
        """);

    ScheduleLoadResult result = new FileSystemScheduleLoader(new ScheduleYamlLoader()).load(file);

    assertEquals(1, result.schedules().size());
    assertEquals(1, result.failures().size());
    assertTrue(result.failures().getFirst().path().endsWith("schedules.yml:schedules[1]"));
  }
}
