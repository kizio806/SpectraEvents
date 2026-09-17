package io.github.kizio806.spectraevents.core.event.lifecycle;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;

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
