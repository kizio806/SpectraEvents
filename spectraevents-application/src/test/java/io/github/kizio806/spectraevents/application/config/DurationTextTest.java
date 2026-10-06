package io.github.kizio806.spectraevents.application.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationTextTest {
  @Test
  void formatsDurationsInTheLargestExactHumanReadableUnit() {
    assertEquals("15m", DurationText.format(Duration.ofMinutes(15)));
    assertEquals("90s", DurationText.format(Duration.ofSeconds(90)));
    assertEquals("1h", DurationText.format(Duration.ofHours(1)));
    assertEquals("250ms", DurationText.format(Duration.ofMillis(250)));
  }

  @Test
  void parsesTheSameSyntaxAcceptedByEventParameters() {
    assertEquals(Duration.ofMinutes(15), DurationText.parse("15m"));
    assertEquals(Duration.ofSeconds(90), DurationText.parse("90s"));
  }
}
