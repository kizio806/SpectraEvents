package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/** Domain identifier for a part within a 3D model definition. */
public record ModelPartId(String value) {
  public ModelPartId {
    Objects.requireNonNull(value, "ModelPartId value cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("ModelPartId value cannot be blank");
    }
  }

  public static ModelPartId of(String value) {
    return new ModelPartId(value);
  }
}
