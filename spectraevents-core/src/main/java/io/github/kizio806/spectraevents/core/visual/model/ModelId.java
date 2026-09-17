package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/** Domain identifier for a 3D model. */
public record ModelId(String value) {
  public ModelId {
    Objects.requireNonNull(value, "ModelId value cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("ModelId value cannot be blank");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
