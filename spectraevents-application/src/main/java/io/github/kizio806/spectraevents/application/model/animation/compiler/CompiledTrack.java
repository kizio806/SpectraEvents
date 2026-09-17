package io.github.kizio806.spectraevents.application.model.animation.compiler;

import io.github.kizio806.spectraevents.core.visual.animation.AnimationTarget;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTrackType;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.List;
import java.util.Objects;

/** Precompiled track for a single target and channel with pre-built linear segments. */
public class CompiledTrack {
  private final AnimationTarget target;
  private final AnimationTrackType trackType;
  private final List<CompiledSegment> segments;

  public CompiledTrack(
      AnimationTarget target, AnimationTrackType trackType, List<CompiledSegment> segments) {
    this.target = Objects.requireNonNull(target, "target cannot be null");
    this.trackType = Objects.requireNonNull(trackType, "trackType cannot be null");
    Objects.requireNonNull(segments, "segments cannot be null");
    this.segments = List.copyOf(segments);
  }

  public AnimationTarget target() {
    return target;
  }

  public AnimationTrackType trackType() {
    return trackType;
  }

  public List<CompiledSegment> segments() {
    return segments;
  }

  /** Evaluates the transform at the given playhead time using binary search lookup (O(log N)). */
  public ModelTransform evaluate(AnimationTime playhead, ModelTransform baseTransform) {
    if (segments.isEmpty()) {
      return baseTransform;
    }

    if (playhead.compareTo(segments.get(0).startTime()) <= 0) {
      return segments.get(0).startTransform();
    }

    CompiledSegment lastSegment = segments.get(segments.size() - 1);
    if (playhead.compareTo(lastSegment.endTime()) >= 0) {
      return lastSegment.targetTransform();
    }

    // Binary search for matching segment
    int low = 0;
    int high = segments.size() - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      CompiledSegment seg = segments.get(mid);
      if (playhead.compareTo(seg.startTime()) >= 0 && playhead.compareTo(seg.endTime()) <= 0) {
        long totalNanos = seg.endTime().nanoseconds() - seg.startTime().nanoseconds();
        if (totalNanos <= 0) {
          return seg.targetTransform();
        }
        float t =
            (float) (playhead.nanoseconds() - seg.startTime().nanoseconds()) / (float) totalNanos;
        t = Math.max(0.0f, Math.min(1.0f, t));
        return seg.startTransform().translation().lerp(seg.targetTransform().translation(), t)
                != null
            ? new ModelTransform(
                seg.startTransform().translation().lerp(seg.targetTransform().translation(), t),
                seg.startTransform().rotation().slerp(seg.targetTransform().rotation(), t),
                seg.startTransform().scale().lerp(seg.targetTransform().scale(), t),
                seg.targetTransform().pivot())
            : seg.targetTransform();
      } else if (playhead.compareTo(seg.startTime()) < 0) {
        high = mid - 1;
      } else {
        low = mid + 1;
      }
    }

    return lastSegment.targetTransform();
  }
}
