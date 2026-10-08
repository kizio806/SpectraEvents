package io.github.kizio806.spectraevents.application.execution;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Mutable, bounded ownership set for a single event wave. */
final class TrackedWave {
  private final String id;
  private final Set<UUID> entities = ConcurrentHashMap.newKeySet();

  TrackedWave(String id) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("wave id must not be blank");
    }
    this.id = id;
  }

  String id() {
    return id;
  }

  void add(UUID entityId) {
    entities.add(Objects.requireNonNull(entityId, "entityId"));
  }

  boolean remove(UUID entityId) {
    return entities.remove(Objects.requireNonNull(entityId, "entityId"));
  }

  boolean isCleared() {
    return entities.isEmpty();
  }

  int size() {
    return entities.size();
  }
}
