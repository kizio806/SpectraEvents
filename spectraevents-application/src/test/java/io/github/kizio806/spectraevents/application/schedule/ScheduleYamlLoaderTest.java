package io.github.kizio806.spectraevents.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScheduleYamlLoaderTest {
  private final ScheduleYamlLoader loader = new ScheduleYamlLoader();

  @Test
  void loadsAValidatedScheduleWithAnExplicitTimeZone() {
    var schedules =
        loader.load(
            """
            schema-version: 1
            schedules:
              - id: nightly-metin
                definition: metin
                cron: "0 20 * * *"
                timezone: Europe/Warsaw
                world: world
                x: 10
                y: 80
                z: -5
                parameters:
                  health: 250
                  lock-duration: 15m
            """,
            "schedules.yml");
    assertEquals(1, schedules.size());
    assertEquals("nightly-metin", schedules.getFirst().id());
    assertEquals(250, schedules.getFirst().parameterOverrides().get("health"));
    assertEquals("15m", schedules.getFirst().parameterOverrides().get("lock-duration"));
  }

  @Test
  void includesFileAndYamlPathForInvalidEntries() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                loader.load(
                    "schema-version: 1\nschedules:\n  - id: invalid id\n", "schedules.yml"));
    assertTrue(exception.getMessage().contains("schedules.yml:schedules[0].definition"));
  }

  @Test
  void keepsValidSchedulesWhenAnotherScheduleIsMalformed() {
    ScheduleLoadResult result =
        loader.loadResilient(
            """
            schema-version: 1
            schedules:
              - id: valid-metin
                definition: metin
                cron: "0 20 * * *"
                timezone: Europe/Warsaw
                world: world
                x: 10
                y: 80
                z: -5
              - id: invalid
                definition: metin
                cron: "invalid"
                timezone: Europe/Warsaw
                world: world
                x: 1
                y: 80
                z: 1
            """,
            "schedules.yml");
    assertEquals(1, result.schedules().size());
    assertEquals(1, result.failures().size());
    assertTrue(result.failures().getFirst().path().contains("schedules.yml:schedules[1]"));
  }
}
