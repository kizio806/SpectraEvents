package io.github.kizio806.spectraevents.application.schedule;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Strict five-field cron expression for server-local event schedules. */
@SuppressWarnings("StringSplitter")
public final class CronExpression {
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");
  private static final Pattern LIST_SEPARATOR = Pattern.compile(",");
  private static final Pattern STEP_SEPARATOR = Pattern.compile("/");
  private static final Pattern RANGE_SEPARATOR = Pattern.compile("-");
  private final Field minutes;
  private final Field hours;
  private final Field daysOfMonth;
  private final Field months;
  private final Field daysOfWeek;

  private CronExpression(
      Field minutes, Field hours, Field daysOfMonth, Field months, Field daysOfWeek) {
    this.minutes = minutes;
    this.hours = hours;
    this.daysOfMonth = daysOfMonth;
    this.months = months;
    this.daysOfWeek = daysOfWeek;
  }

  public static CronExpression parse(String source) {
    String[] fields = WHITESPACE.split(Objects.requireNonNull(source, "source").trim());
    if (fields.length != 5) {
      throw new IllegalArgumentException("Cron expression must contain exactly five fields");
    }
    return new CronExpression(
        Field.parse(fields[0], 0, 59),
        Field.parse(fields[1], 0, 23),
        Field.parse(fields[2], 1, 31),
        Field.parse(fields[3], 1, 12),
        Field.parse(fields[4], 0, 7));
  }

  public boolean matches(ZonedDateTime time) {
    return minutes.matches(time.getMinute())
        && hours.matches(time.getHour())
        && daysOfMonth.matches(time.getDayOfMonth())
        && months.matches(time.getMonthValue())
        && daysOfWeek.matches(time.getDayOfWeek().getValue() % 7);
  }

  private record Field(Set<Integer> allowed) {
    static Field parse(String source, int min, int max) {
      Set<Integer> allowed = new HashSet<>();
      for (String part : LIST_SEPARATOR.split(source)) {
        parsePart(part, min, max, allowed);
      }
      if (allowed.isEmpty()) {
        throw new IllegalArgumentException("Cron field must not be empty");
      }
      return new Field(Set.copyOf(allowed));
    }

    boolean matches(int value) {
      return allowed.contains(value);
    }

    private static void parsePart(String source, int min, int max, Set<Integer> target) {
      String[] stepSplit = STEP_SEPARATOR.split(source, -1);
      if (stepSplit.length > 2) {
        throw new IllegalArgumentException("Invalid cron step: " + source);
      }
      int step = stepSplit.length == 2 ? integer(stepSplit[1], min, max) : 1;
      if (step < 1) {
        throw new IllegalArgumentException("Cron step must be positive");
      }
      String base = stepSplit[0];
      int start;
      int end;
      if ("*".equals(base)) {
        start = min;
        end = max;
      } else if (base.contains("-")) {
        String[] range = RANGE_SEPARATOR.split(base, -1);
        if (range.length != 2) {
          throw new IllegalArgumentException("Invalid cron range: " + source);
        }
        start = integer(range[0], min, max);
        end = integer(range[1], min, max);
      } else {
        start = integer(base, min, max);
        end = start;
      }
      if (start > end) {
        throw new IllegalArgumentException("Cron range start must not exceed its end");
      }
      for (int value = start; value <= end; value += step) {
        target.add(value == 7 && min == 0 ? 0 : value);
      }
    }

    private static int integer(String source, int min, int max) {
      try {
        int value = Integer.parseInt(source);
        if (value < min || value > max) {
          throw new IllegalArgumentException("Cron value is outside its allowed range: " + source);
        }
        return value;
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException("Invalid cron value: " + source, exception);
      }
    }
  }
}
