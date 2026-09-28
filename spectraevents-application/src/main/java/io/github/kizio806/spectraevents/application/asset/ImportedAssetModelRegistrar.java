package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.AnimationTrackSpec;
import io.github.kizio806.spectraevents.application.model.animation.spec.KeyframeSpec;
import io.github.kizio806.spectraevents.application.model.compiler.CompiledModel;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.spec.ModelPartSpec;
import io.github.kizio806.spectraevents.application.model.spec.ModelSpec;
import io.github.kizio806.spectraevents.application.model.spec.TransformSpec;
import io.github.kizio806.spectraevents.core.visual.animation.Easing;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetAnimation;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetNode;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Compiles imported Blockbench nodes into normal model and animation definitions. */
public final class ImportedAssetModelRegistrar {
  private static final float BLOCKBENCH_UNITS_PER_BLOCK = 16.0f;
  private static final float MINIMUM_RENDERABLE_SCALE = 0.001f;

  private final ModelCompiler modelCompiler;
  private final ModelDefinitionRegistry modelRegistry;
  private final AnimationDefinitionRegistry animationRegistry;

  public ImportedAssetModelRegistrar(
      ModelCompiler modelCompiler,
      ModelDefinitionRegistry modelRegistry,
      AnimationDefinitionRegistry animationRegistry) {
    this.modelCompiler = Objects.requireNonNull(modelCompiler, "modelCompiler");
    this.modelRegistry = Objects.requireNonNull(modelRegistry, "modelRegistry");
    this.animationRegistry = Objects.requireNonNull(animationRegistry, "animationRegistry");
  }

  public ModelDefinition register(SpectraAssetDocument document) {
    Objects.requireNonNull(document, "document");
    CompiledModel compiled = modelCompiler.compileWithAnimations(toModelSpec(document));
    modelRegistry.replace(compiled.definition());
    animationRegistry.unregister(compiled.definition().id());
    for (CompiledAnimation animation : compiled.animations().values()) {
      animationRegistry.register(compiled.definition().id(), animation);
    }
    return compiled.definition();
  }

  private ModelSpec toModelSpec(SpectraAssetDocument document) {
    ModelSpec model = new ModelSpec();
    model.setId(document.modelId());
    Map<String, ModelPartSpec> parts = new LinkedHashMap<>();
    for (SpectraAssetNode node : document.nodes()) {
      addNodeParts(document.modelId(), node, null, parts);
    }
    model.setParts(Map.copyOf(parts));
    model.setAnimations(toAnimationSpecs(document.animations()));
    return model;
  }

  private void addNodeParts(
      String modelId, SpectraAssetNode node, String parentId, Map<String, ModelPartSpec> parts) {
    ModelPartSpec part = new ModelPartSpec();
    part.setType("item_display");
    part.setItem(GeneratedAssetItem.reference(modelId, node.nodeId()));
    part.setTransformMode("fixed");
    part.setParent(parentId);
    part.setTransform(transform(node));
    if (parts.putIfAbsent(node.nodeId(), part) != null) {
      throw new IllegalArgumentException(
          "Imported Blockbench group ID is not unique: " + node.nodeId());
    }
    for (SpectraAssetNode child : node.children()) {
      addNodeParts(modelId, child, node.nodeId(), parts);
    }
  }

  private static TransformSpec transform(SpectraAssetNode node) {
    TransformSpec transform = new TransformSpec();
    transform.setTranslation(
        vector(node.translation().x(), node.translation().y(), node.translation().z(), true));
    transform.setScale(vector(node.scale().x(), node.scale().y(), node.scale().z(), false));
    transform.setPivot(vector(node.pivot().x(), node.pivot().y(), node.pivot().z(), true));
    TransformSpec.RotationSpec rotation = new TransformSpec.RotationSpec();
    rotation.setEuler(
        List.of(node.rotation().pitchX(), node.rotation().yawY(), node.rotation().rollZ()));
    transform.setRotation(rotation);
    return transform;
  }

