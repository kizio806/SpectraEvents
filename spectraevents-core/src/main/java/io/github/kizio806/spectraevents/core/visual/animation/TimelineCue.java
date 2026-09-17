package io.github.kizio806.spectraevents.core.visual.animation;

import java.util.Objects;

/** Marker cue on an animation timeline at a specific point in time. */
public record TimelineCue(AnimationTime time, String cueId) implements Comparable<TimelineCue> {
  public TimelineCue {
    Objects.requireNonNull(time, "time cannot be null");
    Objects.requireNonNull(cueId, "cueId cannot be null");
    if (cueId.isBlank()) {
      throw new IllegalArgumentException("cueId cannot be blank");
    }
  }

  @Override
  public int compareTo(TimelineCue o) {
    return this.time.compareTo(o.time);
  }
}
