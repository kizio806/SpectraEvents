package io.github.kizio806.spectraevents.core.visual.animation;

import java.util.Objects;

/**
 * Immutable canonical representation of time within an animation timeline, stored in nanoseconds.
 */
public record AnimationTime(long nanoseconds) implements Comparable<AnimationTime> {
  public static final AnimationTime ZERO = new AnimationTime(0L);

  public AnimationTime {
    if (nanoseconds < 0) {
      throw new IllegalArgumentException("AnimationTime cannot be negative: " + nanoseconds);
    }
  }

  public static AnimationTime fromNanos(long nanos) {
    return new AnimationTime(nanos);
  }

  public static AnimationTime fromMillis(long millis) {
    return new AnimationTime(Math.multiplyExact(millis, 1_000_000L));
  }

  public static AnimationTime fromSeconds(double seconds) {
    if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0) {
      throw new IllegalArgumentException("Invalid seconds value: " + seconds);
    }
    return new AnimationTime(Math.round(seconds * 1_000_000_000.0));
  }

  public static AnimationTime fromTicks(long ticks) {
    return fromMillis(ticks * 50L);
  }

  public long toMillis() {
    return nanoseconds / 1_000_000L;
  }

  public double toSeconds() {
    return nanoseconds / 1_000_000_000.0;
  }

  public long toTicks() {
    return toMillis() / 50L;
  }

  public AnimationTime add(AnimationTime other) {
    Objects.requireNonNull(other, "other cannot be null");
    return new AnimationTime(Math.addExact(this.nanoseconds, other.nanoseconds));
  }

  public AnimationTime subtract(AnimationTime other) {
    Objects.requireNonNull(other, "other cannot be null");
    long result = this.nanoseconds - other.nanoseconds;
    return new AnimationTime(Math.max(0L, result));
  }

  @Override
  public int compareTo(AnimationTime o) {
    return Long.compare(this.nanoseconds, o.nanoseconds);
  }
}
