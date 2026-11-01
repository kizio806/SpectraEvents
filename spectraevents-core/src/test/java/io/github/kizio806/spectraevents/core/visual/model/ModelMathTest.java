package io.github.kizio806.spectraevents.core.visual.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    assertEquals(14.0f, v1.lengthSquared(), EPSILON);

    Vector3 lerped = v1.lerp(v2, 0.5f);
    assertEquals(2.5f, lerped.x(), EPSILON);
    assertEquals(3.5f, lerped.y(), EPSILON);
    assertEquals(4.5f, lerped.z(), EPSILON);
  }

  @Test
  void testVectorNullAndInvalidNumbers() {
    assertThrows(IllegalArgumentException.class, () -> Vector3.of(Float.NaN, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> Vector3.of(0, Float.NaN, 0));
    assertThrows(IllegalArgumentException.class, () -> Vector3.of(0, 0, Float.NaN));
    assertThrows(IllegalArgumentException.class, () -> Vector3.of(0, Float.POSITIVE_INFINITY, 0));
    assertThrows(IllegalArgumentException.class, () -> Vector3.of(Float.NEGATIVE_INFINITY, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> Vector3.of(0, 0, Float.POSITIVE_INFINITY));

    Vector3 v = Vector3.of(1.0f, 2.0f, 3.0f);
    assertThrows(NullPointerException.class, () -> v.add(null));
    assertThrows(NullPointerException.class, () -> v.subtract(null));
    assertThrows(NullPointerException.class, () -> v.multiply((Vector3) null));
    assertThrows(NullPointerException.class, () -> v.lerp(null, 0.5f));
  }

  @Test
  void testEulerRotation() {
    EulerRotation rot = new EulerRotation(10.0f, 20.0f, 30.0f);
    assertEquals(10.0f, rot.pitchX(), EPSILON);
    assertEquals(20.0f, rot.yawY(), EPSILON);
    assertEquals(30.0f, rot.rollZ(), EPSILON);

    EulerRotation target = new EulerRotation(20.0f, 40.0f, 60.0f);
    EulerRotation lerped = rot.lerp(target, 0.5f);
    assertEquals(15.0f, lerped.pitchX(), EPSILON);
    assertEquals(30.0f, lerped.yawY(), EPSILON);
    assertEquals(45.0f, lerped.rollZ(), EPSILON);

    Quaternion q = rot.toQuaternion();
    assertNotNull(q);

    assertThrows(IllegalArgumentException.class, () -> new EulerRotation(Float.NaN, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new EulerRotation(0, Float.NaN, 0));
    assertThrows(IllegalArgumentException.class, () -> new EulerRotation(0, 0, Float.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> new EulerRotation(Float.POSITIVE_INFINITY, 0, 0));
    assertThrows(
        IllegalArgumentException.class, () -> new EulerRotation(0, Float.POSITIVE_INFINITY, 0));
    assertThrows(
        IllegalArgumentException.class, () -> new EulerRotation(0, 0, Float.NEGATIVE_INFINITY));
    assertThrows(NullPointerException.class, () -> rot.lerp(null, 0.5f));
  }

  @Test
  void testQuaternionRotation() {
    Quaternion qYaw90 = Quaternion.fromEulerDegrees(0.0f, 90.0f, 0.0f);
    Vector3 vZ = Vector3.of(0.0f, 0.0f, 1.0f);
    Vector3 rotated = qYaw90.transform(vZ);

    assertEquals(1.0f, rotated.x(), EPSILON);
    assertEquals(0.0f, rotated.y(), EPSILON);
    assertEquals(0.0f, rotated.z(), EPSILON);
  }

  @Test
  void testQuaternionValidationAndNormalization() {
    assertThrows(IllegalArgumentException.class, () -> new Quaternion(Float.NaN, 0, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> new Quaternion(0, Float.NaN, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> new Quaternion(0, 0, Float.NaN, 1));
    assertThrows(IllegalArgumentException.class, () -> new Quaternion(0, 0, 0, Float.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> new Quaternion(Float.POSITIVE_INFINITY, 0, 0, 1));
    assertThrows(
        IllegalArgumentException.class, () -> new Quaternion(0, Float.NEGATIVE_INFINITY, 0, 1));
    assertThrows(
        IllegalArgumentException.class, () -> new Quaternion(0, 0, Float.POSITIVE_INFINITY, 1));
    assertThrows(
        IllegalArgumentException.class, () -> new Quaternion(0, 0, 0, Float.NEGATIVE_INFINITY));

    Quaternion zeroLen = new Quaternion(0.0f, 0.0f, 0.0f, 0.0f).normalize();
    assertEquals(Quaternion.IDENTITY, zeroLen);

    Quaternion normalized = Quaternion.of(0.0f, 3.0f, 4.0f, 0.0f);
    float len =
        (float)
            Math.sqrt(
                normalized.x() * normalized.x()
                    + normalized.y() * normalized.y()
                    + normalized.z() * normalized.z()
                    + normalized.w() * normalized.w());
    assertEquals(1.0f, len, EPSILON);

    Quaternion unit = Quaternion.IDENTITY.normalize();
    assertSame(Quaternion.IDENTITY, unit);
  }

  @Test
  void testQuaternionMultiplicationAndDot() {
    Quaternion q1 = Quaternion.fromEulerDegrees(90.0f, 0.0f, 0.0f);
    Quaternion q2 = Quaternion.fromEulerDegrees(0.0f, 90.0f, 0.0f);
    Quaternion multiplied = q1.multiply(q2);
    assertNotNull(multiplied);

    float dot = q1.dot(q2);
    assertTrue(Float.isFinite(dot));

    assertThrows(NullPointerException.class, () -> q1.multiply(null));
    assertThrows(NullPointerException.class, () -> q1.dot(null));
    assertThrows(NullPointerException.class, () -> q1.transform(null));
    assertThrows(NullPointerException.class, () -> q1.slerp(null, 0.5f));
  }

  @Test
  void testQuaternionSlerp() {
    Quaternion q1 = Quaternion.fromEulerDegrees(0.0f, 0.0f, 0.0f);
    Quaternion q2 = Quaternion.fromEulerDegrees(0.0f, 90.0f, 0.0f);

    assertSame(q1, q1.slerp(q2, 0.0f));
    assertSame(q2, q1.slerp(q2, 1.0f));

    Quaternion mid = q1.slerp(q2, 0.5f);
    Vector3 vZ = Vector3.of(0.0f, 0.0f, 1.0f);
    Vector3 rotated = mid.transform(vZ);
    assertEquals((float) Math.sin(Math.toRadians(45.0)), rotated.x(), EPSILON);
    assertEquals((float) Math.cos(Math.toRadians(45.0)), rotated.z(), EPSILON);

    // Test near-identical angles (cosTheta > 0.9995)
    Quaternion qClose = Quaternion.fromEulerDegrees(0.0f, 0.01f, 0.0f);
    Quaternion closeInterp = q1.slerp(qClose, 0.5f);
    assertNotNull(closeInterp);

    // Test opposite quaternions (negative dot product shortest-path branch)
    Quaternion qOpposite = new Quaternion(-q2.x(), -q2.y(), -q2.z(), -q2.w());
    Quaternion oppositeInterp = q1.slerp(qOpposite, 0.5f);
    assertNotNull(oppositeInterp);
  }

  @Test
  void testTransformHierarchyComposition() {
    ModelTransform parent =
        ModelTransform.of(
            Vector3.of(10.0f, 0.0f, 0.0f), Quaternion.fromEulerDegrees(0.0f, 90.0f, 0.0f));

    ModelTransform child = ModelTransform.of(Vector3.of(0.0f, 5.0f, 2.0f));

    ModelTransform composed = child.compose(parent);

    assertEquals(12.0f, composed.translation().x(), EPSILON);
    assertEquals(5.0f, composed.translation().y(), EPSILON);
    assertEquals(0.0f, composed.translation().z(), EPSILON);
  }

  @Test
  void testTransformWithScaleAndPivot() {
    ModelTransform transform =
        ModelTransform.of(
            Vector3.of(1.0f, 2.0f, 3.0f), Quaternion.IDENTITY, Vector3.of(2.0f, 2.0f, 2.0f));
    assertEquals(2.0f, transform.scale().x(), EPSILON);

    ModelTransform childWithPivot =
        new ModelTransform(
            Vector3.ZERO,
            Quaternion.fromEulerDegrees(0.0f, 90.0f, 0.0f),
            Vector3.ONE,
            Vector3.of(1.0f, 0.0f, 1.0f));
    ModelTransform composed = childWithPivot.compose(ModelTransform.IDENTITY);
    assertNotNull(composed);
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
    assertThrows(
        NullPointerException.class,
        () -> new ModelTransform(null, Quaternion.IDENTITY, Vector3.ONE, Vector3.ZERO));
    assertThrows(
        NullPointerException.class,
        () -> new ModelTransform(Vector3.ZERO, null, Vector3.ONE, Vector3.ZERO));
    assertThrows(
        NullPointerException.class,
        () -> new ModelTransform(Vector3.ZERO, Quaternion.IDENTITY, null, Vector3.ZERO));
    assertThrows(
        NullPointerException.class,
        () -> new ModelTransform(Vector3.ZERO, Quaternion.IDENTITY, Vector3.ONE, null));
    assertThrows(NullPointerException.class, () -> ModelTransform.IDENTITY.compose(null));
  }
}
