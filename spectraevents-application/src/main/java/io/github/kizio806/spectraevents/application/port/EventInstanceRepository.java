package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
   * Persists state durably without blocking a platform-owned thread. The returned future completes
   * only after the write has committed; callers must wait for it before an irreversible action.
   */
  default CompletableFuture<Void> saveStateDurablyAsync(
      io.github.kizio806.spectraevents.application.execution.EventRuntimeState state) {
    try {
      saveStateDurably(state);
      return CompletableFuture.completedFuture(null);
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
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

  /** Stores a new instance and its initial state without blocking a platform-owned thread. */
  default CompletableFuture<Void> saveWithStateDurablyAsync(
      EventInstance eventInstance,
      io.github.kizio806.spectraevents.application.execution.EventRuntimeState state) {
    try {
      saveWithStateDurably(eventInstance, state);
      return CompletableFuture.completedFuture(null);
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }

  /** Finds the runtime state by identity, if previously saved. */
  default Optional<io.github.kizio806.spectraevents.application.execution.EventRuntimeState>
      findState(EventInstanceId eventInstanceId) {
    return Optional.empty();
  }

  /** Flushes pending writes and releases repository resources. */
  default void close() {}
}
