package io.github.kizio806.spectraevents.core.visual.asset;

import io.github.kizio806.spectraevents.core.visual.animation.AnimationDuration;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RotationKeyframe;
import io.github.kizio806.spectraevents.core.visual.animation.ScaleKeyframe;
import io.github.kizio806.spectraevents.core.visual.animation.TimelineCue;
import io.github.kizio806.spectraevents.core.visual.animation.Vector3Keyframe;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Represents a stable parsed animation for the asset model. */
public record SpectraAssetAnimation(
    String animationId,
    AnimationDuration duration,
    LoopMode loopMode,
    Map<String, List<Vector3Keyframe>> translationTracks,
    Map<String, List<RotationKeyframe>> rotationTracks,
    Map<String, List<ScaleKeyframe>> scaleTracks,
    List<TimelineCue> cues) {

  public SpectraAssetAnimation {
    Objects.requireNonNull(animationId, "animationId cannot be null");
    Objects.requireNonNull(duration, "duration cannot be null");
    Objects.requireNonNull(loopMode, "loopMode cannot be null");
    Objects.requireNonNull(translationTracks, "translationTracks cannot be null");
    Objects.requireNonNull(rotationTracks, "rotationTracks cannot be null");
    Objects.requireNonNull(scaleTracks, "scaleTracks cannot be null");
    Objects.requireNonNull(cues, "cues cannot be null");

    if (animationId.isBlank()) {
      throw new IllegalArgumentException("animationId cannot be blank");
    }

    translationTracks = Map.copyOf(translationTracks);
    rotationTracks = Map.copyOf(rotationTracks);
    scaleTracks = Map.copyOf(scaleTracks);
    cues = List.copyOf(cues);
  }
}
