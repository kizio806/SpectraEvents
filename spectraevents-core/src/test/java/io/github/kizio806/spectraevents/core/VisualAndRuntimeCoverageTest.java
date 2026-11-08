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

  @Test
  void testCoreDomainValueObjectsCoverage() {
    // AnimationTime & AnimationDuration
    AnimationTime t1 = AnimationTime.fromNanos(5_000_000L);
    assertEquals(5L, t1.toMillis());
    assertThrows(IllegalArgumentException.class, () -> AnimationTime.fromSeconds(-1.0));
    assertThrows(IllegalArgumentException.class, () -> AnimationTime.fromSeconds(Double.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> AnimationTime.fromSeconds(Double.POSITIVE_INFINITY));

    AnimationDuration d1 = AnimationDuration.fromMillis(500L);
    assertEquals(500_000_000L, d1.toNanos());
    AnimationDuration d2 = AnimationDuration.fromSeconds(1.5);
    assertEquals(1_500_000_000L, d2.toNanos());

    // AnimationId, ModelId, ModelPartId, InteractionId
    assertThrows(IllegalArgumentException.class, () -> new AnimationId(""));
    assertThrows(IllegalArgumentException.class, () -> new AnimationId("   "));
    assertEquals("walk", new AnimationId("walk").value());

    assertThrows(IllegalArgumentException.class, () -> new ModelId(""));
    assertEquals("boss", new ModelId("boss").toString());

    assertThrows(IllegalArgumentException.class, () -> new ModelPartId(""));
    assertEquals("leg", new ModelPartId("leg").value());

    assertThrows(
        IllegalArgumentException.class,
        () -> new io.github.kizio806.spectraevents.core.visual.model.InteractionId(""));
    assertEquals(
        "hit1",
        io.github.kizio806.spectraevents.core.visual.model.InteractionId.of("hit1").value());

    // BlockAssetRef & ItemAssetRef
    assertThrows(
        IllegalArgumentException.class,
        () -> new io.github.kizio806.spectraevents.core.visual.model.BlockAssetRef(""));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef(
                "", io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode.HEAD));
    assertEquals(
        io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode.NONE,
        io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef.of("stone")
            .transformMode());

    // PhaseId & EventDefinitionId
    assertThrows(IllegalArgumentException.class, () -> new PhaseId(""));
    assertThrows(IllegalArgumentException.class, () -> new PhaseId("INVALID_UPPERCASE"));
    assertThrows(IllegalArgumentException.class, () -> new PhaseId("has space"));
    assertEquals("phase_one", new PhaseId("phase_one").toString());

    assertThrows(IllegalArgumentException.class, () -> new EventDefinitionId(""));
    assertThrows(IllegalArgumentException.class, () -> new EventDefinitionId("has-hyphen"));
    assertThrows(IllegalArgumentException.class, () -> new EventDefinitionId("UPPERCASE"));
    assertEquals("meteor_rain", new EventDefinitionId("meteor_rain").toString());

    // Additional coverage for remaining core edge cases
    assertEquals("run", AnimationId.of("run").value());
    io.github.kizio806.spectraevents.core.visual.animation.AnimationDefinition
        animDefWithNullTracks =
            new io.github.kizio806.spectraevents.core.visual.animation.AnimationDefinition(
                new AnimationId("idle"),
                AnimationDuration.fromSeconds(1.0),
                io.github.kizio806.spectraevents.core.visual.animation.LoopMode.ONCE,
                1,
                null,
                null,
                io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy.STOP);
    assertTrue(animDefWithNullTracks.tracks().isEmpty());
    assertTrue(animDefWithNullTracks.cues().isEmpty());

    io.github.kizio806.spectraevents.core.gameplay.health.HealthThresholds ht =
        new io.github.kizio806.spectraevents.core.gameplay.health.HealthThresholds(
            java.util.List.of(50, 25));
    assertTrue(
        ht.checkCrossed(
                null, new io.github.kizio806.spectraevents.core.gameplay.health.Health(10, 100))
            .isEmpty());
    assertTrue(
        ht.checkCrossed(
                new io.github.kizio806.spectraevents.core.gameplay.health.Health(100, 100), null)
            .isEmpty());

    io.github.kizio806.spectraevents.core.gameplay.contribution.DamageContribution dc =
        io.github.kizio806.spectraevents.core.gameplay.contribution.DamageContribution.empty();
    java.util.UUID p1 = java.util.UUID.randomUUID();
    assertEquals(dc, dc.addDamage(p1, 0));
    assertEquals(dc, dc.addDamage(p1, -5));
    assertTrue(dc.getTopContributors(0).isEmpty());
    assertTrue(dc.getTopContributors(-1).isEmpty());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            io.github.kizio806.spectraevents.core.gameplay.contribution.ContributionRanking.podium(
                dc, java.util.List.of(p1), -1, java.util.Map.of()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions
                .AwardPodiumAction(java.util.List.of()));

    io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions.SpawnMobsAction
        spawnMobsNullWave =
            new io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions
                .SpawnMobsAction(null, java.util.List.of());
    assertEquals("", spawnMobsNullWave.waveId());

    // InvalidPhaseTransitionException with null current and requested phases
    EventInstanceId instId = new EventInstanceId(java.util.UUID.randomUUID());
    EventDefinitionId defId = new EventDefinitionId("meteor");
    InvalidPhaseTransitionException ipte =
        new InvalidPhaseTransitionException(instId, defId, null, null, "not started");
    assertNull(ipte.currentPhase());
    assertNull(ipte.requestedPhase());
    assertTrue(ipte.getMessage().contains("none to none"));

    // ModelPose with null partTransforms
    io.github.kizio806.spectraevents.core.visual.animation.ModelPose poseNullParts =
        io.github.kizio806.spectraevents.core.visual.animation.ModelPose.of(
            io.github.kizio806.spectraevents.core.visual.model.ModelTransform.IDENTITY, null);
    assertTrue(poseNullParts.partTransforms().isEmpty());

    // HitCounter boundary conditions
    assertThrows(
        IllegalArgumentException.class,
        () -> new io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter(-1, 10));
    assertThrows(
        IllegalArgumentException.class,
        () -> new io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter(11, 10));
    assertThrows(
        IllegalArgumentException.class,
        () -> new io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter(0, 0));
    io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter hc =
        io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter.startingAt(10).addHits(5);
    assertEquals(5, hc.current());
    assertEquals(10, hc.addHits(10).current());

    // ModelTransform zero scale and out-of-bounds scales
    io.github.kizio806.spectraevents.core.visual.model.Vector3 zero =
        io.github.kizio806.spectraevents.core.visual.model.Vector3.ZERO;
    io.github.kizio806.spectraevents.core.visual.model.Quaternion qId =
        io.github.kizio806.spectraevents.core.visual.model.Quaternion.IDENTITY;
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.visual.model.ModelTransform(
                zero,
                qId,
                new io.github.kizio806.spectraevents.core.visual.model.Vector3(1f, 0f, 1f),
                zero));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.visual.model.ModelTransform(
                zero,
                qId,
                new io.github.kizio806.spectraevents.core.visual.model.Vector3(1f, 1f, 0f),
                zero));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.visual.model.ModelTransform(
                zero,
                qId,
                new io.github.kizio806.spectraevents.core.visual.model.Vector3(1001f, 1f, 1f),
                zero));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.visual.model.ModelTransform(
                zero,
                qId,
                new io.github.kizio806.spectraevents.core.visual.model.Vector3(1f, 1001f, 1f),
                zero));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.visual.model.ModelTransform(
                zero,
                qId,
                new io.github.kizio806.spectraevents.core.visual.model.Vector3(1f, 1f, 1001f),
                zero));

    // ModelDefinition with null animations map
    io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition samplePart =
        new io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition(
            new ModelPartId("part1"),
            null,
            io.github.kizio806.spectraevents.core.visual.model.ModelPartType.ITEM_DISPLAY,
            io.github.kizio806.spectraevents.core.visual.model.ModelTransform.IDENTITY,
            io.github.kizio806.spectraevents.core.visual.model.ModelTransform.IDENTITY,
            io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties.DEFAULT,
            new io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef(
                "minecraft:diamond",
                io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode.HEAD));
    io.github.kizio806.spectraevents.core.visual.model.ModelDefinition modelDefNullAnims =
        new io.github.kizio806.spectraevents.core.visual.model.ModelDefinition(
            new ModelId("test_model"), java.util.List.of(samplePart), java.util.List.of(), null);
    assertTrue(modelDefNullAnims.animations().isEmpty());

    // CoreActions GroundLootEntry and ApplyCombatDamageAction boundary conditions
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.event.execution.action.CoreActions
                .GroundLootEntry("diamond", 1, -1));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.event.execution.action.CoreActions
                .GroundLootEntry("diamond", 1, 101));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.event.execution.action.CoreActions
                .GroundLootEntry("   ", 1, 50));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.event.execution.action.CoreActions
                .ApplyCombatDamageAction(0, 50, Duration.ofSeconds(1)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.event.execution.action.CoreActions
                .ApplyCombatDamageAction(10, -1, Duration.ofSeconds(1)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.event.execution.action.CoreActions
                .ApplyCombatDamageAction(10, 101, Duration.ofSeconds(1)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.event.execution.action.CoreActions
                .ApplyCombatDamageAction(10, 50, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new io.github.kizio806.spectraevents.core.event.execution.action.CoreActions
                .ApplyCombatDamageAction(10, 50, Duration.ofSeconds(-1)));

    // EventEncounterSettings 3-arg constructor with 0.0 radius sets maxActivePerWorld to 0
    EventEncounterSettings encounterZeroRadius =
        new EventEncounterSettings(0.0d, Duration.ofMinutes(1), 10);
    assertEquals(0, encounterZeroRadius.maxActivePerWorld());
    assertFalse(encounterZeroRadius.reservesZone());
  }
}
