package io.github.kizio806.spectraevents.application.model.animation.runtime;

import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation;
import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledSegment;
import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledTrack;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationCueReached;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy;
import io.github.kizio806.spectraevents.core.visual.animation.TimelineCue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Represents a running or paused animation instance bound to a rendered 3D model handle. */
public class ActiveAnimation {

  private final AnimationPlaybackId playbackId;
  private final RenderedModelHandle modelHandle;
  private final CompiledAnimation compiledAnimation;
  private final Consumer<AnimationCueReached> cueListener;

  private PlaybackState state;
  private AnimationTime currentTime;
  private float speed;
  private LoopMode loopMode;
  private int maxLoops;
  private int currentLoop;
  private RecoveryPolicy recoveryPolicy;

  private final Map<CompiledTrack, Integer> trackSegmentIndices = new HashMap<>();
  private final Set<String> firedCueIds = new HashSet<>();

  public ActiveAnimation(
      AnimationPlaybackId playbackId,
      RenderedModelHandle modelHandle,
      CompiledAnimation compiledAnimation,
      float speed,
      LoopMode loopMode,
      int maxLoops,
      RecoveryPolicy recoveryPolicy,
      Consumer<AnimationCueReached> cueListener) {

    this.playbackId = Objects.requireNonNull(playbackId, "playbackId cannot be null");
    this.modelHandle = Objects.requireNonNull(modelHandle, "modelHandle cannot be null");
    this.compiledAnimation =
        Objects.requireNonNull(compiledAnimation, "compiledAnimation cannot be null");
    this.cueListener = cueListener;

    this.state = PlaybackState.PLAYING;
    this.currentTime = AnimationTime.ZERO;
    this.speed = speed > 0.0f ? speed : 1.0f;
    this.loopMode = loopMode != null ? loopMode : compiledAnimation.definition().loopMode();
    this.maxLoops = maxLoops >= 0 ? maxLoops : compiledAnimation.definition().loopCount();
    this.currentLoop = 0;
    this.recoveryPolicy =
        recoveryPolicy != null ? recoveryPolicy : compiledAnimation.definition().recoveryPolicy();

    for (CompiledTrack track : compiledAnimation.tracks()) {
      trackSegmentIndices.put(track, 0);
    }
  }

  public AnimationPlaybackId playbackId() {
    return playbackId;
  }

  public RenderedModelHandle modelHandle() {
    return modelHandle;
  }

  public CompiledAnimation compiledAnimation() {
    return compiledAnimation;
  }

  public synchronized PlaybackState state() {
    return state;
  }

  public synchronized void setState(PlaybackState state) {
    this.state = Objects.requireNonNull(state, "state cannot be null");
  }

  public synchronized AnimationTime currentTime() {
    return currentTime;
  }

  public synchronized void setCurrentTime(AnimationTime currentTime) {
    seekTo(currentTime);
  }

  /**
   * Seeks the playhead to a specific target time, aligning track segment indices and timeline cues.
   */
  public synchronized void seekTo(AnimationTime targetTime) {
    this.currentTime = Objects.requireNonNull(targetTime, "targetTime cannot be null");

    // Align segment indices for each track to match targetTime playhead
    for (CompiledTrack track : compiledAnimation.tracks()) {
      int matchingIndex = 0;
      List<CompiledSegment> segments = track.segments();
      for (int i = 0; i < segments.size(); i++) {
        CompiledSegment seg = segments.get(i);
        if (targetTime.compareTo(seg.startTime()) >= 0
            && targetTime.compareTo(seg.endTime()) <= 0) {
          matchingIndex = i;
          break;
        } else if (targetTime.compareTo(seg.endTime()) > 0) {
          matchingIndex = i + 1;
        }
      }
      trackSegmentIndices.put(track, Math.min(matchingIndex, segments.size()));
    }

    // Align fired cues to prevent duplicate/skipped cue emission
    firedCueIds.clear();
    for (TimelineCue cue : compiledAnimation.definition().cues()) {
      if (cue.time().compareTo(targetTime) <= 0) {
        firedCueIds.add(cue.cueId());
      }
    }
  }

  public synchronized float speed() {
    return speed;
  }

  public synchronized void setSpeed(float speed) {
    if (speed <= 0.0f) {
      throw new IllegalArgumentException("Speed must be positive");
    }
    this.speed = speed;
  }

  public synchronized LoopMode loopMode() {
    return loopMode;
  }

  public synchronized int currentLoop() {
    return currentLoop;
  }

  public synchronized RecoveryPolicy recoveryPolicy() {
    return recoveryPolicy;
  }

  /**
   * Advances the internal animation clock by delta time, evaluating timeline cues and loop
   * boundaries.
   *
   * @param deltaNanos time elapsed in nanoseconds
   * @return true if the animation reached its end and completed; false if active/looping
   */
  public synchronized boolean advanceTime(long deltaNanos) {
    if (state != PlaybackState.PLAYING) {
      return false;
    }

    long scaledNanos = (long) (deltaNanos * speed);
    long newNanos = currentTime.nanoseconds() + scaledNanos;
    long durationNanos = compiledAnimation.definition().duration().toNanos();

    // Check cues
    checkCues(currentTime, AnimationTime.fromNanos(newNanos));

    if (newNanos >= durationNanos) {
      if (loopMode == LoopMode.LOOP || loopMode == LoopMode.PING_PONG) {
        currentLoop++;
        if (maxLoops > 0 && currentLoop >= maxLoops) {
          state = PlaybackState.COMPLETED;
          currentTime = AnimationTime.fromNanos(durationNanos);
          return true;
        } else {
          // Loop around
          long overflow = newNanos % durationNanos;
          currentTime = AnimationTime.fromNanos(overflow);
          firedCueIds.clear();
          resetTrackSegmentIndices();
          return false;
        }
      } else {
        state = PlaybackState.COMPLETED;
        currentTime = AnimationTime.fromNanos(durationNanos);
        return true;
      }
    } else {
      currentTime = AnimationTime.fromNanos(newNanos);
      return false;
    }
  }

  private void checkCues(AnimationTime from, AnimationTime to) {
    if (cueListener == null) return;
    List<TimelineCue> cues = compiledAnimation.definition().cues();
    for (TimelineCue cue : cues) {
      if (!firedCueIds.contains(cue.cueId())
          && cue.time().compareTo(from) >= 0
          && cue.time().compareTo(to) <= 0) {

        firedCueIds.add(cue.cueId());
        cueListener.accept(
            new AnimationCueReached(
                playbackId.value(),
                modelHandle.runtimeId().value(),
                modelHandle.definitionId(),
                compiledAnimation.definition().id(),
                cue.cueId(),
                cue.time()));
      }
    }
  }

  private void resetTrackSegmentIndices() {
    for (CompiledTrack track : compiledAnimation.tracks()) {
      trackSegmentIndices.put(track, 0);
    }
  }

  public synchronized CompiledSegment getActiveSegment(CompiledTrack track) {
    Integer idx = trackSegmentIndices.get(track);
    if (idx == null || idx >= track.segments().size()) {
      return null;
    }
    return track.segments().get(idx);
  }

  public synchronized void advanceTrackSegment(CompiledTrack track) {
    trackSegmentIndices.compute(track, (k, current) -> (current == null) ? 0 : current + 1);
  }

  public synchronized AnimationPlaybackState toSnapshotState() {
    return new AnimationPlaybackState(
        playbackId,
        modelHandle,
        compiledAnimation.definition().id(),
        state,
        currentTime,
        speed,
        loopMode,
        currentLoop,
        recoveryPolicy);
  }
}
