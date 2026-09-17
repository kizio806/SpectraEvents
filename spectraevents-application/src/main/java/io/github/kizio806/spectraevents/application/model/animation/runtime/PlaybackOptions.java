package io.github.kizio806.spectraevents.application.model.animation.runtime;

import io.github.kizio806.spectraevents.core.visual.animation.AnimationCueReached;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy;
import java.util.function.Consumer;

/** Configuration options when initiating an animation playback. */
public record PlaybackOptions(
    float speed,
    LoopMode loopMode,
    int maxLoops,
    RecoveryPolicy recoveryPolicy,
    Consumer<AnimationCueReached> cueListener) {

  public static final PlaybackOptions DEFAULT = new PlaybackOptions(1.0f, null, -1, null, null);

  public PlaybackOptions {
    if (speed <= 0.0f) {
      throw new IllegalArgumentException("Playback speed must be positive");
    }
  }

  public static PlaybackOptions withSpeed(float speed) {
    return new PlaybackOptions(speed, null, -1, null, null);
  }

  public static PlaybackOptions withCueListener(Consumer<AnimationCueReached> cueListener) {
    return new PlaybackOptions(1.0f, null, -1, null, cueListener);
  }
}
