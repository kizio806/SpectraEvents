package io.github.kizio806.spectraevents.application.model.runtime;

import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import java.util.Objects;
import java.util.UUID;

/** Platform-neutral handle for a rendered visual display part entity. */
public record RenderedPartHandle(ModelPartId partId, UUID entityUuid) {
  public RenderedPartHandle {
    Objects.requireNonNull(partId, "partId cannot be null");
    Objects.requireNonNull(entityUuid, "entityUuid cannot be null");
  }
}
