package io.github.kizio806.spectraevents.application.model.animation.runtime;

import java.util.Objects;
import java.util.UUID;

/** Unique identifier for an active animation playback instance. */
public record AnimationPlaybackId(String value) {

  public AnimationPlaybackId {
    Objects.requireNonNull(value, "value cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("AnimationPlaybackId cannot be blank");
    }
  }

  public static AnimationPlaybackId random() {
    return new AnimationPlaybackId(UUID.randomUUID().toString());
  }

  public static AnimationPlaybackId of(String value) {
    return new AnimationPlaybackId(value);
  }
}
