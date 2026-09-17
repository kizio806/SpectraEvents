package dev.spectraevents.core.event.lifecycle;

import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.Objects;

/** Signals that a running event instance was cancelled. */
public record EventInstanceCancelled(EventInstanceId eventInstanceId)
    implements EventLifecycleEvent {
  public EventInstanceCancelled {
    Objects.requireNonNull(eventInstanceId, "eventInstanceId");
  }

  @Override
  public EventLifecycleState state() {
    return EventLifecycleState.CANCELLED;
  }
}
