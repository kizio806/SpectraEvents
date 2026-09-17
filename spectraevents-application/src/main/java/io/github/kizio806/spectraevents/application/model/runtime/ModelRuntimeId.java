package io.github.kizio806.spectraevents.application.model.runtime;

import java.util.Objects;
import java.util.UUID;

/** Unique domain identifier for an active spawned 3D model instance. */
public record ModelRuntimeId(String value) {
  public ModelRuntimeId {
    Objects.requireNonNull(value, "ModelRuntimeId value cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("ModelRuntimeId value cannot be blank");
    }
  }

  public static ModelRuntimeId generate() {
    return new ModelRuntimeId("model-" + UUID.randomUUID());
  }

  public static ModelRuntimeId of(String value) {
    return new ModelRuntimeId(value);
  }
}
