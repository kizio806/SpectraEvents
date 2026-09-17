package io.github.kizio806.spectraevents.application.model.animation.runtime;

import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy;
import java.util.Objects;

/** Immutable snapshot state of an active or persisted animation playback. */
public record AnimationPlaybackState(
    AnimationPlaybackId playbackId,
    RenderedModelHandle modelHandle,
    AnimationId animationId,
    PlaybackState state,
    AnimationTime currentTime,
    float speed,
    LoopMode loopMode,
    int currentLoop,
    RecoveryPolicy recoveryPolicy) {

  public AnimationPlaybackState {
    Objects.requireNonNull(playbackId, "playbackId cannot be null");
    Objects.requireNonNull(modelHandle, "modelHandle cannot be null");
    Objects.requireNonNull(animationId, "animationId cannot be null");
    Objects.requireNonNull(state, "state cannot be null");
    Objects.requireNonNull(currentTime, "currentTime cannot be null");
    Objects.requireNonNull(loopMode, "loopMode cannot be null");
    Objects.requireNonNull(recoveryPolicy, "recoveryPolicy cannot be null");

    if (speed <= 0.0f) {
      throw new IllegalArgumentException("Animation playback speed must be positive");
    }
  }
}
