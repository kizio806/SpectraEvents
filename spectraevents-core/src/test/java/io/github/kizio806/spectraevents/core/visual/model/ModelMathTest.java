package io.github.kizio806.spectraevents.core.visual.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ModelMathTest {

  private static final float EPSILON = 1e-4f;

  @Test
  void testVectorOperations() {
    Vector3 v1 = Vector3.of(1.0f, 2.0f, 3.0f);
    Vector3 v2 = Vector3.of(4.0f, 5.0f, 6.0f);

    assertEquals(Vector3.of(5.0f, 7.0f, 9.0f), v1.add(v2));
    assertEquals(Vector3.of(-3.0f, -3.0f, -3.0f), v1.subtract(v2));
    assertEquals(Vector3.of(2.0f, 4.0f, 6.0f), v1.multiply(2.0f));
    assertEquals(Vector3.of(4.0f, 10.0f, 18.0f), v1.multiply(v2));
  }

  @Test
  void testVectorInvalidNumbers() {
    assertThrows(IllegalArgumentException.class, () -> Vector3.of(Float.NaN, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> Vector3.of(0, Float.POSITIVE_INFINITY, 0));
  }

  @Test
  void testQuaternionRotation() {
    // 90 degrees around Y axis
    Quaternion qYaw90 = Quaternion.fromEulerDegrees(0.0f, 90.0f, 0.0f);

    // Vector pointing along Z axis (0, 0, 1)
    Vector3 vZ = Vector3.of(0.0f, 0.0f, 1.0f);
    Vector3 rotated = qYaw90.transform(vZ);

    // Rotating (0,0,1) by 90 degrees around Y turns it to (1,0,0)
    assertEquals(1.0f, rotated.x(), EPSILON);
    assertEquals(0.0f, rotated.y(), EPSILON);
    assertEquals(0.0f, rotated.z(), EPSILON);
  }

  @Test
  void testTransformHierarchyComposition() {
    // Parent: Translated by (10, 0, 0), Rotated 90 degrees around Y
    ModelTransform parent =
        ModelTransform.of(
            Vector3.of(10.0f, 0.0f, 0.0f), Quaternion.fromEulerDegrees(0.0f, 90.0f, 0.0f));

    // Child: Local translation (0, 5, 2) relative to parent
    ModelTransform child = ModelTransform.of(Vector3.of(0.0f, 5.0f, 2.0f));

    // Composed: (0, 5, 2) rotated 90 deg around Y becomes (2, 5, 0), plus parent translation (10,
    // 0, 0) => (12, 5, 0)
    ModelTransform composed = child.compose(parent);

    assertEquals(12.0f, composed.translation().x(), EPSILON);
    assertEquals(5.0f, composed.translation().y(), EPSILON);
    assertEquals(0.0f, composed.translation().z(), EPSILON);
  }

  @Test
  void testThreeLevelHierarchyComposition() {
    ModelTransform root = ModelTransform.of(Vector3.of(1.0f, 0.0f, 0.0f));
    ModelTransform body = ModelTransform.of(Vector3.of(0.0f, 2.0f, 0.0f));
    ModelTransform arm = ModelTransform.of(Vector3.of(0.0f, 0.0f, 3.0f));

    ModelTransform composedBody = body.compose(root);
    ModelTransform composedArm = arm.compose(composedBody);

    assertEquals(1.0f, composedArm.translation().x(), EPSILON);
    assertEquals(2.0f, composedArm.translation().y(), EPSILON);
    assertEquals(3.0f, composedArm.translation().z(), EPSILON);
  }

  @Test
  void testInvalidScaleGuardrail() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ModelTransform(Vector3.ZERO, Quaternion.IDENTITY, Vector3.ZERO, Vector3.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ModelTransform(
                Vector3.ZERO, Quaternion.IDENTITY, Vector3.of(2000.0f, 1.0f, 1.0f), Vector3.ZERO));
  }
}
