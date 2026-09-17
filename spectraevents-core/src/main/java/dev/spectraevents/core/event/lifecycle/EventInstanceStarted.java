package dev.spectraevents.core.event.lifecycle;

import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.Objects;

/** Signals that an event instance entered the running state. */
public record EventInstanceStarted(EventInstanceId eventInstanceId) implements EventLifecycleEvent {
  public EventInstanceStarted {
    Objects.requireNonNull(eventInstanceId, "eventInstanceId");
  }

  @Override
  public EventLifecycleState state() {
    return EventLifecycleState.RUNNING;
  }
}
