package io.github.kizio806.spectraevents.core.event.runtime;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

/** Raised when an event instance is asked to perform an illegal phase transition. */
public final class InvalidPhaseTransitionException extends IllegalStateException {
  @Serial private static final long serialVersionUID = 1L;

  private final UUID eventInstanceIdValue;
  private final String definitionIdValue;
  private final String currentPhaseValue;
  private final String requestedPhaseValue;
  private final String reason;

  /**
   * Creates an invalid-phase-transition exception.
   *
   * @param eventInstanceId affected instance
   * @param definitionId expected definition
   * @param currentPhase phase before the rejected transition (may be null if not started)
   * @param requestedPhase requested destination phase
   * @param reason human-readable reason for rejection
   */
  public InvalidPhaseTransitionException(
      EventInstanceId eventInstanceId,
      EventDefinitionId definitionId,
      PhaseId currentPhase,
      PhaseId requestedPhase,
      String reason) {
    super(
        "Event instance %s cannot transition from phase %s to %s. Reason: %s"
            .formatted(
                eventInstanceId,
                currentPhase != null ? currentPhase : "none",
                requestedPhase != null ? requestedPhase : "none",
                reason));
    this.eventInstanceIdValue = Objects.requireNonNull(eventInstanceId, "eventInstanceId").value();
    this.definitionIdValue = Objects.requireNonNull(definitionId, "definitionId").value();
    this.currentPhaseValue = currentPhase != null ? currentPhase.value() : null;
    this.requestedPhaseValue = requestedPhase != null ? requestedPhase.value() : null;
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public EventInstanceId eventInstanceId() {
    return new EventInstanceId(eventInstanceIdValue);
  }

  public EventDefinitionId definitionId() {
    return new EventDefinitionId(definitionIdValue);
  }

  public PhaseId currentPhase() {
    return currentPhaseValue != null ? new PhaseId(currentPhaseValue) : null;
  }

  public PhaseId requestedPhase() {
    return requestedPhaseValue != null ? new PhaseId(requestedPhaseValue) : null;
  }

  public String reason() {
    return reason;
  }
}
