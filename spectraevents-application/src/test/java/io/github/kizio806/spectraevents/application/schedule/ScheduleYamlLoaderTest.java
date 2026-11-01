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

  @Test
  void testLoadFailures() {
    // 1. Invalid YAML
    var r1 = loader.loadResilient("key: \"unclosed string", "schedules.yml");
    assertEquals(1, r1.failures().size());
    assertTrue(r1.failures().getFirst().message().contains("invalid YAML"));

    // 2. Root not a mapping
    var r2 = loader.loadResilient("- item1\n- item2", "schedules.yml");
    assertEquals("schedules.yml:root", r2.failures().getFirst().path());
    assertTrue(r2.failures().getFirst().message().contains("must be a YAML mapping"));

    // 3. Schema version not 1
    var r3 = loader.loadResilient("schema-version: 2\nschedules: []", "schedules.yml");
    assertEquals("schedules.yml:schema-version", r3.failures().getFirst().path());

    // 4. Schedules not a list
    var r4 = loader.loadResilient("schema-version: 1\nschedules: not_a_list", "schedules.yml");
    assertEquals("schedules.yml:schedules", r4.failures().getFirst().path());

    // 5. Entry not a mapping
    var r5 =
        loader.loadResilient("schema-version: 1\nschedules:\n  - just_a_string\n", "schedules.yml");
    assertEquals("schedules.yml:schedules[0]", r5.failures().getFirst().path());
    assertTrue(r5.failures().getFirst().message().contains("must be a mapping"));

    // 6. Non-numeric coordinates
    var r6 =
        loader.loadResilient(
            """
            schema-version: 1
            schedules:
              - id: test
                definition: test
                cron: "0 * * * *"
                timezone: UTC
                world: world
                x: not_a_number
                y: 64
                z: 0
            """,
            "schedules.yml");
    assertTrue(r6.failures().getFirst().message().contains("must be numeric"));
  }
}
