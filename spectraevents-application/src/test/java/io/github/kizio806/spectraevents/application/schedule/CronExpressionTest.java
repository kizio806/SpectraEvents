package io.github.kizio806.spectraevents.application.schedule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class CronExpressionTest {
  @Test
  void matchesRangesStepsListsAndSundayAlias() {
    CronExpression expression = CronExpression.parse("*/15 18 1-31/2 1,6,12 0,1-5");
    assertTrue(expression.matches(ZonedDateTime.of(2026, 6, 15, 18, 30, 0, 0, ZoneId.of("UTC"))));
    assertFalse(expression.matches(ZonedDateTime.of(2026, 6, 14, 18, 31, 0, 0, ZoneId.of("UTC"))));
  }

  @Test
  void rejectsMalformedOrOutOfRangeExpressions() {
    assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("* * * *"));
    assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("60 * * * *"));
    assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("*/0 * * * *"));
  }
}
