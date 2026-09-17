package io.github.kizio806.spectraevents.core.event.runtime;

import io.github.kizio806.spectraevents.core.event.lifecycle.EventLifecycleEvent;
import java.util.Objects;

/** Result of a successful lifecycle transition. */
public record EventLifecycleTransition(
    EventInstance eventInstance, EventLifecycleEvent lifecycleEvent) {
  /**
   * Creates a transition result and verifies that its state and event describe the same instance.
   *
   * @param eventInstance transitioned instance
   * @param lifecycleEvent event raised by the transition
   */
  public EventLifecycleTransition {
    Objects.requireNonNull(eventInstance, "eventInstance");
    Objects.requireNonNull(lifecycleEvent, "lifecycleEvent");
    if (!eventInstance.id().equals(lifecycleEvent.eventInstanceId())) {
      throw new IllegalArgumentException("Lifecycle event belongs to a different event instance");
    }
    if (eventInstance.state() != lifecycleEvent.state()) {
      throw new IllegalArgumentException("Lifecycle event does not describe the resulting state");
    }
  }
}
