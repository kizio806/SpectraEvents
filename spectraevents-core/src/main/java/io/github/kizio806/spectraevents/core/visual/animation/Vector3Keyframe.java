package io.github.kizio806.spectraevents.core.visual.animation;

import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.Objects;

/** Keyframe for 3D Vector3 property channels (Translation, Scale). */
public record Vector3Keyframe(AnimationTime time, Vector3 value, Easing easing)
    implements Comparable<Vector3Keyframe> {

  public Vector3Keyframe {
    Objects.requireNonNull(time, "time cannot be null");
    Objects.requireNonNull(value, "value cannot be null");
    Objects.requireNonNull(easing, "easing cannot be null");
  }

  @Override
  public int compareTo(Vector3Keyframe o) {
    return this.time.compareTo(o.time);
  }
}
