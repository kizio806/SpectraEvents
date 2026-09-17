package io.github.kizio806.spectraevents.core.visual.animation;

import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.Objects;

/** Neutral visual pose snapshot of a single model part at a specific instant. */
public record PartPose(ModelTransform localTransform) {
  public PartPose {
    Objects.requireNonNull(localTransform, "localTransform cannot be null");
  }
}
