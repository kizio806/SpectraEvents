package io.github.kizio806.spectraevents.application.execution;

import java.util.Objects;
import java.util.Optional;

/** Platform-neutral persisted anchor for an event instance. */
public record EventLocation(String world, double x, double y, double z, float yaw, float pitch) {
  private static final String PREFIX = "v1|";

  public EventLocation {
    Objects.requireNonNull(world, "world");
    if (world.isBlank() || world.contains("|")) {
      throw new IllegalArgumentException("world must be non-blank and must not contain '|'");
    }
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
      throw new IllegalArgumentException("coordinates must be finite");
    }
  }

  public String serialize() {
    return PREFIX + world + "|" + x + "|" + y + "|" + z + "|" + yaw + "|" + pitch;
  }

  public static Optional<EventLocation> deserialize(String encoded) {
    if (encoded == null || !encoded.startsWith(PREFIX)) {
      return Optional.empty();
    }
    String[] fields = encoded.split("\\|", -1);
    if (fields.length != 7 || !"v1".equals(fields[0])) {
      return Optional.empty();
    }
    try {
      return Optional.of(
          new EventLocation(
              fields[1],
              Double.parseDouble(fields[2]),
              Double.parseDouble(fields[3]),
              Double.parseDouble(fields[4]),
              Float.parseFloat(fields[5]),
              Float.parseFloat(fields[6])));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
