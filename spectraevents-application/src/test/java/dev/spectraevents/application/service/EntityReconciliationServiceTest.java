package dev.spectraevents.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.spectraevents.application.execution.EventRuntimeState;
import dev.spectraevents.application.execution.EventRuntimeStateStore;
import dev.spectraevents.application.port.EventInstanceRepository;
import dev.spectraevents.application.port.PlatformEntityReconcilerPort;
import dev.spectraevents.core.event.runtime.EventInstance;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntityReconciliationServiceTest {

  private EntityReconciliationService service;
  private EventInstanceRepository repository;
  private EventRuntimeStateStore stateStore;
  private StubPlatformReconciler platformReconciler;

  @BeforeEach
  void setUp() {
    repository = new StubEventInstanceRepository();
    stateStore = new EventRuntimeStateStore();
    platformReconciler = new StubPlatformReconciler();
    service = new EntityReconciliationService(platformReconciler, repository, stateStore);
  }

  @Test
  void shouldRemoveOrphanedEntities() {
    UUID orphanEntityId = UUID.randomUUID();
    EventInstanceId orphanInstanceId = new EventInstanceId(UUID.randomUUID());
    platformReconciler.addEntity(orphanEntityId, orphanInstanceId, "boss", null);

    EntityReconciliationReport report = service.reconcileAll();

    assertEquals(0, report.instancesRecovered());
    assertEquals(0, report.entitiesReconnected());
    assertEquals(1, report.orphansRemoved());
    assertTrue(platformReconciler.wasRemoved(orphanEntityId));
  }

  @Test
  void shouldReconnectValidEntities() {
    EventInstanceId instanceId = new EventInstanceId(UUID.randomUUID());
    EventInstance instance =
        EventInstance.reconstitute(
            instanceId,
            new dev.spectraevents.core.event.definition.EventDefinitionId("test"),
            dev.spectraevents.core.event.runtime.EventLifecycleState.RUNNING,
            new dev.spectraevents.core.event.phase.PhaseId("active"));
    repository.save(instance);
    EventRuntimeState state = stateStore.getOrCreate(instanceId);
    state.setBossEntityId(UUID.randomUUID().toString()); // Stale id
    // No need to save in StubEventRuntimeStateStore because we use the real one which modifies the
    // instance directly in memory

    UUID newEntityId = UUID.randomUUID();
    platformReconciler.addEntity(newEntityId, instanceId, "boss", null);

    EntityReconciliationReport report = service.reconcileAll();

    assertEquals(1, report.instancesRecovered());
    assertEquals(1, report.entitiesReconnected());
    assertEquals(0, report.orphansRemoved());
    assertEquals(
        newEntityId.toString(), stateStore.getOrCreate(instanceId).bossEntityId().orElse(null));
  }

  private static class StubPlatformReconciler implements PlatformEntityReconcilerPort {
    private final List<DiscoveredEntity> entities = new java.util.ArrayList<>();
    private final java.util.Set<Object> removed = new java.util.HashSet<>();

    void addEntity(UUID entityId, EventInstanceId instanceId, String role, String partId) {
      entities.add(new DiscoveredEntity(entityId, instanceId, role, partId));
    }

    boolean wasRemoved(Object entityId) {
      return removed.contains(entityId);
    }

    @Override
    public List<DiscoveredEntity> scanLoadedEntities() {
      return entities;
    }

    @Override
    public void removeEntity(Object platformEntityReference) {
      removed.add(platformEntityReference);
    }

    @Override
    public void restoreInstance(EventInstanceId instanceId, List<DiscoveredEntity> entities) {}
  }

  private static class StubEventInstanceRepository implements EventInstanceRepository {
    private final java.util.Map<EventInstanceId, EventInstance> store = new java.util.HashMap<>();

    @Override
    public void save(EventInstance instance) {
      store.put(instance.id(), instance);
    }

    @Override
    public void saveState(EventRuntimeState state) {}

    @Override
    public Optional<EventRuntimeState> findState(EventInstanceId id) {
      return Optional.empty();
    }

    @Override
    public Optional<EventInstance> findById(EventInstanceId id) {
      return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<EventInstance> findAll() {
      return List.copyOf(store.values());
    }

    @Override
    public boolean remove(EventInstanceId id) {
      return store.remove(id) != null;
    }
  }
}
