package io.github.kizio806.spectraevents.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.definition.EventEncounterSettings;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.InvalidPhaseTransitionException;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationCueReached;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDuration;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTarget;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTrackType;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.Transform;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class VisualAndRuntimeCoverageTest {

  @Test
  void testTransform() {
    Transform identity = Transform.IDENTITY;
    assertEquals(0f, identity.tx());
    assertEquals(0f, identity.ty());
    assertEquals(0f, identity.tz());
    assertEquals(0f, identity.rx());
    assertEquals(0f, identity.ry());
    assertEquals(0f, identity.rz());
    assertEquals(1f, identity.sx());
    assertEquals(1f, identity.sy());
    assertEquals(1f, identity.sz());

    Transform translated = Transform.translation(1f, 2f, 3f);
    assertEquals(1f, translated.tx());
    assertEquals(2f, translated.ty());
    assertEquals(3f, translated.tz());
    assertEquals(1f, translated.sx());
  }

  @Test
  void testAnimationTrackType() {
    assertEquals(4, AnimationTrackType.values().length);
    assertEquals(AnimationTrackType.TRANSLATION, AnimationTrackType.valueOf("TRANSLATION"));
    assertEquals(AnimationTrackType.ROTATION, AnimationTrackType.valueOf("ROTATION"));
    assertEquals(AnimationTrackType.SCALE, AnimationTrackType.valueOf("SCALE"));
    assertEquals(AnimationTrackType.VISIBILITY, AnimationTrackType.valueOf("VISIBILITY"));
  }

  @Test
  void testAnimationCueReached() {
    AnimationCueReached cue =
        new AnimationCueReached(
            "play1",
            "runtime1",
            new ModelId("model1"),
            new AnimationId("anim1"),
            "cue1",
            AnimationTime.fromMillis(500));

    assertEquals("play1", cue.playbackId());
    assertEquals("runtime1", cue.modelRuntimeId());
    assertEquals("model1", cue.modelId().value());
    assertEquals("anim1", cue.animationId().value());
    assertEquals("cue1", cue.cueId());
    assertEquals(500L, cue.time().toMillis());

    assertThrows(
        NullPointerException.class,
        () ->
            new AnimationCueReached(
                null,
                "runtime1",
                new ModelId("model1"),
                new AnimationId("anim1"),
                "cue1",
                AnimationTime.ZERO));
  }

  @Test
  void testAnimationTarget() {
    AnimationTarget root = AnimationTarget.ROOT;
    assertTrue(root.isRoot());
    assertNull(root.partId());
    assertEquals("root", root.name());

    AnimationTarget parsedRoot = AnimationTarget.parse("root");
    assertEquals(root, parsedRoot);

    AnimationTarget partTarget = AnimationTarget.ofPart(new ModelPartId("sword"));
    assertFalse(partTarget.isRoot());
    assertEquals("sword", partTarget.name());

    AnimationTarget parsedPart = AnimationTarget.parse("sword");
    assertEquals(partTarget, parsedPart);

    assertThrows(
        IllegalArgumentException.class,
        () -> new AnimationTarget(true, new ModelPartId("invalid")));
    assertThrows(IllegalArgumentException.class, () -> new AnimationTarget(false, null));
    assertThrows(NullPointerException.class, () -> AnimationTarget.ofPart(null));
    assertThrows(NullPointerException.class, () -> AnimationTarget.parse(null));
  }

  @Test
  void testAnimationDuration() {
    AnimationTime time = AnimationTime.fromSeconds(2.5);
    AnimationDuration duration = AnimationDuration.of(time);
    assertEquals(time, duration.time());

    assertThrows(NullPointerException.class, () -> AnimationDuration.of(null));
  }

  @Test
  void testEventEncounterSettings() {
    EventEncounterSettings settings =
        new EventEncounterSettings(15.0d, Duration.ofMinutes(5), 10, 2);

    assertEquals(15.0d, settings.zoneRadius());
    assertEquals(Duration.ofMinutes(5), settings.deadline());
    assertEquals(10, settings.minimumContribution());
    assertEquals(2, settings.maxActivePerWorld());
    assertTrue(settings.reservesZone());
    assertTrue(settings.hasDeadline());

    EventEncounterSettings defaultSettings =
        new EventEncounterSettings(10.0d, Duration.ofSeconds(30), 0);
    assertEquals(1, defaultSettings.maxActivePerWorld());

    EventEncounterSettings none = EventEncounterSettings.NONE;
    assertFalse(none.reservesZone());
    assertFalse(none.hasDeadline());

    assertThrows(
        IllegalArgumentException.class,
        () -> new EventEncounterSettings(-1.0d, Duration.ZERO, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EventEncounterSettings(Double.NaN, Duration.ZERO, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EventEncounterSettings(0.0d, Duration.ofSeconds(-1), 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EventEncounterSettings(0.0d, Duration.ZERO, -1, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EventEncounterSettings(0.0d, Duration.ZERO, 0, -1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EventEncounterSettings(5.0d, Duration.ZERO, 0, 0));
  }

  @Test
  void testEventDefinitionId() {
    EventDefinitionId id = new EventDefinitionId("meteor_shower");
    assertEquals("meteor_shower", id.value());

    assertThrows(NullPointerException.class, () -> new EventDefinitionId(null));
    assertThrows(IllegalArgumentException.class, () -> new EventDefinitionId(""));
    assertThrows(IllegalArgumentException.class, () -> new EventDefinitionId("   "));
  }

  @Test
  void testInvalidPhaseTransitionException() {
    EventInstanceId instanceId = EventInstanceId.generate();
    EventDefinitionId defId = new EventDefinitionId("boss_event");
    PhaseId phaseA = new PhaseId("phase_a");
    PhaseId phaseB = new PhaseId("phase_b");

    InvalidPhaseTransitionException ex =
        new InvalidPhaseTransitionException(
            instanceId, defId, phaseA, phaseB, "Transition not allowed");

    assertEquals(instanceId, ex.eventInstanceId());
    assertEquals(defId, ex.definitionId());
    assertEquals(phaseA, ex.currentPhase());
    assertEquals(phaseB, ex.requestedPhase());
    assertEquals("Transition not allowed", ex.reason());
    assertNotNull(ex.getMessage());
  }

  @Test
  void testCoreTriggersValidation() {
    CoreTriggers.WaveClearedTrigger wave = new CoreTriggers.WaveClearedTrigger("wave1");
    assertEquals("wave1", wave.waveId());
    assertEquals("wave_cleared", wave.type());

    assertThrows(IllegalArgumentException.class, () -> new CoreTriggers.WaveClearedTrigger(""));
    assertThrows(IllegalArgumentException.class, () -> new CoreTriggers.WaveClearedTrigger(null));

    CoreTriggers.HealthPercentThresholdCrossedTrigger health =
        new CoreTriggers.HealthPercentThresholdCrossedTrigger(50);
    assertEquals(50, health.percent());
    assertEquals("health_percent_threshold_crossed", health.type());

    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreTriggers.HealthPercentThresholdCrossedTrigger(-1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreTriggers.HealthPercentThresholdCrossedTrigger(101));

    CoreTriggers.HitsPercentThresholdCrossedTrigger hits =
        new CoreTriggers.HitsPercentThresholdCrossedTrigger(75);
    assertEquals(75, hits.percent());
    assertEquals("hits_percent_threshold_crossed", hits.type());

    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreTriggers.HitsPercentThresholdCrossedTrigger(-1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreTriggers.HitsPercentThresholdCrossedTrigger(101));
  }
}
