package io.github.kizio806.spectraevents.core.visual.animation;

import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.Objects;

/** Keyframe for scale property channel. */
public record ScaleKeyframe(AnimationTime time, Vector3 scale, Easing easing)
    implements Comparable<ScaleKeyframe> {

  public ScaleKeyframe {
    Objects.requireNonNull(time, "time cannot be null");
    Objects.requireNonNull(scale, "scale cannot be null");
    Objects.requireNonNull(easing, "easing cannot be null");
  }

  @Override
  public int compareTo(ScaleKeyframe o) {
    return this.time.compareTo(o.time);
  }
}
