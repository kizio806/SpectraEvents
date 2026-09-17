package io.github.kizio806.spectraevents.core.visual.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable compiled definition of a multi-part 3D model with visual elements and hitboxes. */
public record ModelDefinition(
    ModelId id, List<ModelPartDefinition> parts, List<InteractionDefinition> interactions) {

  public static final int MAX_PARTS_LIMIT = 500;
  public static final int MAX_INTERACTIONS_LIMIT = 100;

  public ModelDefinition {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(parts, "parts cannot be null");
    Objects.requireNonNull(interactions, "interactions cannot be null");

    parts = List.copyOf(parts);
    interactions = List.copyOf(interactions);

    if (parts.isEmpty()) {
      throw new IllegalArgumentException("ModelDefinition must contain at least one part");
    }
    if (parts.size() > MAX_PARTS_LIMIT) {
      throw new IllegalArgumentException(
          "ModelDefinition parts count ("
              + parts.size()
              + ") exceeds guardrail limit of "
              + MAX_PARTS_LIMIT);
    }
    if (interactions.size() > MAX_INTERACTIONS_LIMIT) {
      throw new IllegalArgumentException(
          "ModelDefinition interactions count ("
              + interactions.size()
              + ") exceeds guardrail limit of "
              + MAX_INTERACTIONS_LIMIT);
    }

    long distinctPartIds = parts.stream().map(ModelPartDefinition::partId).distinct().count();
    if (distinctPartIds != parts.size()) {
      throw new IllegalArgumentException("ModelDefinition contains duplicate part IDs");
    }

    long distinctInteractionIds =
        interactions.stream().map(InteractionDefinition::interactionId).distinct().count();
    if (distinctInteractionIds != interactions.size()) {
      throw new IllegalArgumentException("ModelDefinition contains duplicate interaction IDs");
    }
  }

  public Optional<ModelPartDefinition> findPart(ModelPartId partId) {
    Objects.requireNonNull(partId, "partId cannot be null");
    return parts.stream().filter(p -> p.partId().equals(partId)).findFirst();
  }

  public Optional<InteractionDefinition> findInteraction(InteractionId interactionId) {
    Objects.requireNonNull(interactionId, "interactionId cannot be null");
    return interactions.stream().filter(i -> i.interactionId().equals(interactionId)).findFirst();
  }
}
