package io.github.kizio806.spectraevents.application.model.animation.compiler;

import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.Objects;

/** Precompiled interpolation segment for client-side Display entity update. */
public record CompiledSegment(
    AnimationTime startTime,
    AnimationTime endTime,
    ModelTransform startTransform,
    ModelTransform targetTransform,
    int interpolationDurationTicks)
    implements Comparable<CompiledSegment> {

  public CompiledSegment {
    Objects.requireNonNull(startTime, "startTime cannot be null");
    Objects.requireNonNull(endTime, "endTime cannot be null");
    Objects.requireNonNull(startTransform, "startTransform cannot be null");
    Objects.requireNonNull(targetTransform, "targetTransform cannot be null");
    if (interpolationDurationTicks < 0) {
      throw new IllegalArgumentException("interpolationDurationTicks cannot be negative");
    }
  }

  @Override
  public int compareTo(CompiledSegment o) {
    return this.startTime.compareTo(o.startTime);
  }
}
