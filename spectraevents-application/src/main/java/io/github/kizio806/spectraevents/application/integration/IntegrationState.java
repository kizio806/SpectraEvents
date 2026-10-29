package io.github.kizio806.spectraevents.application.integration;

/** Lifecycle status of an optional external plugin integration. */
public enum IntegrationState {
  ABSENT,
  AVAILABLE,
  ACTIVE,
  DEGRADED,
  FAILED;

  public boolean isActive() {
    return this == ACTIVE;
  }
}
