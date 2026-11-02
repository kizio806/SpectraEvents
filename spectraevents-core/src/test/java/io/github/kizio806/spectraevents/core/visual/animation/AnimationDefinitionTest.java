package io.github.kizio806.spectraevents.core.visual.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnimationDefinitionTest {

  @Test
  void testAnimationTimeConversionsAndComparison() {
    AnimationTime t1 = AnimationTime.fromSeconds(1.0);
    assertEquals(1_000_000_000L, t1.nanoseconds());
    assertEquals(1000L, t1.toMillis());
    assertEquals(1.0, t1.toSeconds(), 1e-4);
    assertEquals(20L, t1.toTicks());

    AnimationTime t2 = AnimationTime.fromMillis(500);
    assertEquals(500_000_000L, t2.nanoseconds());

    AnimationTime t3 = AnimationTime.fromTicks(10);
    assertEquals(500_000_000L, t3.nanoseconds());

    assertEquals(0, t2.compareTo(t3));
    assertTrue(t1.compareTo(t2) > 0);
    assertTrue(t2.compareTo(t1) < 0);

    AnimationTime added = t1.add(t2);
    assertEquals(1500L, added.toMillis());

    AnimationTime subtracted = t1.subtract(t2);
    assertEquals(500L, subtracted.toMillis());

    assertThrows(IllegalArgumentException.class, () -> AnimationTime.fromNanos(-1));
  }

  @Test
  void testAnimationDuration() {
    AnimationTime time = AnimationTime.fromSeconds(2.0);
    AnimationDuration duration = AnimationDuration.of(time);
    assertEquals(time, duration.time());

    assertThrows(NullPointerException.class, () -> AnimationDuration.of(null));
  }

  @Test
  void testTimelineCue() {
    TimelineCue cue1 = new TimelineCue(AnimationTime.fromMillis(200), "sound_step");
    TimelineCue cue2 = new TimelineCue(AnimationTime.fromMillis(500), "particle_burst");

    assertEquals("sound_step", cue1.cueId());
    assertEquals(200L, cue1.time().toMillis());
    assertTrue(cue1.compareTo(cue2) < 0);

    assertThrows(NullPointerException.class, () -> new TimelineCue(null, "cue"));
    assertThrows(NullPointerException.class, () -> new TimelineCue(AnimationTime.ZERO, null));
    assertThrows(IllegalArgumentException.class, () -> new TimelineCue(AnimationTime.ZERO, "   "));
  }

  @Test
  void testRotationKeyframe() {
    AnimationTime t1 = AnimationTime.fromMillis(100);
    AnimationTime t2 = AnimationTime.fromMillis(200);

    RotationKeyframe kf1 =
        RotationKeyframe.of(t1, Quaternion.IDENTITY, Easing.LINEAR, RotationMode.SHORTEST);
    assertEquals(t1, kf1.time());
    assertEquals(Quaternion.IDENTITY, kf1.rotation());
    assertNull(kf1.euler());
    assertEquals(Easing.LINEAR, kf1.easing());
    assertEquals(RotationMode.SHORTEST, kf1.rotationMode());

    EulerRotation euler = new EulerRotation(10, 20, 30);
    RotationKeyframe kf2 =
        RotationKeyframe.ofEuler(t2, euler, Easing.EASE_IN_OUT_QUAD, RotationMode.CONTINUOUS);
    assertEquals(euler, kf2.euler());
    assertEquals(RotationMode.CONTINUOUS, kf2.rotationMode());

    assertTrue(kf1.compareTo(kf2) < 0);

    assertThrows(
        NullPointerException.class,
        () -> RotationKeyframe.of(null, Quaternion.IDENTITY, Easing.LINEAR, RotationMode.SHORTEST));
    assertThrows(
        NullPointerException.class,
        () -> RotationKeyframe.of(t1, null, Easing.LINEAR, RotationMode.SHORTEST));
    assertThrows(
        NullPointerException.class,
        () -> RotationKeyframe.of(t1, Quaternion.IDENTITY, null, RotationMode.SHORTEST));
    assertThrows(
        NullPointerException.class,
        () -> RotationKeyframe.of(t1, Quaternion.IDENTITY, Easing.LINEAR, null));
    assertThrows(
        NullPointerException.class,
        () -> RotationKeyframe.ofEuler(t1, null, Easing.LINEAR, RotationMode.CONTINUOUS));
  }

  @Test
  void testScaleKeyframe() {
    AnimationTime t1 = AnimationTime.fromMillis(100);
    AnimationTime t2 = AnimationTime.fromMillis(200);

    ScaleKeyframe skf1 = new ScaleKeyframe(t1, Vector3.ONE, Easing.LINEAR);
    ScaleKeyframe skf2 = new ScaleKeyframe(t2, Vector3.of(2, 2, 2), Easing.EASE_IN_QUAD);

    assertEquals(t1, skf1.time());
    assertEquals(Vector3.ONE, skf1.scale());
    assertEquals(Easing.LINEAR, skf1.easing());
    assertTrue(skf1.compareTo(skf2) < 0);

    assertThrows(
        NullPointerException.class, () -> new ScaleKeyframe(null, Vector3.ONE, Easing.LINEAR));
    assertThrows(NullPointerException.class, () -> new ScaleKeyframe(t1, null, Easing.LINEAR));
    assertThrows(NullPointerException.class, () -> new ScaleKeyframe(t1, Vector3.ONE, null));
  }

  @Test
  void testVector3Keyframe() {
    AnimationTime t1 = AnimationTime.fromMillis(100);
    AnimationTime t2 = AnimationTime.fromMillis(200);

    Vector3Keyframe vkf1 = new Vector3Keyframe(t1, Vector3.ZERO, Easing.LINEAR);
    Vector3Keyframe vkf2 = new Vector3Keyframe(t2, Vector3.of(1, 2, 3), Easing.EASE_OUT_QUAD);

    assertEquals(t1, vkf1.time());
    assertEquals(Vector3.ZERO, vkf1.value());
    assertTrue(vkf1.compareTo(vkf2) < 0);

    assertThrows(
        NullPointerException.class, () -> new Vector3Keyframe(null, Vector3.ZERO, Easing.LINEAR));
    assertThrows(NullPointerException.class, () -> new Vector3Keyframe(t1, null, Easing.LINEAR));
    assertThrows(NullPointerException.class, () -> new Vector3Keyframe(t1, Vector3.ZERO, null));
  }

  @Test
  void testAnimationDefinitionCreationAndValidation() {
    AnimationId id = new AnimationId("attack");
    AnimationDuration duration = AnimationDuration.of(AnimationTime.fromSeconds(1.5));
    AnimationTarget target = AnimationTarget.ofPart(new ModelPartId("sword"));

    RotationKeyframe kf =
        RotationKeyframe.of(
            AnimationTime.ZERO, Quaternion.IDENTITY, Easing.LINEAR, RotationMode.SHORTEST);
    TimelineCue cue = new TimelineCue(AnimationTime.fromMillis(500), "hit");

    AnimationDefinition anim =
        AnimationDefinition.of(
            id,
            duration,
            LoopMode.ONCE,
            1,
            Map.of(target, List.of(kf)),
            List.of(cue),
            RecoveryPolicy.STOP);

    assertEquals(id, anim.id());
    assertEquals(duration, anim.duration());
    assertEquals(LoopMode.ONCE, anim.loopMode());
    assertEquals(1, anim.loopCount());
    assertEquals(1, anim.tracks().size());
    assertEquals(1, anim.cues().size());
    assertEquals(RecoveryPolicy.STOP, anim.recoveryPolicy());

    // Validation: null checks
    assertThrows(
        NullPointerException.class,
        () ->
            new AnimationDefinition(
                null, duration, LoopMode.ONCE, 1, Map.of(), List.of(), RecoveryPolicy.STOP));
    assertThrows(
        NullPointerException.class,
        () ->
            new AnimationDefinition(
                id, null, LoopMode.ONCE, 1, Map.of(), List.of(), RecoveryPolicy.STOP));
    assertThrows(
        NullPointerException.class,
        () ->
            new AnimationDefinition(
                id, duration, null, 1, Map.of(), List.of(), RecoveryPolicy.STOP));
    assertThrows(
        NullPointerException.class,
        () -> new AnimationDefinition(id, duration, LoopMode.ONCE, 1, Map.of(), List.of(), null));

    // Zero/negative duration validation
    AnimationDuration zeroDuration = AnimationDuration.of(AnimationTime.ZERO);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AnimationDefinition(
                id, zeroDuration, LoopMode.ONCE, 1, Map.of(), List.of(), RecoveryPolicy.STOP));
  }

  @Test
  void testModelPoseAndPartPose() {
    ModelPartId partId = new ModelPartId("wing");
    ModelTransform transform =
        new ModelTransform(Vector3.of(1, 0, 0), Quaternion.IDENTITY, Vector3.ONE, Vector3.ZERO);
    PartPose partPose = new PartPose(transform);
    assertEquals(transform, partPose.localTransform());

    ModelPose modelPose = ModelPose.of(ModelTransform.IDENTITY, Map.of(partId, transform));
    assertEquals(ModelTransform.IDENTITY, modelPose.rootTransform());
    assertEquals(1, modelPose.partTransforms().size());
    assertEquals(transform, modelPose.partTransforms().get(partId));
  }

  @Test
  void testAllEasingCurves() {
    for (Easing easing : Easing.values()) {
      double at0 = easing.evaluate(0.0);
      double at1 = easing.evaluate(1.0);
      double atMid = easing.evaluate(0.5);

      assertTrue(Double.isFinite(at0));
      assertTrue(Double.isFinite(at1));
      assertTrue(Double.isFinite(atMid));

      // Test clamp logic
      assertTrue(Double.isFinite(easing.evaluate(-0.5)));
      assertTrue(Double.isFinite(easing.evaluate(1.5)));
    }
  }
}
