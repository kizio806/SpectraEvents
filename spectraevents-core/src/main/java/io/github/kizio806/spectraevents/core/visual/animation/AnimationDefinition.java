package io.github.kizio806.spectraevents.core.visual.animation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, compiled, validated, platform-neutral definition of an animation clip. */
public record AnimationDefinition(
    AnimationId id,
    AnimationDuration duration,
    LoopMode loopMode,
    int loopCount,
    Map<AnimationTarget, List<?>> tracks,
    List<TimelineCue> cues,
    RecoveryPolicy recoveryPolicy) {

  public AnimationDefinition {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(duration, "duration cannot be null");
    Objects.requireNonNull(loopMode, "loopMode cannot be null");
    Objects.requireNonNull(recoveryPolicy, "recoveryPolicy cannot be null");
    tracks =
        tracks == null
            ? Map.of()
            : tracks.entrySet().stream()
                .collect(
                    java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    cues = cues == null ? List.of() : List.copyOf(cues);

    if (duration.time().nanoseconds() <= 0) {
      throw new IllegalArgumentException(
          "AnimationDefinition duration must be positive: " + duration.time().toMillis() + "ms");
    }
  }

  public static AnimationDefinition of(
      AnimationId id,
      AnimationDuration duration,
      LoopMode loopMode,
      int loopCount,
      Map<AnimationTarget, List<?>> tracks,
      List<TimelineCue> cues,
      RecoveryPolicy recoveryPolicy) {
    return new AnimationDefinition(id, duration, loopMode, loopCount, tracks, cues, recoveryPolicy);
  }

  @Override
  public Map<AnimationTarget, List<?>> tracks() {
    return Map.copyOf(tracks);
  }

  @Override
  public List<TimelineCue> cues() {
    return List.copyOf(cues);
  }
}
