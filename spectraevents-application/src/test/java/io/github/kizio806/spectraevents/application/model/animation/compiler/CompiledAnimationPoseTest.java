package io.github.kizio806.spectraevents.application.model.animation.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartType;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
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
