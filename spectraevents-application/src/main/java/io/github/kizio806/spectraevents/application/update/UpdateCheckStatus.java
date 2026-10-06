package io.github.kizio806.spectraevents.application.update;

/** Distinguishes a confirmed version result from a check that could not be completed. */
public enum UpdateCheckStatus {
  NOT_CHECKED,
  UP_TO_DATE,
  UPDATE_AVAILABLE,
  FAILED
}
