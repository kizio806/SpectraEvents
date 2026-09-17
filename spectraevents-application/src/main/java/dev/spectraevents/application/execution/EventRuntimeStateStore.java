package dev.spectraevents.application.execution;

import dev.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe store for per-instance runtime state. */
public final class EventRuntimeStateStore {
  private final Map<EventInstanceId, EventRuntimeState> states = new ConcurrentHashMap<>();

  public EventRuntimeState getOrCreate(EventInstanceId instanceId) {
    return states.computeIfAbsent(instanceId, EventRuntimeState::new);
  }

  public Optional<EventRuntimeState> get(EventInstanceId instanceId) {
    return Optional.ofNullable(states.get(instanceId));
  }

  public void remove(EventInstanceId instanceId) {
    states.remove(instanceId);
  }

  public void clear() {
    states.clear();
  }
}
