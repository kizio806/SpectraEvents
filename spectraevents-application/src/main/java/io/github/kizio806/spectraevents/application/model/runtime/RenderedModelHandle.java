package io.github.kizio806.spectraevents.application.model.runtime;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.InteractionId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Platform-neutral handle for a completely rendered 3D model instance. */
public record RenderedModelHandle(
    ModelRuntimeId runtimeId,
    ModelId definitionId,
    EventInstanceId ownerEventId,
    ModelAnchor anchor,
    Map<ModelPartId, RenderedPartHandle> parts,
    Map<InteractionId, UUID> interactions) {

  public RenderedModelHandle {
    Objects.requireNonNull(runtimeId, "runtimeId cannot be null");
    Objects.requireNonNull(definitionId, "definitionId cannot be null");
    Objects.requireNonNull(anchor, "anchor cannot be null");
    Objects.requireNonNull(parts, "parts cannot be null");
    Objects.requireNonNull(interactions, "interactions cannot be null");

    parts = Map.copyOf(parts);
    interactions = Map.copyOf(interactions);
  }
}
