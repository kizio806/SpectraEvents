package dev.spectraevents.application.repository;

import dev.spectraevents.application.port.EventInstanceRepository;
import dev.spectraevents.core.event.runtime.EventInstance;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** In-memory event-instance repository for the runtime's initial application composition. */
public final class InMemoryEventInstanceRepository implements EventInstanceRepository {
  private final ConcurrentMap<EventInstanceId, EventInstance> eventInstances =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<
          EventInstanceId, dev.spectraevents.application.execution.EventRuntimeState>
      eventStates = new ConcurrentHashMap<>();

  @Override
  public void save(EventInstance eventInstance) {
    EventInstance nonNullEventInstance = Objects.requireNonNull(eventInstance, "eventInstance");
    eventInstances.put(nonNullEventInstance.id(), nonNullEventInstance);
  }

  @Override
  public Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
    return Optional.ofNullable(
        eventInstances.get(Objects.requireNonNull(eventInstanceId, "eventInstanceId")));
  }

  @Override
  public List<EventInstance> findAll() {
    return List.copyOf(eventInstances.values());
  }

  @Override
  public boolean remove(EventInstanceId eventInstanceId) {
    eventStates.remove(Objects.requireNonNull(eventInstanceId, "eventInstanceId"));
    return eventInstances.remove(eventInstanceId) != null;
  }

  @Override
  public void saveState(dev.spectraevents.application.execution.EventRuntimeState state) {
    eventStates.put(Objects.requireNonNull(state.instanceId(), "instanceId"), state);
  }

  @Override
  public Optional<dev.spectraevents.application.execution.EventRuntimeState> findState(
      EventInstanceId eventInstanceId) {
    return Optional.ofNullable(
        eventStates.get(Objects.requireNonNull(eventInstanceId, "eventInstanceId")));
  }
}
