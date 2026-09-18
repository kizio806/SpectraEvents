package io.github.kizio806.spectraevents.application.asset.delivery;

/** Neutral state tracking for player's resource pack delivery. */
public enum PlayerResourcePackState {
  NOT_REQUESTED,
  REQUESTED,
  ACCEPTED,
  DOWNLOADED,
  LOADED,
  DECLINED,
  FAILED
}
