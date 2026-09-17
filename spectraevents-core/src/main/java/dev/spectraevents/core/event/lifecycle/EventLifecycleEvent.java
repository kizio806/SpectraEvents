package dev.spectraevents.core.event.lifecycle;

import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.core.event.runtime.EventLifecycleState;

/** Domain event raised by a successful event-instance lifecycle transition. */
public sealed interface EventLifecycleEvent
    permits EventInstanceCancelled,
        EventInstanceCompleted,
        EventInstanceFailed,
        EventInstanceStarted,
        EventPhaseChanged {
  EventInstanceId eventInstanceId();

  EventLifecycleState state();
}
