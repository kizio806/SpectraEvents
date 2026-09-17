package dev.spectraevents.core.event.lifecycle;

import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.Objects;

/** Signals that a running event instance failed. */
public record EventInstanceFailed(EventInstanceId eventInstanceId) implements EventLifecycleEvent {
  public EventInstanceFailed {
    Objects.requireNonNull(eventInstanceId, "eventInstanceId");
  }

  @Override
  public EventLifecycleState state() {
    return EventLifecycleState.FAILED;
  }
}
