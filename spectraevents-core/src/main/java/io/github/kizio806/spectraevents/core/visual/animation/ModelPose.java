package io.github.kizio806.spectraevents.core.visual.animation;

import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.Map;
import java.util.Objects;

/** Neutral visual pose snapshot of an entire model (root transform + rendered part transforms). */
public record ModelPose(
    ModelTransform rootTransform, Map<ModelPartId, ModelTransform> partTransforms) {

  public ModelPose {
    Objects.requireNonNull(rootTransform, "rootTransform cannot be null");
    partTransforms = partTransforms == null ? Map.of() : Map.copyOf(partTransforms);
  }

  public static ModelPose of(
      ModelTransform rootTransform, Map<ModelPartId, ModelTransform> partTransforms) {
    return new ModelPose(rootTransform, partTransforms);
  }

  @Override
  public Map<ModelPartId, ModelTransform> partTransforms() {
    return Map.copyOf(partTransforms);
  }
}
