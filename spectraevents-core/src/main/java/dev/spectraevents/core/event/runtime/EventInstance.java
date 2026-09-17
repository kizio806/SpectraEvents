package dev.spectraevents.core.event.runtime;

import dev.spectraevents.core.event.definition.EventDefinition;
import dev.spectraevents.core.event.definition.EventDefinitionId;
import dev.spectraevents.core.event.lifecycle.EventInstanceCancelled;
import dev.spectraevents.core.event.lifecycle.EventInstanceCompleted;
import dev.spectraevents.core.event.lifecycle.EventInstanceFailed;
import dev.spectraevents.core.event.lifecycle.EventInstanceStarted;
import dev.spectraevents.core.event.lifecycle.EventLifecycleEvent;
import dev.spectraevents.core.event.lifecycle.EventPhaseChanged;
import dev.spectraevents.core.event.phase.PhaseDefinition;
import dev.spectraevents.core.event.phase.PhaseId;
import java.util.Objects;
import java.util.Optional;

/** Immutable domain state machine for one running event. */
public final class EventInstance {
  private final EventInstanceId id;
  private final EventDefinitionId definitionId;
  private final EventLifecycleState state;
  private final PhaseId currentPhase;

  private EventInstance(
      EventInstanceId id,
      EventDefinitionId definitionId,
      EventLifecycleState state,
      PhaseId currentPhase) {
    this.id = Objects.requireNonNull(id, "id");
    this.definitionId = Objects.requireNonNull(definitionId, "definitionId");
    this.state = Objects.requireNonNull(state, "state");
    // currentPhase can be null if not started yet
    this.currentPhase = currentPhase;
  }

  /**
   * Creates an event instance before it has started.
   *
   * @param id stable instance identity
   * @param definitionId stable definition identity
   * @return event instance in {@link EventLifecycleState#CREATED}
   */
  public static EventInstance create(EventInstanceId id, EventDefinitionId definitionId) {
    return new EventInstance(id, definitionId, EventLifecycleState.CREATED, null);
  }

  /**
   * Reconstitutes a persisted event instance from storage.
   *
   * @param id stable instance identity
   * @param definitionId stable definition identity
   * @param state lifecycle state
   * @param currentPhase current phase or null
   * @return reconstituted event instance
   */
  public static EventInstance reconstitute(
      EventInstanceId id,
      EventDefinitionId definitionId,
      EventLifecycleState state,
      PhaseId currentPhase) {
    return new EventInstance(id, definitionId, state, currentPhase);
  }

  public EventInstanceId id() {
    return id;
  }

  public EventDefinitionId definitionId() {
    return definitionId;
  }

  public EventLifecycleState state() {
    return state;
  }

  public Optional<PhaseId> currentPhase() {
    return Optional.ofNullable(currentPhase);
  }

  /**
   * Starts a newly created event instance.
   *
   * @param definition the definition of the event
   * @return new instance state and the event raised by the transition
   */
  public EventLifecycleTransition start(EventDefinition definition) {
    validateDefinitionMatch(definition);
    if (state != EventLifecycleState.CREATED) {
      throw new InvalidEventLifecycleTransitionException(id, state, EventLifecycleState.RUNNING);
    }

    EventInstance startedInstance =
        new EventInstance(id, definitionId, EventLifecycleState.RUNNING, definition.initialPhase());

    return new EventLifecycleTransition(startedInstance, new EventInstanceStarted(id));
  }

  /**
   * Transitions a running event instance to a new phase.
   *
   * @param definition the definition of the event
   * @param targetPhase the target phase
   * @return new instance state and the event raised by the transition
   */
  public EventLifecycleTransition transitionPhase(EventDefinition definition, PhaseId targetPhase) {
    validateDefinitionMatch(definition);
    Objects.requireNonNull(targetPhase, "targetPhase");

    if (state != EventLifecycleState.RUNNING) {
      throw new InvalidPhaseTransitionException(
          id, definitionId, currentPhase, targetPhase, "Event is not in RUNNING state");
    }

    if (currentPhase == null) {
      throw new InvalidPhaseTransitionException(
          id, definitionId, currentPhase, targetPhase, "Event has no current phase");
    }

    PhaseDefinition phaseDef =
        definition
            .phase(currentPhase)
            .orElseThrow(
                () ->
                    new InvalidPhaseTransitionException(
                        id,
                        definitionId,
                        currentPhase,
                        targetPhase,
                        "Current phase is unknown to definition"));

    if (!definition.phase(targetPhase).isPresent()) {
      throw new InvalidPhaseTransitionException(
          id, definitionId, currentPhase, targetPhase, "Target phase is unknown to definition");
    }

    if (!phaseDef.allowsTransitionTo(targetPhase)) {
      throw new InvalidPhaseTransitionException(
          id,
          definitionId,
          currentPhase,
          targetPhase,
          "Transition is not allowed by phase definition");
    }

    EventInstance nextInstance =
        new EventInstance(id, definitionId, EventLifecycleState.RUNNING, targetPhase);

    return new EventLifecycleTransition(
        nextInstance, new EventPhaseChanged(id, definitionId, currentPhase, targetPhase));
  }

  /**
   * Completes a running event instance successfully.
   *
   * @return new instance state and the event raised by the transition
   */
  public EventLifecycleTransition complete() {
    return transition(
        EventLifecycleState.RUNNING, EventLifecycleState.COMPLETED, new EventInstanceCompleted(id));
  }

  /**
   * Cancels a running event instance.
   *
   * @return new instance state and the event raised by the transition
   */
  public EventLifecycleTransition cancel() {
    return transition(
        EventLifecycleState.RUNNING, EventLifecycleState.CANCELLED, new EventInstanceCancelled(id));
  }

  /**
   * Marks a running event instance as failed.
   *
   * @return new instance state and the event raised by the transition
   */
  public EventLifecycleTransition fail() {
    return transition(
        EventLifecycleState.RUNNING, EventLifecycleState.FAILED, new EventInstanceFailed(id));
  }

  private EventLifecycleTransition transition(
      EventLifecycleState requiredState,
      EventLifecycleState nextState,
      EventLifecycleEvent lifecycleEvent) {
    if (state != requiredState) {
      throw new InvalidEventLifecycleTransitionException(id, state, nextState);
    }
    return new EventLifecycleTransition(
        new EventInstance(id, definitionId, nextState, currentPhase), lifecycleEvent);
  }

  private void validateDefinitionMatch(EventDefinition definition) {
    Objects.requireNonNull(definition, "definition");
    if (!this.definitionId.equals(definition.id())) {
      throw new InvalidPhaseTransitionException(
          id, this.definitionId, currentPhase, null, "Definition ID mismatch");
    }
  }
}
