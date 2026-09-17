package dev.spectraevents.core.visual.model;

import java.util.Objects;

/** Defines a single part of a model. */
public record ModelPartDefinition(String partId, Transform localTransform, String visualId) {
  public ModelPartDefinition {
    Objects.requireNonNull(partId, "partId cannot be null");
    Objects.requireNonNull(localTransform, "localTransform cannot be null");
    Objects.requireNonNull(visualId, "visualId cannot be null");
    if (partId.isBlank()) {
      throw new IllegalArgumentException("partId cannot be blank");
    }
    if (visualId.isBlank()) {
      throw new IllegalArgumentException("visualId cannot be blank");
    }
  }
}
