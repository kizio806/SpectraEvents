package dev.spectraevents.core.event.lifecycle;

import dev.spectraevents.core.event.definition.EventDefinitionId;
import dev.spectraevents.core.event.phase.PhaseId;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.Objects;

/** Domain event emitted when a running event instance transitions between phases. */
public record EventPhaseChanged(
    EventInstanceId eventInstanceId,
    EventDefinitionId definitionId,
    PhaseId previousPhase,
    PhaseId currentPhase)
    implements EventLifecycleEvent {

  public EventPhaseChanged {
    Objects.requireNonNull(eventInstanceId, "eventInstanceId");
    Objects.requireNonNull(definitionId, "definitionId");
    // previousPhase can be null if it's the initial transition on start
    Objects.requireNonNull(currentPhase, "currentPhase");
  }

  @Override
  public EventLifecycleState state() {
    return EventLifecycleState.RUNNING;
  }
}
