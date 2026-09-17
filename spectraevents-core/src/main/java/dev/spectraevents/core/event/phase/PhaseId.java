package dev.spectraevents.core.event.phase;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable identity of an event phase.
 *
 * @param value identity value (e.g., "falling", "impact")
 */
public record PhaseId(String value) {
  private static final Pattern VALID_FORMAT = Pattern.compile("^[a-z0-9_\\-]+$");

  /**
   * Creates a phase identifier.
   *
   * @param value identifier value
   */
  public PhaseId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("PhaseId cannot be blank");
    }
    if (!VALID_FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "PhaseId must contain only lowercase letters, numbers, hyphens, and underscores");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
