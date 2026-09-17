package dev.spectraevents.core.event.definition;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable identity of an event definition.
 *
 * @param value identity value (e.g., "meteor", "airdrop")
 */
public record EventDefinitionId(String value) {
  private static final Pattern VALID_FORMAT = Pattern.compile("^[a-z0-9_]+$");

  /**
   * Creates an event definition identifier.
   *
   * @param value identifier value
   */
  public EventDefinitionId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("EventDefinitionId cannot be blank");
    }
    if (!VALID_FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "EventDefinitionId must contain only lowercase letters, numbers, and underscores");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
