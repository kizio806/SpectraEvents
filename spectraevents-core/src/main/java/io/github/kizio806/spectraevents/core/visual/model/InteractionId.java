package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/** Domain identifier for an interaction hitbox within a 3D model. */
public record InteractionId(String value) {
  public InteractionId {
    Objects.requireNonNull(value, "InteractionId value cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("InteractionId value cannot be blank");
    }
  }

  public static InteractionId of(String value) {
    return new InteractionId(value);
  }
}
