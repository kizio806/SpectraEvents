package io.github.kizio806.spectraevents.core.event.definition;

import java.time.Duration;
import java.util.Objects;

/**
 * Optional, platform-neutral operating limits for a large encounter.
 *
 * <p>A zero radius denotes an ordinary event without a reserved world zone. The deadline is an
 * absolute encounter limit, independent from phase timers.
 */
public record EventEncounterSettings(
    double zoneRadius, Duration deadline, int minimumContribution, int maxActivePerWorld) {
  public static final EventEncounterSettings NONE =
      new EventEncounterSettings(0.0d, Duration.ZERO, 0, 0);

  public EventEncounterSettings(double zoneRadius, Duration deadline, int minimumContribution) {
    this(zoneRadius, deadline, minimumContribution, zoneRadius > 0.0d ? 1 : 0);
  }

  public EventEncounterSettings {
    Objects.requireNonNull(deadline, "deadline");
    if (!Double.isFinite(zoneRadius) || zoneRadius < 0.0d) {
      throw new IllegalArgumentException("zoneRadius must be finite and non-negative");
    }
    if (deadline.isNegative()) {
      throw new IllegalArgumentException("deadline must not be negative");
    }
    if (minimumContribution < 0) {
      throw new IllegalArgumentException("minimumContribution must not be negative");
    }
    if (maxActivePerWorld < 0) {
      throw new IllegalArgumentException("maxActivePerWorld must not be negative");
    }
    if (zoneRadius > 0.0d && maxActivePerWorld < 1) {
      throw new IllegalArgumentException(
          "zone-reserving encounters require a positive maxActivePerWorld");
    }
  }

  public boolean reservesZone() {
    return zoneRadius > 0.0d;
  }

  public boolean hasDeadline() {
    return !deadline.isZero();
  }
}
