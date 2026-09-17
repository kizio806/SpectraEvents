package io.github.kizio806.spectraevents.core.visual.animation;

import java.util.Objects;

/** Domain identifier for an animation definition. */
public record AnimationId(String value) {
  public AnimationId {
    Objects.requireNonNull(value, "AnimationId value cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("AnimationId value cannot be blank");
    }
  }

  public static AnimationId of(String value) {
    return new AnimationId(value);
  }
}
