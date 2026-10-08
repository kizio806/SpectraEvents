package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDuration;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.Easing;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.ScaleKeyframe;
import io.github.kizio806.spectraevents.core.visual.animation.Vector3Keyframe;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetAnimation;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetNode;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImportedAssetModelRegistrarTest {

  @Test
  void registersImportedNodesAsEventUsableModelPartsAndAnimations() {
    ModelDefinitionRegistry modelRegistry = new ModelDefinitionRegistry();
    AnimationDefinitionRegistry animationRegistry = new AnimationDefinitionRegistry();
    ImportedAssetModelRegistrar registrar =
        new ImportedAssetModelRegistrar(new ModelCompiler(), modelRegistry, animationRegistry);

    registrar.register(document());

    ModelId modelId = new ModelId("meteor");
    var definition = modelRegistry.get(modelId).orElseThrow();
    Assertions.assertEquals(1, definition.parts().size());
    ItemAssetRef asset = (ItemAssetRef) definition.parts().getFirst().visualAsset();
    Assertions.assertEquals(GeneratedAssetItem.reference("meteor", "root"), asset.itemRef());
    Assertions.assertTrue(animationRegistry.find(modelId, new AnimationId("pulse")).isPresent());
  }

  @Test
  void mapsTerminalZeroScaleToMinimalRenderableScale() {
    ModelDefinitionRegistry modelRegistry = new ModelDefinitionRegistry();
    AnimationDefinitionRegistry animationRegistry = new AnimationDefinitionRegistry();
    ImportedAssetModelRegistrar registrar =
        new ImportedAssetModelRegistrar(new ModelCompiler(), modelRegistry, animationRegistry);

    Assertions.assertDoesNotThrow(() -> registrar.register(documentWithTerminalZeroScale()));
  }

  private static SpectraAssetDocument document() {
    SpectraAssetNode root =
        new SpectraAssetNode(
            "root",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(),
            List.of());
    SpectraAssetAnimation animation =
        new SpectraAssetAnimation(
            "pulse",
            AnimationDuration.fromSeconds(1.0),
            LoopMode.LOOP,
            Map.of(
                "root",
                List.of(
                    new Vector3Keyframe(AnimationTime.ZERO, Vector3.ZERO, Easing.LINEAR),
                    new Vector3Keyframe(
                        AnimationTime.fromSeconds(1.0),
                        new Vector3(16.0f, 0.0f, 0.0f),
                        Easing.LINEAR))),
            Map.of(),
            Map.of(),
            List.of());
    return new SpectraAssetDocument(
        1, "meteor", Map.of(), List.of(root), Map.of("pulse", animation));
  }

  private static SpectraAssetDocument documentWithTerminalZeroScale() {
    SpectraAssetNode root =
        new SpectraAssetNode(
            "root",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(),
            List.of());
    SpectraAssetAnimation animation =
        new SpectraAssetAnimation(
            "collapse",
            AnimationDuration.fromSeconds(1.0),
            LoopMode.ONCE,
            Map.of(),
            Map.of(),
            Map.of(
                "root",
                List.of(
                    new ScaleKeyframe(
                        AnimationTime.fromSeconds(1.0), Vector3.ZERO, Easing.LINEAR))),
            List.of());
    return new SpectraAssetDocument(
        1, "meteor", Map.of(), List.of(root), Map.of("collapse", animation));
  }
}
