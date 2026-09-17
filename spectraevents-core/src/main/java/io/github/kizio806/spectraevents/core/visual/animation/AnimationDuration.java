package io.github.kizio806.spectraevents.core.visual.animation;

import java.util.Objects;

/** Immutable duration of an animation clip. */
public record AnimationDuration(AnimationTime time) {
  public AnimationDuration {
    Objects.requireNonNull(time, "time cannot be null");
  }

  public static AnimationDuration of(AnimationTime time) {
    return new AnimationDuration(time);
  }

  public static AnimationDuration fromMillis(long millis) {
    return new AnimationDuration(AnimationTime.fromMillis(millis));
  }

  public static AnimationDuration fromSeconds(double seconds) {
    return new AnimationDuration(AnimationTime.fromSeconds(seconds));
  }

  public long toNanos() {
    return time.nanoseconds();
  }
}
