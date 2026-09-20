package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
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
  default void saveState(
      io.github.kizio806.spectraevents.application.execution.EventRuntimeState state) {}

  /**
   * Stores runtime state and returns only after the storage implementation has durably accepted the
   * write. Implementations that do not buffer writes may use the default implementation.
   */
  default void saveStateDurably(
      io.github.kizio806.spectraevents.application.execution.EventRuntimeState state) {
    saveState(state);
  }

  /**
   * Stores a newly-created instance and its initial runtime state as one durable unit when the
   * backing store supports transactions.
   */
  default void saveWithStateDurably(
      EventInstance eventInstance,
      io.github.kizio806.spectraevents.application.execution.EventRuntimeState state) {
    save(eventInstance);
    saveStateDurably(state);
  }

  /** Finds the runtime state by identity, if previously saved. */
  default Optional<io.github.kizio806.spectraevents.application.execution.EventRuntimeState>
      findState(EventInstanceId eventInstanceId) {
    return Optional.empty();
  }

  /** Flushes pending writes and releases repository resources. */
  default void close() {}
}
