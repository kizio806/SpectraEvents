package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/** Immutable 3D floating-point vector with strict validation against NaN and Infinity. */
public record Vector3(float x, float y, float z) {
  public static final Vector3 ZERO = new Vector3(0.0f, 0.0f, 0.0f);
  public static final Vector3 ONE = new Vector3(1.0f, 1.0f, 1.0f);

  public Vector3 {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) {
      throw new IllegalArgumentException(
          "Vector3 components cannot be NaN: [" + x + ", " + y + ", " + z + "]");
    }
    if (Float.isInfinite(x) || Float.isInfinite(y) || Float.isInfinite(z)) {
      throw new IllegalArgumentException(
          "Vector3 components cannot be Infinite: [" + x + ", " + y + ", " + z + "]");
    }
  }

  public static Vector3 of(float x, float y, float z) {
    return new Vector3(x, y, z);
  }

  public Vector3 add(Vector3 other) {
    Objects.requireNonNull(other, "other vector cannot be null");
    return new Vector3(this.x + other.x, this.y + other.y, this.z + other.z);
  }

  public Vector3 subtract(Vector3 other) {
    Objects.requireNonNull(other, "other vector cannot be null");
    return new Vector3(this.x - other.x, this.y - other.y, this.z - other.z);
  }

  public Vector3 multiply(float scalar) {
    return new Vector3(this.x * scalar, this.y * scalar, this.z * scalar);
  }

  public Vector3 multiply(Vector3 scale) {
    Objects.requireNonNull(scale, "scale vector cannot be null");
    return new Vector3(this.x * scale.x, this.y * scale.y, this.z * scale.z);
  }

  public float lengthSquared() {
    return x * x + y * y + z * z;
  }

  public Vector3 lerp(Vector3 target, float t) {
    Objects.requireNonNull(target, "target vector cannot be null");
    float rx = this.x + (target.x - this.x) * t;
    float ry = this.y + (target.y - this.y) * t;
    float rz = this.z + (target.z - this.z) * t;
    return new Vector3(rx, ry, rz);
  }
}
