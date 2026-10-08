package io.github.kizio806.spectraevents.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class EventScheduleTest {
  @Test
  void parsesCronAndIanaTimeZoneWithoutUsingServerDefaultTime() {
    EventSchedule schedule =
        EventSchedule.parse(
            "daily-metin",
            "metin",
            "0 20 * * *",
            "Europe/Warsaw",
            new EventLocation("world", 12, 80, -4, 0, 0));
    assertEquals(ZoneId.of("Europe/Warsaw"), schedule.zoneId());
  }

  @Test
  void rejectsUnstableIdsAndUnknownTimeZones() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            EventSchedule.parse(
                "Daily Metin",
                "metin",
                "0 20 * * *",
                "UTC",
                new EventLocation("world", 0, 0, 0, 0, 0)));
    assertThrows(
        java.time.DateTimeException.class,
        () ->
            EventSchedule.parse(
                "daily-metin",
                "metin",
                "0 20 * * *",
                "not/a-timezone",
                new EventLocation("world", 0, 0, 0, 0, 0)));
  }
}
