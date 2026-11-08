package io.github.kizio806.spectraevents.application.model.animation.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.TimelineCueSpec;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTarget;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTrackType;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnimationCompilerTest {

  @Test
  void testCompileValidAnimation() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("spin");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("2s");
    spec.setLoop("LOOP");

    KeyframeSpec kf1 = new KeyframeSpec();
    kf1.setAt("0s");
    kf1.setValue(List.of(0.0f, 0.0f, 0.0f));

    KeyframeSpec kf2 = new KeyframeSpec();
    kf2.setAt("2s");
    kf2.setValue(List.of(0.0f, 10.0f, 0.0f));

    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setTranslation(List.of(kf1, kf2));

    spec.setTracks(Map.of("part1", trackSpec));

    TimelineCueSpec cue = new TimelineCueSpec();
    cue.setAt("1s");
    cue.setId("midway");
    spec.setCues(List.of(cue));

    CompiledAnimation compiled = compiler.compile(animId, spec, Set.of("part1"));

    assertNotNull(compiled);
    assertEquals("spin", compiled.definition().id().value());
    assertEquals(LoopMode.LOOP, compiled.definition().loopMode());
    assertEquals(1, compiled.tracks().size());
    assertEquals(1, compiled.definition().cues().size());
    assertEquals("midway", compiled.definition().cues().get(0).cueId());
  }

  @Test
  void testCompileInvalidDurationThrowsException() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("invalid");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("0s");

    assertThrows(AnimationCompilerException.class, () -> compiler.compile(animId, spec, Set.of()));
  }

  @Test
  void testKeyframeExceedingDurationThrowsException() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("exceed");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");

    KeyframeSpec kf1 = new KeyframeSpec();
    kf1.setAt("0s");
    kf1.setValue(List.of(0.0f, 0.0f, 0.0f));

    KeyframeSpec kf2 = new KeyframeSpec();
    kf2.setAt("2s"); // Exceeds 1s
    kf2.setValue(List.of(0.0f, 10.0f, 0.0f));

    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setTranslation(List.of(kf1, kf2));

    spec.setTracks(Map.of("part1", trackSpec));

    assertThrows(
        AnimationCompilerException.class, () -> compiler.compile(animId, spec, Set.of("part1")));
  }

  @Test
  void testNonLinearEasingSubStepSegmentation() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("eased");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s"); // 20 ticks

    KeyframeSpec kf1 = new KeyframeSpec();
    kf1.setAt("0s");
    kf1.setValue(List.of(0.0f, 0.0f, 0.0f));
    kf1.setEasing("EASE_IN_OUT_QUAD");

    KeyframeSpec kf2 = new KeyframeSpec();
    kf2.setAt("1s");
    kf2.setValue(List.of(0.0f, 10.0f, 0.0f));

    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setTranslation(List.of(kf1, kf2));

    spec.setTracks(Map.of("part1", trackSpec));

    CompiledAnimation compiled = compiler.compile(animId, spec, Set.of("part1"));

    // Non-linear easing breaks 1s track into 20 sub-step segments (50ms / 1 tick each)
    CompiledTrack track = compiled.tracks().get(0);
    assertEquals(20, track.segments().size());
  }

  @Test
  void testRotationAndScaleTracksWithRoot() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("spin_scale");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");
    spec.setLoop("ONCE");
    spec.setRecovery("RESUME");

    // Rotation track with Euler angles
    KeyframeSpec rot1 = new KeyframeSpec();
    rot1.setAt("0s");
    rot1.setEuler(List.of(0.0f, 0.0f, 0.0f));

    KeyframeSpec rot2 = new KeyframeSpec();
    rot2.setAt("1s");
    rot2.setEuler(List.of(0.0f, 180.0f, 0.0f));
    rot2.setRotationMode("CONTINUOUS");

    // Scale track
    KeyframeSpec scale1 = new KeyframeSpec();
    scale1.setAt("0s");
    scale1.setValue(List.of(1.0f, 1.0f, 1.0f));

    KeyframeSpec scale2 = new KeyframeSpec();
    scale2.setAt("1s");
    scale2.setValue(List.of(2.0f, 2.0f, 2.0f));

    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setRotation(List.of(rot1, rot2));
    trackSpec.setScale(List.of(scale1, scale2));

    spec.setTracks(Map.of("root", trackSpec));

    CompiledAnimation compiled = compiler.compile(animId, spec, Set.of("part1"));
    assertNotNull(compiled);
    assertEquals(2, compiled.tracks().size());
  }

  @Test
  void testRotationTrackWithQuaternion() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("quat_rot");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");

    KeyframeSpec rot1 = new KeyframeSpec();
    rot1.setAt("0s");
    rot1.setQuaternion(List.of(0.0f, 0.0f, 0.0f, 1.0f));

    KeyframeSpec rot2 = new KeyframeSpec();
    rot2.setAt("1s");
    rot2.setQuaternion(List.of(0.0f, 1.0f, 0.0f, 0.0f));

    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setRotation(List.of(rot1, rot2));

    spec.setTracks(Map.of("part1", trackSpec));

    CompiledAnimation compiled = compiler.compile(animId, spec, Set.of("part1"));
    assertNotNull(compiled);
    assertEquals(1, compiled.tracks().size());
  }

  @Test
  void testCompilerValidationErrors() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("errors");

    // 1. Unknown target part
    AnimationSpec specUnknownPart = new AnimationSpec();
    specUnknownPart.setDuration("1s");
    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    KeyframeSpec kf = new KeyframeSpec();
    kf.setAt("0s");
    kf.setValue(List.of(0.0f, 0.0f, 0.0f));
    trackSpec.setTranslation(List.of(kf));
    specUnknownPart.setTracks(Map.of("missing_part", trackSpec));

    AnimationCompilerException ex1 =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specUnknownPart, Set.of("valid_part")));
    assertTrue(
        ex1.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_TRACK_TARGET_PART")));

    // 2. Blank cue id and cue time exceeding duration
    AnimationSpec specCueErrors = new AnimationSpec();
    specCueErrors.setDuration("1s");
    TimelineCueSpec cue1 = new TimelineCueSpec();
    cue1.setAt("0.5s");
    cue1.setId("");
    TimelineCueSpec cue2 = new TimelineCueSpec();
    cue2.setAt("2s");
    cue2.setId("late");
    specCueErrors.setCues(List.of(cue1, cue2));

    AnimationCompilerException ex2 =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specCueErrors, Set.of()));
    assertTrue(ex2.diagnostics().stream().anyMatch(d -> d.code().equals("CUE_ID_BLANK")));
    assertTrue(
        ex2.diagnostics().stream().anyMatch(d -> d.code().equals("CUE_TIME_EXCEEDS_DURATION")));

    // 3. Invalid keyframe dimension (2 elements instead of 3)
    AnimationSpec specBadKf = new AnimationSpec();
    specBadKf.setDuration("1s");
    KeyframeSpec badDimKf = new KeyframeSpec();
    badDimKf.setAt("0s");
    badDimKf.setValue(List.of(1.0f, 2.0f)); // Only 2 values
    AnimationTrackSpec badTrack = new AnimationTrackSpec();
    badTrack.setTranslation(List.of(badDimKf));
    specBadKf.setTracks(Map.of("root", badTrack));

    AnimationCompilerException ex3 =
        assertThrows(
            AnimationCompilerException.class, () -> compiler.compile(animId, specBadKf, Set.of()));
    assertTrue(ex3.diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_VECTOR3")));
  }

  @Test
  void testScaleTrackWithNonLinearEasingSubSteps() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("eased_scale");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");

    KeyframeSpec kf1 = new KeyframeSpec();
    kf1.setAt("0s");
    kf1.setValue(List.of(1.0f, 1.0f, 1.0f));
    kf1.setEasing("EASE_IN_QUAD");

    KeyframeSpec kf2 = new KeyframeSpec();
    kf2.setAt("1s");
    kf2.setValue(List.of(2.0f, 2.0f, 2.0f));

    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setScale(List.of(kf1, kf2));
    spec.setTracks(Map.of("root", trackSpec));

    CompiledAnimation compiled = compiler.compile(animId, spec, Set.of("part1"));
    assertEquals(1, compiled.tracks().size());
    CompiledTrack track = compiled.tracks().get(0);
    assertEquals(20, track.segments().size());
  }

  @Test
  void testAdditionalValidationDiagnostics() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("extra_diagnostics");

    // Invalid rotation euler / quaternion formats
    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");

    KeyframeSpec badEuler = new KeyframeSpec();
    badEuler.setAt("0s");
    badEuler.setEuler(List.of(10.0f, 20.0f)); // Needs 3
    badEuler.setRotationMode("BOGUS_ROT_MODE");
    badEuler.setEasing("BOGUS_EASING");

    KeyframeSpec badQuat = new KeyframeSpec();
    badQuat.setAt("1s");
    badQuat.setQuaternion(List.of(1.0f, 2.0f, 3.0f)); // Needs 4

    AnimationTrackSpec track = new AnimationTrackSpec();
    track.setRotation(List.of(badEuler, badQuat));
    spec.setTracks(Map.of("part1", track));

    AnimationCompilerException ex =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, spec, Set.of("part1")));

    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_EULER")));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_ROTATION_MODE")));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_EASING")));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_QUATERNION")));
  }

  @Test
  void testInvalidTimeFormatDiagnostic() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("bad_time");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("invalid-duration");

    AnimationCompilerException ex =
        assertThrows(
            AnimationCompilerException.class, () -> compiler.compile(animId, spec, Set.of()));

    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_TIME_SYNTAX")));
  }

  @Test
  void testDurationUnitsAndLimits() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("units");

    // 1. Milliseconds unit
    AnimationSpec specMs = new AnimationSpec();
    specMs.setDuration("500ms");
    CompiledAnimation animMs = compiler.compile(animId, specMs, Set.of());
    assertEquals(500_000_000L, animMs.definition().duration().time().nanoseconds());

    // 2. Ticks unit (20t = 1s = 1_000_000_000ns)
    AnimationSpec specTicks = new AnimationSpec();
    specTicks.setDuration("20t");
    CompiledAnimation animTicks = compiler.compile(animId, specTicks, Set.of());
    assertEquals(1_000_000_000L, animTicks.definition().duration().time().nanoseconds());

    // 3. Bare seconds number without 's'
    AnimationSpec specBare = new AnimationSpec();
    specBare.setDuration("2");
    CompiledAnimation animBare = compiler.compile(animId, specBare, Set.of());
    assertEquals(2_000_000_000L, animBare.definition().duration().time().nanoseconds());

    // 4. Seconds unit
    AnimationSpec specSec = new AnimationSpec();
    specSec.setDuration("2s");
    CompiledAnimation animSec = compiler.compile(animId, specSec, Set.of());
    assertEquals(2_000_000_000L, animSec.definition().duration().time().nanoseconds());

    // 5. Exceeding max limit (3601s > 1h)
    AnimationSpec specExceed = new AnimationSpec();
    specExceed.setDuration("3601s");
    AnimationCompilerException exExceed =
        assertThrows(
            AnimationCompilerException.class, () -> compiler.compile(animId, specExceed, Set.of()));
    assertTrue(
        exExceed.diagnostics().stream()
            .anyMatch(d -> d.code().equals("ANIMATION_DURATION_EXCEEDS_MAX")));

    // 6. Null and blank duration
    AnimationSpec specNull = new AnimationSpec();
    specNull.setDuration(null);
    AnimationCompilerException exNull =
        assertThrows(
            AnimationCompilerException.class, () -> compiler.compile(animId, specNull, Set.of()));
    assertTrue(
        exNull.diagnostics().stream().anyMatch(d -> d.code().equals("ANIMATION_DURATION_BLANK")));

    AnimationSpec specBlank = new AnimationSpec();
    specBlank.setDuration("   ");
    AnimationCompilerException exBlank =
        assertThrows(
            AnimationCompilerException.class, () -> compiler.compile(animId, specBlank, Set.of()));
    assertTrue(
        exBlank.diagnostics().stream().anyMatch(d -> d.code().equals("ANIMATION_DURATION_BLANK")));
  }

  @Test
  void testLoopModesAndRecoveryPolicies() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("loops_recovery");

    // All valid loop modes and recovery policies
    for (String loop : List.of("ONCE", "LOOP", "PING_PONG")) {
      for (String rec : List.of("RESTART", "RESUME", "STOP")) {
        AnimationSpec spec = new AnimationSpec();
        spec.setDuration("1s");
        spec.setLoop(loop);
        spec.setRecovery(rec);
        spec.setLoopCount(3);
        CompiledAnimation compiled = compiler.compile(animId, spec, Set.of());
        assertEquals(LoopMode.valueOf(loop), compiled.definition().loopMode());
        assertEquals(3, compiled.definition().loopCount());
        assertEquals(RecoveryPolicy.valueOf(rec), compiled.definition().recoveryPolicy());
      }
    }

    // Default loopCount for ONCE is 1, for LOOP is 0
    AnimationSpec specOnce = new AnimationSpec();
    specOnce.setDuration("1s");
    specOnce.setLoop("ONCE");
    assertEquals(1, compiler.compile(animId, specOnce, Set.of()).definition().loopCount());

    AnimationSpec specLoop = new AnimationSpec();
    specLoop.setDuration("1s");
    specLoop.setLoop("LOOP");
    assertEquals(0, compiler.compile(animId, specLoop, Set.of()).definition().loopCount());

    // Null and blank defaults
    AnimationSpec specDefault = new AnimationSpec();
    specDefault.setDuration("1s");
    CompiledAnimation defCompiled = compiler.compile(animId, specDefault, Set.of());
    assertEquals(LoopMode.ONCE, defCompiled.definition().loopMode());
    assertEquals(RecoveryPolicy.RESUME, defCompiled.definition().recoveryPolicy());

    // Unknown loop mode and unknown recovery policy diagnostics
    AnimationSpec specBad = new AnimationSpec();
    specBad.setDuration("1s");
    specBad.setLoop("BOGUS_LOOP");
    specBad.setRecovery("BOGUS_RECOVERY");
    AnimationCompilerException ex =
        assertThrows(
            AnimationCompilerException.class, () -> compiler.compile(animId, specBad, Set.of()));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_LOOP_MODE")));
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("UNKNOWN_RECOVERY_POLICY")));
  }

  @Test
  void testKeyframeStartInterpolationAndErrors() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("synthetic_start");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");

    KeyframeSpec kf1 = new KeyframeSpec();
    kf1.setAt("0s");
    kf1.setValue(List.of(0.0f, 0.0f, 0.0f));

    KeyframeSpec kf2 = new KeyframeSpec();
    kf2.setAt("0.5s");
    kf2.setValue(List.of(0.0f, 5.0f, 0.0f));

    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setTranslation(List.of(kf1, kf2));
    spec.setTracks(Map.of("root", trackSpec));

    CompiledAnimation compiled = compiler.compile(animId, spec, Set.of());
    CompiledTrack track = compiled.tracks().get(0);
    assertTrue(track.segments().size() >= 1);
    assertEquals(AnimationTime.ZERO, track.segments().get(0).startTime());

    // Continuous rotation with quaternion (covering slerp sub-step branch)
    AnimationSpec specQuatCont = new AnimationSpec();
    specQuatCont.setDuration("1s");
    KeyframeSpec q1 = new KeyframeSpec();
    q1.setAt("0s");
    q1.setQuaternion(List.of(0.0f, 0.0f, 0.0f, 1.0f));
    q1.setRotationMode("CONTINUOUS");
    KeyframeSpec q2 = new KeyframeSpec();
    q2.setAt("1s");
    q2.setQuaternion(List.of(0.0f, 1.0f, 0.0f, 0.0f));
    q2.setRotationMode("CONTINUOUS");
    AnimationTrackSpec quatTrack = new AnimationTrackSpec();
    quatTrack.setRotation(List.of(q1, q2));
    specQuatCont.setTracks(Map.of("root", quatTrack));
    CompiledAnimation compiledQuat = compiler.compile(animId, specQuatCont, Set.of());
    assertEquals(1, compiledQuat.tracks().size());

    // Duplicate keyframe times on track
    AnimationSpec specDup = new AnimationSpec();
    specDup.setDuration("1s");
    KeyframeSpec dup1 = new KeyframeSpec();
    dup1.setAt("0.5s");
    dup1.setValue(List.of(0.0f, 0.0f, 0.0f));
    KeyframeSpec dup2 = new KeyframeSpec();
    dup2.setAt("0.5s");
    dup2.setValue(List.of(1.0f, 1.0f, 1.0f));
    AnimationTrackSpec dupTrack = new AnimationTrackSpec();
    dupTrack.setTranslation(List.of(dup1, dup2));
    specDup.setTracks(Map.of("root", dupTrack));
    AnimationCompilerException exDup =
        assertThrows(
            AnimationCompilerException.class, () -> compiler.compile(animId, specDup, Set.of()));
    assertTrue(
        exDup.diagnostics().stream().anyMatch(d -> d.code().equals("DUPLICATE_KEYFRAME_TIME")));

    // Blank track target and null track spec
    AnimationSpec specTrackErrors = new AnimationSpec();
    specTrackErrors.setDuration("1s");
    Map<String, AnimationTrackSpec> tracksMap = new HashMap<>();
    tracksMap.put("", new AnimationTrackSpec());
    tracksMap.put("root", null);
    specTrackErrors.setTracks(tracksMap);

    AnimationCompilerException exTrack =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specTrackErrors, Set.of()));
    assertTrue(exTrack.diagnostics().stream().anyMatch(d -> d.code().equals("TRACK_TARGET_BLANK")));
  }

  @Test
  void testCompiledTrackEvaluationCornerCases() {
    // 1. Empty segments list
    CompiledTrack emptyTrack =
        new CompiledTrack(AnimationTarget.ROOT, AnimationTrackType.TRANSLATION, List.of());
    assertEquals(AnimationTarget.ROOT, emptyTrack.target());
    assertEquals(AnimationTrackType.TRANSLATION, emptyTrack.trackType());
    assertTrue(emptyTrack.segments().isEmpty());

    ModelTransform base = ModelTransform.IDENTITY;
    assertSame(base, emptyTrack.evaluate(AnimationTime.fromSeconds(1.0), base));

    // 2. Playhead before first segment startTime
    CompiledSegment seg1 =
        new CompiledSegment(
            AnimationTime.fromSeconds(1.0),
            AnimationTime.fromSeconds(2.0),
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            20);
    CompiledTrack track =
        new CompiledTrack(AnimationTarget.ROOT, AnimationTrackType.TRANSLATION, List.of(seg1));
    assertEquals(ModelTransform.IDENTITY, track.evaluate(AnimationTime.fromSeconds(0.5), base));

    // 3. Segment with totalNanos <= 0
    CompiledSegment instantSeg =
        new CompiledSegment(
            AnimationTime.fromSeconds(1.0),
            AnimationTime.fromSeconds(1.0),
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            0);
    CompiledTrack instantTrack =
        new CompiledTrack(
            AnimationTarget.ROOT, AnimationTrackType.TRANSLATION, List.of(instantSeg));
    assertNotNull(instantTrack.evaluate(AnimationTime.fromSeconds(1.0), base));

    // 4. Binary search earlier and later branches with 3 segments
    CompiledSegment segA =
        new CompiledSegment(
            AnimationTime.fromSeconds(0.0),
            AnimationTime.fromSeconds(1.0),
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            20);
    CompiledSegment segB =
        new CompiledSegment(
            AnimationTime.fromSeconds(1.0),
            AnimationTime.fromSeconds(2.0),
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            20);
    CompiledSegment segC =
        new CompiledSegment(
            AnimationTime.fromSeconds(2.0),
            AnimationTime.fromSeconds(3.0),
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            20);
    CompiledTrack threeTrack =
        new CompiledTrack(
            AnimationTarget.ROOT, AnimationTrackType.TRANSLATION, List.of(segA, segB, segC));

    assertNotNull(threeTrack.evaluate(AnimationTime.fromSeconds(0.5), base));
    assertNotNull(threeTrack.evaluate(AnimationTime.fromSeconds(2.5), base));
    assertNotNull(threeTrack.evaluate(AnimationTime.fromSeconds(4.0), base)); // Past last segment
  }

  @Test
  void testCompiledSegmentValidationAndComparison() {
    AnimationTime t0 = AnimationTime.fromSeconds(0.0);
    AnimationTime t1 = AnimationTime.fromSeconds(1.0);
    AnimationTime t2 = AnimationTime.fromSeconds(2.0);

    CompiledSegment seg1 =
        new CompiledSegment(t0, t1, ModelTransform.IDENTITY, ModelTransform.IDENTITY, 20);
    CompiledSegment seg2 =
        new CompiledSegment(t1, t2, ModelTransform.IDENTITY, ModelTransform.IDENTITY, 20);

    assertTrue(seg1.compareTo(seg2) < 0);
    assertTrue(seg2.compareTo(seg1) > 0);

    assertThrows(
        IllegalArgumentException.class,
        () -> new CompiledSegment(t0, t1, ModelTransform.IDENTITY, ModelTransform.IDENTITY, -1));
  }

  @Test
  void testCompiledAnimationPoseEvaluationHierarchyAndCycles() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("cycle_test");

    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");
    KeyframeSpec kf1 = new KeyframeSpec();
    kf1.setAt("0s");
    kf1.setValue(List.of(0.0f, 0.0f, 0.0f));
    KeyframeSpec kf2 = new KeyframeSpec();
    kf2.setAt("1s");
    kf2.setValue(List.of(1.0f, 1.0f, 1.0f));
    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setTranslation(List.of(kf1, kf2));
    spec.setTracks(Map.of("root", trackSpec));

    CompiledAnimation compiled = compiler.compile(animId, spec, Set.of());
    assertTrue(compiled.boundaryScheduleTimes().contains(AnimationTime.ZERO));

    // Test model with cycle: A -> B -> A
    io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef asset =
        io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef.of("minecraft:stone");
    ModelPartDefinition partA =
        new ModelPartDefinition(
            new ModelPartId("partA"),
            new ModelPartId("partB"),
            io.github.kizio806.spectraevents.core.visual.model.ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties.DEFAULT,
            asset);
    ModelPartDefinition partB =
        new ModelPartDefinition(
            new ModelPartId("partB"),
            new ModelPartId("partA"),
            io.github.kizio806.spectraevents.core.visual.model.ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties.DEFAULT,
            asset);
    ModelDefinition cyclicModel =
        new ModelDefinition(
            new io.github.kizio806.spectraevents.core.visual.model.ModelId("cyclic"),
            List.of(partA, partB),
            List.of());

    IllegalArgumentException exCycle =
        assertThrows(
            IllegalArgumentException.class,
            () -> compiled.evaluatePose(AnimationTime.ZERO, cyclicModel));
    assertTrue(exCycle.getMessage().contains("Model hierarchy contains a cycle"));

    // Test model referencing missing parent
    ModelPartDefinition orphan =
        new ModelPartDefinition(
            new ModelPartId("orphan"),
            new ModelPartId("non_existent_parent"),
            io.github.kizio806.spectraevents.core.visual.model.ModelPartType.ITEM_DISPLAY,
            ModelTransform.IDENTITY,
            ModelTransform.IDENTITY,
            io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties.DEFAULT,
            asset);
    ModelDefinition missingParentModel =
        new ModelDefinition(
            new io.github.kizio806.spectraevents.core.visual.model.ModelId("missing"),
            List.of(orphan),
            List.of());

    IllegalArgumentException exMissing =
        assertThrows(
            IllegalArgumentException.class,
            () -> compiled.evaluatePose(AnimationTime.ZERO, missingParentModel));
    assertTrue(exMissing.getMessage().contains("references missing parent"));
  }

  @Test
  void testCompileAnimationEdgeCasesAndValidationDiagnostics() {
    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animId = new AnimationId("edge_anim");

    // 1. Invalid track target (blank)
    AnimationSpec specInvalidTarget = new AnimationSpec();
    specInvalidTarget.setDuration("1s");
    specInvalidTarget.setTracks(Map.of("   ", new AnimationTrackSpec()));
    AnimationCompilerException exTarget =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specInvalidTarget, Set.of("part1")));
    assertTrue(
        exTarget.diagnostics().stream().anyMatch(d -> "TRACK_TARGET_BLANK".equals(d.code())));

    // 2. Invalid cue ID (blank)
    AnimationSpec specInvalidCue = new AnimationSpec();
    specInvalidCue.setDuration("1s");
    TimelineCueSpec blankCue = new TimelineCueSpec();
    blankCue.setAt("500ms");
    blankCue.setId("   ");
    specInvalidCue.setCues(List.of(blankCue));
    AnimationCompilerException exCue =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specInvalidCue, Set.of("part1")));
    assertTrue(exCue.diagnostics().stream().anyMatch(d -> "CUE_ID_BLANK".equals(d.code())));

    // 3. Rotation keyframe with identity default (neither euler nor quaternion), continuous
    // rotation & ease_in_quad easing
    AnimationSpec specRot = new AnimationSpec();
    specRot.setDuration("2s");
    AnimationTrackSpec trackRot = new AnimationTrackSpec();
    KeyframeSpec kfRot1 = new KeyframeSpec();
    kfRot1.setAt("0s");
    kfRot1.setRotationMode("CONTINUOUS");
    kfRot1.setEasing("EASE_IN_QUAD");
    KeyframeSpec kfRot2 = new KeyframeSpec();
    kfRot2.setAt("1s");
    kfRot2.setRotationMode("CONTINUOUS");
    kfRot2.setEasing("EASE_IN_QUAD");
    trackRot.setRotation(List.of(kfRot1, kfRot2));
    specRot.setTracks(Map.of("part1", trackRot));
    CompiledAnimation compiledRot = compiler.compile(animId, specRot, Set.of("part1"));
    assertNotNull(compiledRot);
    assertEquals(1, compiledRot.tracks().size());

    // 4. Scale keyframes: exceeding duration and duplicate time
    AnimationSpec specScaleErrors = new AnimationSpec();
    specScaleErrors.setDuration("1s");
    AnimationTrackSpec trackScale = new AnimationTrackSpec();
    KeyframeSpec kfScale1 = new KeyframeSpec();
    kfScale1.setAt("500ms");
    kfScale1.setValue(List.of(1.0f, 1.0f, 1.0f));
    KeyframeSpec kfScale2 = new KeyframeSpec();
    kfScale2.setAt("500ms");
    kfScale2.setValue(List.of(2.0f, 2.0f, 2.0f));
    KeyframeSpec kfScale3 = new KeyframeSpec();
    kfScale3.setAt("2s");
    kfScale3.setValue(List.of(3.0f, 3.0f, 3.0f));
    trackScale.setScale(List.of(kfScale1, kfScale2, kfScale3));
    specScaleErrors.setTracks(Map.of("part1", trackScale));
    AnimationCompilerException exScale =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specScaleErrors, Set.of("part1")));
    assertTrue(
        exScale.diagnostics().stream().anyMatch(d -> "DUPLICATE_KEYFRAME_TIME".equals(d.code())));
    assertTrue(
        exScale.diagnostics().stream().anyMatch(d -> "KEYFRAME_EXCEEDS_DURATION".equals(d.code())));

    // 5. Non-linear scale and empty tracks
    AnimationSpec specScaleOk = new AnimationSpec();
    specScaleOk.setDuration("1s");
    AnimationTrackSpec trackScaleOk = new AnimationTrackSpec();
    KeyframeSpec kfS1 = new KeyframeSpec();
    kfS1.setAt("0s");
    kfS1.setEasing("EASE_OUT_QUAD");
    kfS1.setValue(List.of(1.0f, 1.0f, 1.0f));
    KeyframeSpec kfS2 = new KeyframeSpec();
    kfS2.setAt("1s");
    kfS2.setEasing("EASE_OUT_QUAD");
    kfS2.setValue(List.of(2.0f, 2.0f, 2.0f));
    trackScaleOk.setScale(List.of(kfS1, kfS2));
    trackScaleOk.setTranslation(List.of());
    trackScaleOk.setRotation(List.of());
    specScaleOk.setTracks(Map.of("part1", trackScaleOk));
    CompiledAnimation compiledScale = compiler.compile(animId, specScaleOk, Set.of("part1"));
    assertNotNull(compiledScale);

    // 6. Unknown easing validation diagnostic
    AnimationSpec specBadEasing = new AnimationSpec();
    specBadEasing.setDuration("1s");
    AnimationTrackSpec trackBadEasing = new AnimationTrackSpec();
    KeyframeSpec kfBad = new KeyframeSpec();
    kfBad.setAt("0s");
    kfBad.setEasing("UNKNOWN_EASING_FUNC");
    kfBad.setValue(List.of(1.0f, 1.0f, 1.0f));
    trackBadEasing.setTranslation(List.of(kfBad));
    specBadEasing.setTracks(Map.of("part1", trackBadEasing));
    AnimationCompilerException exBadEasing =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specBadEasing, Set.of("part1")));
    assertTrue(exBadEasing.diagnostics().stream().anyMatch(d -> "UNKNOWN_EASING".equals(d.code())));

    // 7. Track segments budget exceeded
    AnimationSpec specTooManySegments = new AnimationSpec();
    specTooManySegments.setDuration("52s");
    AnimationTrackSpec trackTooMany = new AnimationTrackSpec();
    KeyframeSpec kfSeg1 = new KeyframeSpec();
    kfSeg1.setAt("0s");
    kfSeg1.setEasing("EASE_IN_QUAD");
    kfSeg1.setValue(List.of(0.0f, 0.0f, 0.0f));
    KeyframeSpec kfSeg2 = new KeyframeSpec();
    kfSeg2.setAt("52s");
    kfSeg2.setEasing("EASE_IN_QUAD");
    kfSeg2.setValue(List.of(1.0f, 1.0f, 1.0f));
    trackTooMany.setTranslation(List.of(kfSeg1, kfSeg2));
    specTooManySegments.setTracks(Map.of("part1", trackTooMany));
    AnimationCompilerException exSegs =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specTooManySegments, Set.of("part1")));
    assertTrue(
        exSegs.diagnostics().stream()
            .anyMatch(d -> "TRACK_SEGMENTS_EXCEED_BUDGET".equals(d.code())));

    // 8. Total animation segments budget exceeded
    AnimationSpec specTotalTooMany = new AnimationSpec();
    specTotalTooMany.setDuration("48s");
    Map<String, AnimationTrackSpec> manyTracks = new HashMap<>();
    Set<String> manyParts = new java.util.HashSet<>();
    for (int i = 0; i < 11; i++) {
      String p = "part_" + i;
      manyParts.add(p);
      AnimationTrackSpec t = new AnimationTrackSpec();
      KeyframeSpec s1 = new KeyframeSpec();
      s1.setAt("0s");
      s1.setEasing("EASE_IN_QUAD");
      s1.setValue(List.of(0.0f, 0.0f, 0.0f));
      KeyframeSpec s2 = new KeyframeSpec();
      s2.setAt("48s");
      s2.setEasing("EASE_IN_QUAD");
      s2.setValue(List.of(1.0f, 1.0f, 1.0f));
      t.setTranslation(List.of(s1, s2));
      manyTracks.put(p, t);
    }
    specTotalTooMany.setTracks(manyTracks);
    AnimationCompilerException exTotalSegs =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specTotalTooMany, manyParts));
    assertTrue(
        exTotalSegs.diagnostics().stream()
            .anyMatch(d -> "ANIMATION_SEGMENTS_EXCEED_BUDGET".equals(d.code())));

    // 9. Cue validation (blank ID and time exceeding duration)
    AnimationSpec specBlankCue = new AnimationSpec();
    specBlankCue.setDuration("1s");
    TimelineCueSpec blankIdCue = new TimelineCueSpec();
    blankIdCue.setAt("0.5s");
    blankIdCue.setId("   ");
    TimelineCueSpec exceedCue = new TimelineCueSpec();
    exceedCue.setAt("2s");
    exceedCue.setId("late-cue");
    specBlankCue.setCues(List.of(blankIdCue, exceedCue));
    AnimationCompilerException exBlankIdCue =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specBlankCue, Set.of()));
    assertTrue(exBlankIdCue.diagnostics().stream().anyMatch(d -> "CUE_ID_BLANK".equals(d.code())));
    assertTrue(
        exBlankIdCue.diagnostics().stream()
            .anyMatch(d -> "CUE_TIME_EXCEEDS_DURATION".equals(d.code())));

    // 10. Rotation keyframe exceeding duration and duplicate time
    AnimationSpec specRotErrors = new AnimationSpec();
    specRotErrors.setDuration("1s");
    AnimationTrackSpec rotTrack = new AnimationTrackSpec();
    KeyframeSpec r1 = new KeyframeSpec();
    r1.setAt("0.5s");
    r1.setEuler(List.of(0.0f, 0.0f, 0.0f));
    KeyframeSpec r2 = new KeyframeSpec();
    r2.setAt("0.5s"); // duplicate
    r2.setEuler(List.of(10.0f, 0.0f, 0.0f));
    KeyframeSpec r3 = new KeyframeSpec();
    r3.setAt("2s"); // exceeds duration
    r3.setEuler(List.of(20.0f, 0.0f, 0.0f));
    rotTrack.setRotation(List.of(r1, r2, r3));
    specRotErrors.setTracks(Map.of("part1", rotTrack));
    AnimationCompilerException exRot =
        assertThrows(
            AnimationCompilerException.class,
            () -> compiler.compile(animId, specRotErrors, Set.of("part1")));
    assertTrue(
        exRot.diagnostics().stream().anyMatch(d -> "DUPLICATE_KEYFRAME_TIME".equals(d.code())));
    assertTrue(
        exRot.diagnostics().stream().anyMatch(d -> "KEYFRAME_EXCEEDS_DURATION".equals(d.code())));

    // 11. Rotation keyframe with neither euler nor quaternion defaults to IDENTITY
    AnimationSpec specRotDefault = new AnimationSpec();
    specRotDefault.setDuration("1s");
    AnimationTrackSpec rotTrackDef = new AnimationTrackSpec();
    KeyframeSpec rd1 = new KeyframeSpec();
    rd1.setAt("0s");
    KeyframeSpec rd2 = new KeyframeSpec();
    rd2.setAt("1s");
    rotTrackDef.setRotation(List.of(rd1, rd2));
    specRotDefault.setTracks(Map.of("part1", rotTrackDef));
    CompiledAnimation compRotDef = compiler.compile(animId, specRotDefault, Set.of("part1"));
    assertNotNull(compRotDef);

    // 12. Continuous rotation mode with linear and non-linear easing
    AnimationSpec specRotCont = new AnimationSpec();
    specRotCont.setDuration("1s");
    AnimationTrackSpec rotTrackCont = new AnimationTrackSpec();
    KeyframeSpec rc1 = new KeyframeSpec();
    rc1.setAt("0s");
    rc1.setEuler(List.of(0.0f, 0.0f, 0.0f));
    rc1.setRotationMode("CONTINUOUS");
    rc1.setEasing("LINEAR");
    KeyframeSpec rc2 = new KeyframeSpec();
    rc2.setAt("0.5s");
    rc2.setEuler(List.of(0.0f, 180.0f, 0.0f));
    rc2.setRotationMode("CONTINUOUS");
    rc2.setEasing("EASE_IN_QUAD");
    KeyframeSpec rc3 = new KeyframeSpec();
    rc3.setAt("1s");
    rc3.setEuler(List.of(0.0f, 360.0f, 0.0f));
    rc3.setRotationMode("CONTINUOUS");
    rotTrackCont.setRotation(List.of(rc1, rc2, rc3));
    specRotCont.setTracks(Map.of("part1", rotTrackCont));
    CompiledAnimation compRotCont = compiler.compile(animId, specRotCont, Set.of("part1"));
    assertNotNull(compRotCont);

    // 13. Empty keyframes defensive handling in compileTranslationTrack, compileRotationTrack,
    // compileScaleTrack
    try {
      var mTrans =
          AnimationCompiler.class.getDeclaredMethod(
              "compileTranslationTrack", AnimationTarget.class, List.class);
      mTrans.setAccessible(true);
      CompiledTrack emptyTrans =
          (CompiledTrack) mTrans.invoke(compiler, AnimationTarget.ROOT, List.of());
      assertTrue(emptyTrans.segments().isEmpty());

      var mRot =
          AnimationCompiler.class.getDeclaredMethod(
              "compileRotationTrack", AnimationTarget.class, List.class);
      mRot.setAccessible(true);
      CompiledTrack emptyRot =
          (CompiledTrack) mRot.invoke(compiler, AnimationTarget.ROOT, List.of());
      assertTrue(emptyRot.segments().isEmpty());

      var mScale =
          AnimationCompiler.class.getDeclaredMethod(
              "compileScaleTrack", AnimationTarget.class, List.class);
      mScale.setAccessible(true);
      CompiledTrack emptyScale =
          (CompiledTrack) mScale.invoke(compiler, AnimationTarget.ROOT, List.of());
      assertTrue(emptyScale.segments().isEmpty());
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
