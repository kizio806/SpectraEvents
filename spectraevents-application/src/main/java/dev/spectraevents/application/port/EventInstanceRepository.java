package dev.spectraevents.application.port;

import dev.spectraevents.core.event.runtime.EventInstance;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import java.util.Optional;

/** Application-owned storage boundary for runtime event instances. */
public interface EventInstanceRepository {
  /** Stores a new instance or replaces the state currently stored under the same identity. */
  void save(EventInstance eventInstance);

  /** Finds an event instance by identity. */
  Optional<EventInstance> findById(EventInstanceId eventInstanceId);

  /** Returns a stable snapshot of all currently stored instances. */
  List<EventInstance> findAll();

  /** Removes an event instance if present. */
  boolean remove(EventInstanceId eventInstanceId);

  /**
   * Stores the runtime state associated with the event instance. Useful for persisting mutable
   * state like health or claims without overwriting the base instance.
   */
  default void saveState(dev.spectraevents.application.execution.EventRuntimeState state) {}

  /** Finds the runtime state by identity, if previously saved. */
  default Optional<dev.spectraevents.application.execution.EventRuntimeState> findState(
      EventInstanceId eventInstanceId) {
    return Optional.empty();
  }
}
