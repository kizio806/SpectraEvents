package dev.spectraevents.core.event.runtime;

import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

/** Raised when an event instance is asked to perform an illegal lifecycle transition. */
public final class InvalidEventLifecycleTransitionException extends IllegalStateException {
  @Serial private static final long serialVersionUID = 1L;

  private final UUID eventInstanceIdValue;
  private final EventLifecycleState currentState;
  private final EventLifecycleState requestedState;

  /**
   * Creates an invalid-transition exception.
   *
   * @param eventInstanceId affected instance
   * @param currentState state before the rejected transition
   * @param requestedState requested destination state
   */
  public InvalidEventLifecycleTransitionException(
      EventInstanceId eventInstanceId,
      EventLifecycleState currentState,
      EventLifecycleState requestedState) {
    super(
        "Event instance %s cannot transition from %s to %s"
            .formatted(eventInstanceId, currentState, requestedState));
    this.eventInstanceIdValue = Objects.requireNonNull(eventInstanceId, "eventInstanceId").value();
    this.currentState = Objects.requireNonNull(currentState, "currentState");
    this.requestedState = Objects.requireNonNull(requestedState, "requestedState");
  }

  public EventInstanceId eventInstanceId() {
    return new EventInstanceId(eventInstanceIdValue);
  }

  public EventLifecycleState currentState() {
    return currentState;
  }

  public EventLifecycleState requestedState() {
    return requestedState;
  }
}
