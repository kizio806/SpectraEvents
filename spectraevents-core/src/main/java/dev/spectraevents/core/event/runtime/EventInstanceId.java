package dev.spectraevents.core.event.runtime;

import java.util.Objects;
import java.util.UUID;

/** Stable identity of one runtime event instance. */
public record EventInstanceId(UUID value) {
  /**
   * Creates an event instance identifier.
   *
   * @param value identifier value
   */
  public EventInstanceId {
    Objects.requireNonNull(value, "value");
  }

  /**
   * Generates a new event instance identifier.
   *
   * @return generated identifier
   */
  public static EventInstanceId generate() {
    return new EventInstanceId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
