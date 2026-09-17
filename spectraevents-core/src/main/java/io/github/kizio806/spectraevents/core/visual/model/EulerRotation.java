package io.github.kizio806.spectraevents.core.visual.model;

/** Platform-neutral authoring representation of Euler angles in degrees. */
public record EulerRotation(float pitchX, float yawY, float rollZ) {
  public static final EulerRotation ZERO = new EulerRotation(0.0f, 0.0f, 0.0f);

  public EulerRotation {
    if (Float.isNaN(pitchX) || Float.isNaN(yawY) || Float.isNaN(rollZ)) {
      throw new IllegalArgumentException(
          "EulerRotation angles cannot be NaN: [" + pitchX + ", " + yawY + ", " + rollZ + "]");
    }
    if (Float.isInfinite(pitchX) || Float.isInfinite(yawY) || Float.isInfinite(rollZ)) {
      throw new IllegalArgumentException(
          "EulerRotation angles cannot be Infinite: [" + pitchX + ", " + yawY + ", " + rollZ + "]");
    }
  }

  public EulerRotation lerp(EulerRotation target, float t) {
    java.util.Objects.requireNonNull(target, "target EulerRotation cannot be null");
    float p = pitchX + (target.pitchX - pitchX) * t;
    float y = yawY + (target.yawY - yawY) * t;
    float r = rollZ + (target.rollZ - rollZ) * t;
    return new EulerRotation(p, y, r);
  }

  public Quaternion toQuaternion() {
    return Quaternion.fromEulerDegrees(pitchX, yawY, rollZ);
  }
}
