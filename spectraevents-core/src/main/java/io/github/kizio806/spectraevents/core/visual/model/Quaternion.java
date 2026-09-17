package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/**
 * Immutable normalized quaternion representing a 3D rotation (x, y, z, w). Canonical rotation
 * representation used across the 3D model engine.
 */
public record Quaternion(float x, float y, float z, float w) {
  public static final Quaternion IDENTITY = new Quaternion(0.0f, 0.0f, 0.0f, 1.0f);

  public Quaternion {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z) || Float.isNaN(w)) {
      throw new IllegalArgumentException(
          "Quaternion components cannot be NaN: [" + x + ", " + y + ", " + z + ", " + w + "]");
    }
    if (Float.isInfinite(x) || Float.isInfinite(y) || Float.isInfinite(z) || Float.isInfinite(w)) {
      throw new IllegalArgumentException(
          "Quaternion components cannot be Infinite: [" + x + ", " + y + ", " + z + ", " + w + "]");
    }
  }

  public static Quaternion of(float x, float y, float z, float w) {
    return new Quaternion(x, y, z, w).normalize();
  }

  /**
   * Constructs a unit Quaternion from Euler angles in degrees using Z-X-Y intrinsic rotation order.
   *
   * @param pitchX rotation around X axis in degrees
   * @param yawY rotation around Y axis in degrees
   * @param rollZ rotation around Z axis in degrees
   * @return normalized Quaternion
   */
  public static Quaternion fromEulerDegrees(float pitchX, float yawY, float rollZ) {
    return fromEulerRadians(
        (float) Math.toRadians(pitchX),
        (float) Math.toRadians(yawY),
        (float) Math.toRadians(rollZ));
  }

  /** Constructs a unit Quaternion from Euler angles in radians (Z-X-Y order). */
  public static Quaternion fromEulerRadians(float pitchX, float yawY, float rollZ) {
    float cx = (float) Math.cos(pitchX * 0.5f);
    float sx = (float) Math.sin(pitchX * 0.5f);
    float cy = (float) Math.cos(yawY * 0.5f);
    float sy = (float) Math.sin(yawY * 0.5f);
    float cz = (float) Math.cos(rollZ * 0.5f);
    float sz = (float) Math.sin(rollZ * 0.5f);

    float qw = cx * cy * cz - sx * sy * sz;
    float qx = sx * cy * cz + cx * sy * sz;
    float qy = cx * sy * cz - sx * cy * sz;
    float qz = cx * cy * sz + sx * sy * cz;

    return new Quaternion(qx, qy, qz, qw).normalize();
  }

  /**
   * Multiplies this quaternion by another quaternion (this * other). Represents applying 'other'
   * rotation followed by 'this' rotation.
   */
  public Quaternion multiply(Quaternion other) {
    Objects.requireNonNull(other, "other quaternion cannot be null");
    float qx = this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y;
    float qy = this.w * other.y - this.x * other.z + this.y * other.w + this.z * other.x;
    float qz = this.w * other.z + this.x * other.y - this.y * other.x + this.z * other.w;
    float qw = this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z;

    return new Quaternion(qx, qy, qz, qw).normalize();
  }

  /** Transforms (rotates) a 3D vector by this quaternion. */
  public Vector3 transform(Vector3 vector) {
    Objects.requireNonNull(vector, "vector cannot be null");
    float vx = vector.x();
    float vy = vector.y();
    float vz = vector.z();

    // Calculate q * v * q^-1
    float ix = w * vx + y * vz - z * vy;
    float iy = w * vy + z * vx - x * vz;
    float iz = w * vz + x * vy - y * vx;
    float iw = -x * vx - y * vy - z * vz;

    float rx = ix * w + iw * -x + iy * -z - iz * -y;
    float ry = iy * w + iw * -y + iz * -x - ix * -z;
    float rz = iz * w + iw * -z + ix * -y - iy * -x;

    return new Vector3(rx, ry, rz);
  }

  /** Calculates dot product between this quaternion and another. */
  public float dot(Quaternion other) {
    Objects.requireNonNull(other, "other quaternion cannot be null");
    return x * other.x + y * other.y + z * other.z + w * other.w;
  }

  /**
   * Performs Spherical Linear Interpolation (SLERP) between this quaternion and target.
   * Automatically handles shortest-path orientation (q vs -q) and near-identical angles.
   *
   * @param end target quaternion
   * @param t interpolation factor [0, 1]
   * @return interpolated unit quaternion
   */
  public Quaternion slerp(Quaternion end, float t) {
    Objects.requireNonNull(end, "end quaternion cannot be null");
    if (t <= 0.0f) return this;
    if (t >= 1.0f) return end;

    float cosTheta = this.dot(end);
    float endX = end.x;
    float endY = end.y;
    float endZ = end.z;
    float endW = end.w;

    // Shortest-path sign handling: if dot product is negative, invert target quaternion
    if (cosTheta < 0.0f) {
      cosTheta = -cosTheta;
      endX = -endX;
      endY = -endY;
      endZ = -endZ;
      endW = -endW;
    }

    if (cosTheta > 0.9995f) {
      // Near-identical quaternions, use linear interpolation to avoid division by zero
      float rx = this.x + (endX - this.x) * t;
      float ry = this.y + (endY - this.y) * t;
      float rz = this.z + (endZ - this.z) * t;
      float rw = this.w + (endW - this.w) * t;
      return new Quaternion(rx, ry, rz, rw).normalize();
    }

    double theta = Math.acos(cosTheta);
    double sinTheta = Math.sin(theta);
    double w1 = Math.sin((1.0 - t) * theta) / sinTheta;
    double w2 = Math.sin(t * theta) / sinTheta;

    float rx = (float) (this.x * w1 + endX * w2);
    float ry = (float) (this.y * w1 + endY * w2);
    float rz = (float) (this.z * w1 + endZ * w2);
    float rw = (float) (this.w * w1 + endW * w2);

    return new Quaternion(rx, ry, rz, rw).normalize();
  }

  /** Normalizes this quaternion to ensure unit length. */
  public Quaternion normalize() {
    float lenSq = x * x + y * y + z * z + w * w;
    if (lenSq == 0.0f) {
      return IDENTITY;
    }
    if (Math.abs(lenSq - 1.0f) < 1e-6f) {
      return this;
    }
    float invLen = (float) (1.0 / Math.sqrt(lenSq));
    return new Quaternion(x * invLen, y * invLen, z * invLen, w * invLen);
  }
}
