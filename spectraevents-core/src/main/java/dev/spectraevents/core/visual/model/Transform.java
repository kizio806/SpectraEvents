package dev.spectraevents.core.visual.model;

/**
 * Platform-agnostic spatial transform holding translation, rotation (euler angles in degrees), and
 * scale.
 */
public record Transform(
    float tx, float ty, float tz, float rx, float ry, float rz, float sx, float sy, float sz) {
  public static final Transform IDENTITY = new Transform(0, 0, 0, 0, 0, 0, 1, 1, 1);

  public static Transform translation(float tx, float ty, float tz) {
    return new Transform(tx, ty, tz, 0, 0, 0, 1, 1, 1);
  }
}
