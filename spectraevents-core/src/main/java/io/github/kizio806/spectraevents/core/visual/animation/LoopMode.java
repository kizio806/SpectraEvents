package io.github.kizio806.spectraevents.core.visual.animation;

/** Playback loop behavior for an animation definition. */
public enum LoopMode {
  /** Animation plays once from start to end and stops at completed state. */
  ONCE,

  /** Animation loops continuously from start to end. */
  LOOP,

  /** Animation plays forward to end then backward to start. */
  PING_PONG
}
