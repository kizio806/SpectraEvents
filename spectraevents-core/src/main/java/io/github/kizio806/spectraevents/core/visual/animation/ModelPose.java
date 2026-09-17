package io.github.kizio806.spectraevents.core.visual.animation;

import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/** Neutral visual pose snapshot of an entire 3D model (root transform + local part transforms). */
public record ModelPose(
    ModelTransform rootTransform, Map<ModelPartId, ModelTransform> partTransforms) {

  public ModelPose {
    Objects.requireNonNull(rootTransform, "rootTransform cannot be null");
    partTransforms = partTransforms != null ? Map.copyOf(partTransforms) : Collections.emptyMap();
  }

  public static ModelPose of(
      ModelTransform rootTransform, Map<ModelPartId, ModelTransform> partTransforms) {
    return new ModelPose(rootTransform, partTransforms);
  }
}
