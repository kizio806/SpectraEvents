package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/**
 * Platform-neutral spatial transform representing 3D translation, canonical Quaternion rotation,
 * scale, and pivot origin.
 */
public record ModelTransform(
    Vector3 translation, Quaternion rotation, Vector3 scale, Vector3 pivot) {
  public static final ModelTransform IDENTITY =
      new ModelTransform(Vector3.ZERO, Quaternion.IDENTITY, Vector3.ONE, Vector3.ZERO);

  public ModelTransform {
    Objects.requireNonNull(translation, "translation cannot be null");
    Objects.requireNonNull(rotation, "rotation cannot be null");
    Objects.requireNonNull(scale, "scale cannot be null");
    Objects.requireNonNull(pivot, "pivot cannot be null");

    // Guard against non-sensical scales (e.g. 0 or absurdly huge scale factors)
    if (scale.x() == 0.0f || scale.y() == 0.0f || scale.z() == 0.0f) {
      throw new IllegalArgumentException(
          "ModelTransform scale components cannot be zero: " + scale);
    }
    if (Math.abs(scale.x()) > 1000.0f
        || Math.abs(scale.y()) > 1000.0f
        || Math.abs(scale.z()) > 1000.0f) {
      throw new IllegalArgumentException(
          "ModelTransform scale exceeds guardrail limit of 1000.0: " + scale);
    }
  }

  public static ModelTransform of(Vector3 translation) {
    return new ModelTransform(translation, Quaternion.IDENTITY, Vector3.ONE, Vector3.ZERO);
  }

  public static ModelTransform of(Vector3 translation, Quaternion rotation) {
    return new ModelTransform(translation, rotation, Vector3.ONE, Vector3.ZERO);
  }

  public static ModelTransform of(Vector3 translation, Quaternion rotation, Vector3 scale) {
    return new ModelTransform(translation, rotation, scale, Vector3.ZERO);
  }

  /**
   * Composes this (child) transform with a parent transform in hierarchical space.
   *
   * @param parent the parent transform
   * @return composed absolute transform relative to root anchor
   */
  public ModelTransform compose(ModelTransform parent) {
    Objects.requireNonNull(parent, "parent transform cannot be null");

    // 1. Calculate effective local child displacement considering child pivot
    Vector3 pivotOffset = pivot.subtract(rotation.transform(pivot.multiply(scale)));
    Vector3 childEffectiveOffset = translation.add(pivotOffset);

    // 2. Scale and rotate child displacement by parent transform
    Vector3 scaledChildOffset = childEffectiveOffset.multiply(parent.scale());
    Vector3 rotatedChildOffset = parent.rotation().transform(scaledChildOffset);

    // 3. Composed translation is parent translation plus rotated child displacement
    Vector3 composedTranslation = parent.translation().add(rotatedChildOffset);

    // 4. Composed rotation is parent rotation multiplied by child rotation
    Quaternion composedRotation = parent.rotation().multiply(this.rotation);

    // 5. Composed scale is component-wise product of parent and child scale
    Vector3 composedScale = parent.scale().multiply(this.scale);

    return new ModelTransform(composedTranslation, composedRotation, composedScale, this.pivot);
  }
}
