package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/** Immutable definition of an interaction hitbox associated with a 3D model. */
public record InteractionDefinition(
    InteractionId interactionId,
    ModelPartId parentPartId,
    Vector3 localOffset,
    Vector3 composedOffset,
    float width,
    float height,
    boolean responsive) {

  public InteractionDefinition {
    Objects.requireNonNull(interactionId, "interactionId cannot be null");
    Objects.requireNonNull(localOffset, "localOffset cannot be null");
    Objects.requireNonNull(composedOffset, "composedOffset cannot be null");
    if (width <= 0.0f) {
      throw new IllegalArgumentException("Interaction width must be positive: " + width);
    }
    if (height <= 0.0f) {
      throw new IllegalArgumentException("Interaction height must be positive: " + height);
    }
  }
}
