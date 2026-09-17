package io.github.kizio806.spectraevents.core.visual.animation;

import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import java.util.Objects;

/** Keyframe for rotation property channel with SLERP or continuous Euler delta interpolation. */
public record RotationKeyframe(
    AnimationTime time,
    Quaternion rotation,
    EulerRotation euler,
    Easing easing,
    RotationMode rotationMode)
    implements Comparable<RotationKeyframe> {

  public RotationKeyframe {
    Objects.requireNonNull(time, "time cannot be null");
    Objects.requireNonNull(rotation, "rotation cannot be null");
    Objects.requireNonNull(easing, "easing cannot be null");
    Objects.requireNonNull(rotationMode, "rotationMode cannot be null");
  }

  public static RotationKeyframe of(
      AnimationTime time, Quaternion rotation, Easing easing, RotationMode rotationMode) {
    return new RotationKeyframe(time, rotation, null, easing, rotationMode);
  }

  public static RotationKeyframe ofEuler(
      AnimationTime time, EulerRotation euler, Easing easing, RotationMode rotationMode) {
    Objects.requireNonNull(euler, "euler cannot be null");
    return new RotationKeyframe(time, euler.toQuaternion(), euler, easing, rotationMode);
  }

  @Override
  public int compareTo(RotationKeyframe o) {
    return this.time.compareTo(o.time);
  }
}
