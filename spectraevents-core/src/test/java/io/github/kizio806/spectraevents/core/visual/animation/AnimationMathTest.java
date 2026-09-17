package io.github.kizio806.spectraevents.core.visual.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import org.junit.jupiter.api.Test;

class AnimationMathTest {

  private static final float EPSILON = 1e-4f;

  @Test
  void testVector3Lerp() {
    Vector3 start = Vector3.of(0.0f, 10.0f, 20.0f);
    Vector3 end = Vector3.of(10.0f, 20.0f, 40.0f);

    Vector3 mid = start.lerp(end, 0.5f);
    assertEquals(5.0f, mid.x(), EPSILON);
    assertEquals(15.0f, mid.y(), EPSILON);
    assertEquals(30.0f, mid.z(), EPSILON);
  }

  @Test
  void testQuaternionSlerpShortestPath() {
    Quaternion q1 = Quaternion.IDENTITY;
    Quaternion q2 = Quaternion.fromEulerDegrees(0.0f, 90.0f, 0.0f);

    Quaternion mid = q1.slerp(q2, 0.5f);
    Quaternion expectedMid = Quaternion.fromEulerDegrees(0.0f, 45.0f, 0.0f);

    assertEquals(expectedMid.x(), mid.x(), EPSILON);
    assertEquals(expectedMid.y(), mid.y(), EPSILON);
    assertEquals(expectedMid.z(), mid.z(), EPSILON);
    assertEquals(expectedMid.w(), mid.w(), EPSILON);
  }

  @Test
  void testQuaternionSlerpOppositeSignShortestArc() {
    Quaternion q1 = Quaternion.of(0.0f, 0.0f, 0.0f, 1.0f);
    // Negated quaternion representing the exact same orientation
    Quaternion q2 = Quaternion.of(0.0f, 0.0f, 0.0f, -1.0f);

    Quaternion mid = q1.slerp(q2, 0.5f);
    assertNotNull(mid);
    assertTrue(Math.abs(mid.w()) > 0.99f);
  }

  @Test
  void testEulerRotationLerpContinuousSpin() {
    EulerRotation e1 = new EulerRotation(0.0f, 0.0f, 0.0f);
    EulerRotation e2 = new EulerRotation(0.0f, 720.0f, 0.0f);

    EulerRotation mid = e1.lerp(e2, 0.5f);
    assertEquals(0.0f, mid.pitchX(), EPSILON);
    assertEquals(360.0f, mid.yawY(), EPSILON);
    assertEquals(0.0f, mid.rollZ(), EPSILON);
  }

  @Test
  void testEasingFunctions() {
    assertEquals(0.0, Easing.LINEAR.evaluate(0.0), EPSILON);
    assertEquals(0.5, Easing.LINEAR.evaluate(0.5), EPSILON);
    assertEquals(1.0, Easing.LINEAR.evaluate(1.0), EPSILON);

    assertTrue(Easing.EASE_IN_QUAD.evaluate(0.5) < 0.5);
    assertTrue(Easing.EASE_OUT_QUAD.evaluate(0.5) > 0.5);
  }
}
