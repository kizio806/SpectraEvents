package io.github.kizio806.spectraevents.application.execution;

import java.util.Objects;

/** Circular world zone reserved by one large event instance. */
public record EventZone(String world, double x, double z, double radius) {
  public EventZone {
    Objects.requireNonNull(world, "world");
    if (world.isBlank()) {
      throw new IllegalArgumentException("world must not be blank");
    }
    if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(radius) || radius <= 0.0d) {
      throw new IllegalArgumentException(
          "zone coordinates must be finite and radius must be positive");
    }
  }

  public static EventZone at(EventLocation location, double radius) {
    Objects.requireNonNull(location, "location");
    return new EventZone(location.world(), location.x(), location.z(), radius);
  }

  public boolean overlaps(EventZone other) {
    Objects.requireNonNull(other, "other");
    if (!world.equals(other.world)) {
      return false;
    }
    double dx = x - other.x;
    double dz = z - other.z;
    double requiredDistance = radius + other.radius;
    return (dx * dx) + (dz * dz) < requiredDistance * requiredDistance;
  }

  public boolean contains(EventLocation location) {
    Objects.requireNonNull(location, "location");
    if (!world.equals(location.world())) {
      return false;
    }
    double dx = x - location.x();
    double dz = z - location.z();
    return (dx * dx) + (dz * dz) <= radius * radius;
  }
}
