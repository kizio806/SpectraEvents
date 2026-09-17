package io.github.kizio806.spectraevents.application.model.animation.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
