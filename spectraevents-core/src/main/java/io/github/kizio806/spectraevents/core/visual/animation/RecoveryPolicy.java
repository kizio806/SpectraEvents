package io.github.kizio806.spectraevents.core.visual.animation;

/**
 * Policy defining how active animation playbacks are recovered after server boot/reconciliation.
 */
public enum RecoveryPolicy {
  /** Resumes playback from stored playhead position. */
  RESUME,

  /** Restarts animation playback from time 0. */
  RESTART,

  /** Stops animation playback and retains current pose. */
  STOP
}
