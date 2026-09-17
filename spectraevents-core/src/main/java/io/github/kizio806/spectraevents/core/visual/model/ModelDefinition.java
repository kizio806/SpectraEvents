package io.github.kizio806.spectraevents.core.visual.model;

import java.util.List;
import java.util.Objects;

/** Defines a multipart 3D model independent of a specific event instance. */
public record ModelDefinition(ModelId id, List<ModelPartDefinition> parts) {
  public ModelDefinition {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(parts, "parts cannot be null");
    parts = List.copyOf(parts); // Defensive copy

    if (parts.isEmpty()) {
      throw new IllegalArgumentException("ModelDefinition must contain at least one part");
    }

    long distinctIds = parts.stream().map(ModelPartDefinition::partId).distinct().count();
    if (distinctIds != parts.size()) {
      throw new IllegalArgumentException("ModelDefinition contains duplicate part IDs");
    }
  }
}
