package io.github.kizio806.spectraevents.core.visual.animation;

import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.util.Objects;

/** Event emitted when a timeline cue marker is passed during animation playback. */
public record AnimationCueReached(
    String playbackId,
    String modelRuntimeId,
    ModelId modelId,
    AnimationId animationId,
    String cueId,
    AnimationTime time) {

  public AnimationCueReached {
    Objects.requireNonNull(playbackId, "playbackId cannot be null");
    Objects.requireNonNull(modelRuntimeId, "modelRuntimeId cannot be null");
    Objects.requireNonNull(modelId, "modelId cannot be null");
    Objects.requireNonNull(animationId, "animationId cannot be null");
    Objects.requireNonNull(cueId, "cueId cannot be null");
    Objects.requireNonNull(time, "time cannot be null");
  }
}
