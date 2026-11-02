package io.github.kizio806.spectraevents.application.model.animation.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.animation.compiler.AnimationCompiler;
import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation;
import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledTrack;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.TimelineCueSpec;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedPartHandle;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationCueReached;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActiveAnimationTest {

  private RenderedModelHandle handle;
  private CompiledAnimation compiledAnim;

  @BeforeEach
  void setUp() {
    handle =
        new RenderedModelHandle(
            ModelRuntimeId.of("run_active"),
            new ModelId("test_model"),
            null,
            new ModelAnchor("world", 0, 64, 0, 0, 0),
            Map.of(
                ModelPartId.of("root"),
                new RenderedPartHandle(ModelPartId.of("root"), UUID.randomUUID())),
            Map.of());

    AnimationCompiler compiler = new AnimationCompiler();
    AnimationSpec spec = new AnimationSpec();
    spec.setDuration("1s");

    KeyframeSpec kf1 = new KeyframeSpec();
    kf1.setAt("0s");
    kf1.setValue(List.of(0.0f, 0.0f, 0.0f));

    KeyframeSpec kf2 = new KeyframeSpec();
    kf2.setAt("1s");
    kf2.setValue(List.of(0.0f, 10.0f, 0.0f));

    AnimationTrackSpec trackSpec = new AnimationTrackSpec();
    trackSpec.setTranslation(List.of(kf1, kf2));
    spec.setTracks(Map.of("root", trackSpec));

    TimelineCueSpec cue = new TimelineCueSpec();
    cue.setAt("500ms");
    cue.setId("step_cue");
    spec.setCues(List.of(cue));

    compiledAnim = compiler.compile(new AnimationId("run_anim"), spec, Set.of("root"));
  }

  @Test
  void testConstructorValidationsAndDefaults() {
    assertThrows(
        NullPointerException.class,
        () -> new ActiveAnimation(null, handle, compiledAnim, 1.0f, null, -1, null, null));
    assertThrows(
        NullPointerException.class,
        () ->
            new ActiveAnimation(
                AnimationPlaybackId.random(), null, compiledAnim, 1.0f, null, -1, null, null));
    assertThrows(
        NullPointerException.class,
        () ->
            new ActiveAnimation(
                AnimationPlaybackId.random(), handle, null, 1.0f, null, -1, null, null));

    ActiveAnimation anim =
        new ActiveAnimation(
            AnimationPlaybackId.random(),
            handle,
            compiledAnim,
            -1.0f, // fallback to 1.0f
            null, // fallback to loop mode from definition
            -1, // fallback to loop count
            null, // fallback to recovery policy
            null);

    assertEquals(1.0f, anim.speed());
    assertEquals(compiledAnim.definition().loopMode(), anim.loopMode());
    assertEquals(compiledAnim.definition().recoveryPolicy(), anim.recoveryPolicy());
    assertEquals(PlaybackState.PLAYING, anim.state());
    assertEquals(AnimationTime.ZERO, anim.currentTime());
    assertEquals(0, anim.currentLoop());
  }

  @Test
  void testSpeedAndStateModifications() {
    ActiveAnimation anim =
        new ActiveAnimation(
            AnimationPlaybackId.random(),
            handle,
            compiledAnim,
            1.5f,
            LoopMode.ONCE,
            1,
            RecoveryPolicy.RESUME,
            null);

    assertEquals(1.5f, anim.speed());
    anim.setSpeed(2.0f);
    assertEquals(2.0f, anim.speed());

    assertThrows(IllegalArgumentException.class, () -> anim.setSpeed(0.0f));
    assertThrows(IllegalArgumentException.class, () -> anim.setSpeed(-1.0f));

    anim.setState(PlaybackState.PAUSED);
    assertEquals(PlaybackState.PAUSED, anim.state());
  }

  @Test
  void testSeekAndLooping() {
    List<AnimationCueReached> cues = new ArrayList<>();
    ActiveAnimation anim =
        new ActiveAnimation(
            AnimationPlaybackId.random(),
            handle,
            compiledAnim,
            1.0f,
            LoopMode.LOOP,
            2, // 2 loops max
            RecoveryPolicy.STOP,
            cues::add);

    // SeekTo within bounds
    anim.seekTo(AnimationTime.fromMillis(600));
    assertEquals(AnimationTime.fromMillis(600), anim.currentTime());

    // setCurrentTime
    anim.setCurrentTime(AnimationTime.fromMillis(400));
    assertEquals(AnimationTime.fromMillis(400), anim.currentTime());
    assertTrue(cues.isEmpty());
  }

  @Test
  void testAdvanceTimeWithLoopModeAndMaxLoops() {
    List<AnimationCueReached> cues = new ArrayList<>();
    ActiveAnimation anim =
        new ActiveAnimation(
            AnimationPlaybackId.random(),
            handle,
            compiledAnim,
            1.0f,
            LoopMode.LOOP,
            2, // 2 max loops
            RecoveryPolicy.STOP,
            cues::add);

    // Advance 600ms (triggers 500ms cue)
    boolean completed = anim.advanceTime(600_000_000L);
    assertFalse(completed);
    assertEquals(1, cues.size());
    assertEquals(0, anim.currentLoop());

    // Advance 500ms more (total 1100ms -> loops once, currentLoop becomes 1, overflow 100ms)
    completed = anim.advanceTime(500_000_000L);
    assertFalse(completed);
    assertEquals(1, anim.currentLoop());
    assertEquals(AnimationTime.fromMillis(100), anim.currentTime());

    // Advance 950ms more (total loop 2 reaches 1050ms >= 1000ms, maxLoops 2 reached -> completed)
    completed = anim.advanceTime(950_000_000L);
    assertTrue(completed);
    assertEquals(PlaybackState.COMPLETED, anim.state());
  }

  @Test
  void testTrackSegments() {
    ActiveAnimation anim =
        new ActiveAnimation(
            AnimationPlaybackId.random(),
            handle,
            compiledAnim,
            1.0f,
            LoopMode.ONCE,
            1,
            RecoveryPolicy.STOP,
            null);

    CompiledTrack track = compiledAnim.tracks().get(0);
    assertNotNull(anim.getActiveSegment(track));

    anim.advanceTrackSegment(track);
    assertNull(anim.getActiveSegment(track)); // reached end of single segment
  }
}
