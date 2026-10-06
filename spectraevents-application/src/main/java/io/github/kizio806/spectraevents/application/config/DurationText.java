package io.github.kizio806.spectraevents.application.config;

import java.time.Duration;
import java.util.Objects;

/** Parses the compact duration syntax used by event YAML and formats it without unit surprises. */
public final class DurationText {
  private DurationText() {}

  public static Duration parse(Object value) {
    String text = String.valueOf(Objects.requireNonNull(value, "value")).trim();
    if (text.endsWith("ms")) {
      return Duration.ofMillis(Long.parseLong(text.substring(0, text.length() - 2)));
    }
    if (text.endsWith("s")) {
      return Duration.ofSeconds(Long.parseLong(text.substring(0, text.length() - 1)));
    }
    if (text.endsWith("m")) {
      return Duration.ofMinutes(Long.parseLong(text.substring(0, text.length() - 1)));
    }
    if (text.endsWith("h")) {
      return Duration.ofHours(Long.parseLong(text.substring(0, text.length() - 1)));
    }
    throw new IllegalArgumentException("Duration must end with ms, s, m, or h.");
  }

  public static String format(Duration duration) {
    Objects.requireNonNull(duration, "duration");
    if (duration.isNegative() || duration.isZero()) {
      throw new IllegalArgumentException("Duration must be positive.");
    }
    long millis = duration.toMillis();
    if (millis % 3_600_000L == 0L) {
      return duration.toHours() + "h";
    }
    if (millis % 60_000L == 0L) {
      return duration.toMinutes() + "m";
    }
    if (millis % 1_000L == 0L) {
      return duration.toSeconds() + "s";
    }
    return millis + "ms";
  }
}
