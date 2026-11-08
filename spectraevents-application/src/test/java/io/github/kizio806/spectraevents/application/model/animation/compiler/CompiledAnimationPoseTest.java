package io.github.kizio806.spectraevents.application.model.animation.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDefinition;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDuration;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTarget;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTrackType;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy;
import io.github.kizio806.spectraevents.core.visual.animation.TimelineCue;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartType;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompiledAnimationPoseTest {

  @Test
  void composesAnimatedParentTransformIntoEveryChildPose() {
    ModelPartId parentId = ModelPartId.of("parent");
    ModelPartId childId = ModelPartId.of("child");
    ModelTransform parentLocal = ModelTransform.of(new Vector3(1.0f, 0.0f, 0.0f));
    ModelTransform childLocal = ModelTransform.of(new Vector3(0.0f, 2.0f, 0.0f));
    ModelDefinition model =
        new ModelDefinition(
            new ModelId("hierarchy"),
            List.of(
                part(parentId, null, parentLocal, parentLocal),
                part(childId, parentId, childLocal, childLocal.compose(parentLocal))),
            List.of());

    AnimationSpec animation = new AnimationSpec();
    animation.setDuration("1s");
    AnimationTrackSpec track = new AnimationTrackSpec();
    track.setTranslation(List.of(keyframe("0s", 1.0f), keyframe("1s", 4.0f)));
    animation.setTracks(Map.of("parent", track));

    AnimationCompiler compiler = new AnimationCompiler();
    AnimationId animationId = new AnimationId("move");
    Set<String> partIds = Set.of("parent", "child");
    CompiledAnimation compiled = compiler.compile(animationId, animation, partIds);

    var pose = compiled.evaluatePose(AnimationTime.fromSeconds(1.0), model);

    assertEquals(4.0f, pose.partTransforms().get(parentId).translation().x());
    assertEquals(4.0f, pose.partTransforms().get(childId).translation().x());
    assertEquals(2.0f, pose.partTransforms().get(childId).translation().y());
  }

  @Test
  void evaluatesRootAndPartTracksAcrossAllChannels() {
    ModelPartId partId = ModelPartId.of("part1");
    ModelTransform local = ModelTransform.IDENTITY;
    ModelDefinition model =
        new ModelDefinition(
            new ModelId("model1"), List.of(part(partId, null, local, local)), List.of());

    TimelineCue cue = new TimelineCue(AnimationTime.fromSeconds(0.5), "sound-effect");
    AnimationDefinition def =
        new AnimationDefinition(
            new AnimationId("full-channels"),
            AnimationDuration.fromSeconds(1.0),
            LoopMode.ONCE,
            1,
            Map.of(),
            List.of(cue),
            RecoveryPolicy.STOP);

    CompiledSegment rootTransSeg =
        new CompiledSegment(
            AnimationTime.ZERO,
            AnimationTime.fromSeconds(1.0),
            ModelTransform.IDENTITY,
            ModelTransform.of(new Vector3(1.0f, 2.0f, 3.0f)),
            20);
    CompiledTrack rootTransTrack =
        new CompiledTrack(
            AnimationTarget.ROOT, AnimationTrackType.TRANSLATION, List.of(rootTransSeg));

    CompiledSegment rootRotSeg =
        new CompiledSegment(
            AnimationTime.ZERO,
            AnimationTime.fromSeconds(1.0),
            ModelTransform.IDENTITY,
            ModelTransform.of(Vector3.ZERO, new EulerRotation(0.0f, 90.0f, 0.0f).toQuaternion()),
            20);
    CompiledTrack rootRotTrack =
        new CompiledTrack(AnimationTarget.ROOT, AnimationTrackType.ROTATION, List.of(rootRotSeg));

    CompiledSegment rootScaleSeg =
        new CompiledSegment(
            AnimationTime.ZERO,
            AnimationTime.fromSeconds(1.0),
            ModelTransform.IDENTITY,
            new ModelTransform(
                Vector3.ZERO, Quaternion.IDENTITY, new Vector3(2.0f, 2.0f, 2.0f), Vector3.ZERO),
            20);
    CompiledTrack rootScaleTrack =
        new CompiledTrack(AnimationTarget.ROOT, AnimationTrackType.SCALE, List.of(rootScaleSeg));

    CompiledTrack rootVisTrack =
        new CompiledTrack(AnimationTarget.ROOT, AnimationTrackType.VISIBILITY, List.of());

    CompiledSegment partRotSeg =
        new CompiledSegment(
            AnimationTime.ZERO,
            AnimationTime.fromSeconds(1.0),
            ModelTransform.IDENTITY,
            ModelTransform.of(Vector3.ZERO, new EulerRotation(45.0f, 0.0f, 0.0f).toQuaternion()),
            20);
    CompiledTrack partRotTrack =
        new CompiledTrack(
            AnimationTarget.ofPart(partId), AnimationTrackType.ROTATION, List.of(partRotSeg));

    CompiledSegment partScaleSeg =
        new CompiledSegment(
            AnimationTime.ZERO,
            AnimationTime.fromSeconds(1.0),
            ModelTransform.IDENTITY,
            new ModelTransform(
                Vector3.ZERO, Quaternion.IDENTITY, new Vector3(3.0f, 3.0f, 3.0f), Vector3.ZERO),
            20);
    CompiledTrack partScaleTrack =
        new CompiledTrack(
            AnimationTarget.ofPart(partId), AnimationTrackType.SCALE, List.of(partScaleSeg));

    CompiledTrack partVisTrack =
        new CompiledTrack(AnimationTarget.ofPart(partId), AnimationTrackType.VISIBILITY, List.of());

    CompiledAnimation compiled =
        new CompiledAnimation(
            def,
            List.of(
                rootTransTrack,
                rootRotTrack,
                rootScaleTrack,
                rootVisTrack,
                partRotTrack,
                partScaleTrack,
                partVisTrack));

    var pose = compiled.evaluatePose(AnimationTime.fromSeconds(1.0), model);
    assertNotNull(pose);
    assertEquals(def, compiled.definition());
    assertEquals(7, compiled.tracks().size());
    assertEquals(3, compiled.boundaryScheduleTimes().size());
    assertEquals(1.0f, pose.rootTransform().translation().x());
    assertEquals(2.0f, pose.rootTransform().scale().x());
    assertEquals(6.0f, pose.partTransforms().get(partId).scale().x());
  }

  @Test
  void throwsOnHierarchyCycle() {
    ModelPartId aId = ModelPartId.of("partA");
    ModelPartId bId = ModelPartId.of("partB");
    ModelTransform local = ModelTransform.IDENTITY;

    ModelDefinition model =
        new ModelDefinition(
            new ModelId("cyclic"),
            List.of(part(aId, bId, local, local), part(bId, aId, local, local)),
            List.of());

    AnimationDefinition def =
        new AnimationDefinition(
            new AnimationId("cycle"),
            AnimationDuration.fromSeconds(1.0),
            LoopMode.ONCE,
            1,
            Map.of(),
            List.of(),
            RecoveryPolicy.STOP);

    CompiledAnimation compiled = new CompiledAnimation(def, List.of());
    assertThrows(
        IllegalArgumentException.class, () -> compiled.evaluatePose(AnimationTime.ZERO, model));
  }

  @Test
  void throwsOnMissingParent() {
    ModelPartId aId = ModelPartId.of("partA");
    ModelPartId missingParent = ModelPartId.of("unknownParent");
    ModelTransform local = ModelTransform.IDENTITY;

    ModelDefinition model =
        new ModelDefinition(
            new ModelId("missing-parent"),
            List.of(part(aId, missingParent, local, local)),
            List.of());

    AnimationDefinition def =
        new AnimationDefinition(
            new AnimationId("missing"),
            AnimationDuration.fromSeconds(1.0),
            LoopMode.ONCE,
            1,
            Map.of(),
            List.of(),
            RecoveryPolicy.STOP);

    CompiledAnimation compiled = new CompiledAnimation(def, List.of());
    assertThrows(
        IllegalArgumentException.class, () -> compiled.evaluatePose(AnimationTime.ZERO, model));
  }

  private static KeyframeSpec keyframe(String at, float x) {
    KeyframeSpec keyframe = new KeyframeSpec();
    keyframe.setAt(at);
    keyframe.setValue(List.of(x, 0.0f, 0.0f));
    return keyframe;
  }

  private static ModelPartDefinition part(
      ModelPartId id,
      ModelPartId parentId,
      ModelTransform localTransform,
      ModelTransform composedTransform) {
    return new ModelPartDefinition(
        id,
        parentId,
        ModelPartType.ITEM_DISPLAY,
        localTransform,
        composedTransform,
        ModelRenderProperties.DEFAULT,
        ItemAssetRef.of("minecraft:stone"));
  }
}
