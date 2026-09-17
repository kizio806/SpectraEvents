package io.github.kizio806.spectraevents.core.event.lifecycle;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.Objects;

/** Signals that an event instance completed successfully. */
public record EventInstanceCompleted(EventInstanceId eventInstanceId)
    implements EventLifecycleEvent {
  public EventInstanceCompleted {
    Objects.requireNonNull(eventInstanceId, "eventInstanceId");
  }

  @Override
  public EventLifecycleState state() {
    return EventLifecycleState.COMPLETED;
  }
}