  private static Map<String, AnimationSpec> toAnimationSpecs(
      Map<String, SpectraAssetAnimation> animations) {
    Map<String, AnimationSpec> result = new LinkedHashMap<>();
    for (Map.Entry<String, SpectraAssetAnimation> entry : animations.entrySet()) {
      SpectraAssetAnimation imported = entry.getValue();
      AnimationSpec animation = new AnimationSpec();
      animation.setDuration(imported.duration().time().toMillis() + "ms");
      animation.setLoop(imported.loopMode().name());
      animation.setRecovery("RESUME");
      Map<String, AnimationTrackSpec> tracks = new LinkedHashMap<>();
      addTranslationTracks(tracks, imported);
      addRotationTracks(tracks, imported);
      addScaleTracks(tracks, imported);
      animation.setTracks(Map.copyOf(tracks));
      result.put(entry.getKey(), animation);
    }
    return Map.copyOf(result);
  }

  private static void addTranslationTracks(
      Map<String, AnimationTrackSpec> tracks, SpectraAssetAnimation animation) {
    animation
        .translationTracks()
        .forEach(
            (nodeId, keyframes) -> {
              AnimationTrackSpec track =
                  tracks.computeIfAbsent(nodeId, ignored -> new AnimationTrackSpec());
              List<KeyframeSpec> converted = new ArrayList<>();
              keyframes.forEach(
                  keyframe -> {
                    KeyframeSpec convertedKeyframe =
                        commonKeyframe(keyframe.time().toMillis(), keyframe.easing());
                    convertedKeyframe.setValue(
                        vector(
                            keyframe.value().x(),
                            keyframe.value().y(),
                            keyframe.value().z(),
                            true));
                    converted.add(convertedKeyframe);
                  });
              track.setTranslation(List.copyOf(converted));
            });
  }

  private static void addRotationTracks(
      Map<String, AnimationTrackSpec> tracks, SpectraAssetAnimation animation) {
    animation
        .rotationTracks()
        .forEach(
            (nodeId, keyframes) -> {
              AnimationTrackSpec track =
                  tracks.computeIfAbsent(nodeId, ignored -> new AnimationTrackSpec());
              List<KeyframeSpec> converted = new ArrayList<>();
              keyframes.forEach(
                  keyframe -> {
                    KeyframeSpec convertedKeyframe =
                        commonKeyframe(keyframe.time().toMillis(), keyframe.easing());
                    convertedKeyframe.setEuler(
                        List.of(
                            keyframe.euler().pitchX(),
                            keyframe.euler().yawY(),
                            keyframe.euler().rollZ()));
                    convertedKeyframe.setRotationMode(keyframe.rotationMode().name());
                    converted.add(convertedKeyframe);
                  });
              track.setRotation(List.copyOf(converted));
            });
  }

  private static void addScaleTracks(
      Map<String, AnimationTrackSpec> tracks, SpectraAssetAnimation animation) {
    animation
        .scaleTracks()
        .forEach(
            (nodeId, keyframes) -> {
              AnimationTrackSpec track =
                  tracks.computeIfAbsent(nodeId, ignored -> new AnimationTrackSpec());
              List<KeyframeSpec> converted = new ArrayList<>();
              keyframes.forEach(
                  keyframe -> {
                    KeyframeSpec convertedKeyframe =
                        commonKeyframe(keyframe.time().toMillis(), keyframe.easing());
                    convertedKeyframe.setValue(
                        vector(
                            renderableScale(keyframe.scale().x()),
                            renderableScale(keyframe.scale().y()),
                            renderableScale(keyframe.scale().z()),
                            false));
                    converted.add(convertedKeyframe);
                  });
              track.setScale(List.copyOf(converted));
            });
  }

  private static KeyframeSpec commonKeyframe(long millis, Easing easing) {
    KeyframeSpec keyframe = new KeyframeSpec();
    keyframe.setAt(millis + "ms");
    keyframe.setEasing(easing.name());
    return keyframe;
  }

  private static List<Float> vector(float x, float y, float z, boolean convertUnits) {
    if (convertUnits) {
      return List.of(
          x / BLOCKBENCH_UNITS_PER_BLOCK,
          y / BLOCKBENCH_UNITS_PER_BLOCK,
          z / BLOCKBENCH_UNITS_PER_BLOCK);
    }
    return List.of(x, y, z);
  }

  private static float renderableScale(float value) {
    return value == 0.0f ? MINIMUM_RENDERABLE_SCALE : value;
  }
}
