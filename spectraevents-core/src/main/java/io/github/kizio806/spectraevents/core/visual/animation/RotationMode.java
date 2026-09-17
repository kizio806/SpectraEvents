package io.github.kizio806.spectraevents.core.visual.animation;

/** Rotation interpolation mode. */
public enum RotationMode {
  /** Shortest path SLERP between quaternions. */
  SHORTEST,

  /** Continuous multi-revolution rotation preserving total angular displacement. */
  CONTINUOUS
}
