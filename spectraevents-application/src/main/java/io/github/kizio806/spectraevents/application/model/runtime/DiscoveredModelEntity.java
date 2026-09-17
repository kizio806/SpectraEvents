package io.github.kizio806.spectraevents.application.model.runtime;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.util.Objects;
import java.util.UUID;

/** Metadata container for a native entity discovered during startup reconciliation. */
public record DiscoveredModelEntity(
    UUID entityUuid,
    ModelRuntimeId runtimeId,
    ModelId definitionId,
    String partOrInteractionId,
    ResourceRole role,
    EventInstanceId ownerEventId) {

  public DiscoveredModelEntity {
    Objects.requireNonNull(entityUuid, "entityUuid cannot be null");
    Objects.requireNonNull(runtimeId, "runtimeId cannot be null");
    Objects.requireNonNull(definitionId, "definitionId cannot be null");
    Objects.requireNonNull(partOrInteractionId, "partOrInteractionId cannot be null");
    Objects.requireNonNull(role, "role cannot be null");
  }
}
