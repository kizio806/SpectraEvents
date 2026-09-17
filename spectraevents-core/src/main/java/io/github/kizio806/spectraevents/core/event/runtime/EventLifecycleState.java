package io.github.kizio806.spectraevents.core.event.runtime;

/** Lifecycle state of a runtime event instance. */
public enum EventLifecycleState {
  CREATED(false),
  RUNNING(false),
  COMPLETED(true),
  CANCELLED(true),
  FAILED(true);

  private final boolean terminal;

  EventLifecycleState(boolean terminal) {
    this.terminal = terminal;
  }

  /**
   * Reports whether no further lifecycle transition is allowed.
   *
   * @return {@code true} for a terminal state
   */
  public boolean isTerminal() {
    return terminal;
  }
}
