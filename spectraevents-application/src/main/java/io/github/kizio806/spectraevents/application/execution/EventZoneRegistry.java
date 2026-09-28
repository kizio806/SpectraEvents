package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe reservation registry for large event zones. */
final class EventZoneRegistry {
  private final Map<EventInstanceId, Reservation> zones = new ConcurrentHashMap<>();

  public synchronized void reserve(EventInstanceId instanceId, EventZone requested) {
    reserve(instanceId, new EventDefinitionId("legacy"), requested, 3);
  }

  public synchronized void reserve(
      EventInstanceId instanceId,
      EventDefinitionId definitionId,
      EventZone requested,
      int maxActivePerWorld) {
    Objects.requireNonNull(instanceId, "instanceId");
    Objects.requireNonNull(definitionId, "definitionId");
    Objects.requireNonNull(requested, "requested");
    if (maxActivePerWorld < 1) {
      throw new IllegalArgumentException("maxActivePerWorld must be positive");
    }
    Reservation current = zones.get(instanceId);
    if (current != null
        && requested.equals(current.zone())
        && definitionId.equals(current.definitionId())) {
      return;
    }
    long inWorld =
        zones.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(instanceId))
            .map(Map.Entry::getValue)
            .filter(existing -> existing.definitionId().equals(definitionId))
            .map(Reservation::zone)
            .filter(existing -> existing.world().equals(requested.world()))
            .count();
    if (inWorld >= maxActivePerWorld) {
      throw new IllegalStateException(
          "Refusing to start event definition '"
              + definitionId.value()
              + "': world '"
              + requested.world()
              + "' already has its maximum of "
              + maxActivePerWorld
              + " active zones");
    }
    boolean overlap =
        zones.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(instanceId))
            .map(Map.Entry::getValue)
            .map(Reservation::zone)
            .anyMatch(requested::overlaps);
    if (overlap) {
      throw new IllegalStateException(
          "Refusing to start large event: requested zone overlaps an active zone");
    }
    zones.put(instanceId, new Reservation(definitionId, requested));
  }

  public void release(EventInstanceId instanceId) {
    zones.remove(Objects.requireNonNull(instanceId, "instanceId"));
  }

  public Optional<EventZone> find(EventInstanceId instanceId) {
    return Optional.ofNullable(zones.get(Objects.requireNonNull(instanceId, "instanceId")))
        .map(Reservation::zone);
  }

  private record Reservation(EventDefinitionId definitionId, EventZone zone) {}
}
