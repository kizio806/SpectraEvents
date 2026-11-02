package io.github.kizio806.spectraevents.application.model.animation.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.TimelineCueSpec;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
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
}
