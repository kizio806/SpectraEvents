package io.github.kizio806.spectraevents.application.service;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntityReconciliationService {
  private final PlatformEntityReconcilerPort reconcilerPort;
  private final EventInstanceRepository repository;
  private final EventRuntimeStateStore stateStore;

  public EntityReconciliationService(
      PlatformEntityReconcilerPort reconcilerPort,
      EventInstanceRepository repository,
      EventRuntimeStateStore stateStore) {
    this.reconcilerPort = reconcilerPort;
    this.repository = repository;
    this.stateStore = stateStore;
  }

  public EntityReconciliationReport reconcileAll() {
    List<PlatformEntityReconcilerPort.DiscoveredEntity> discovered =
        reconcilerPort.scanLoadedEntities();

    Map<EventInstanceId, List<PlatformEntityReconcilerPort.DiscoveredEntity>> byInstance =
        discovered.stream()
            .collect(
                Collectors.groupingBy(PlatformEntityReconcilerPort.DiscoveredEntity::instanceId));

    long recovered = 0;
    long reconnected = 0;
    long orphans = 0;
    long failed = 0;
    long warnings = 0;

    for (Map.Entry<EventInstanceId, List<PlatformEntityReconcilerPort.DiscoveredEntity>> entry :
        byInstance.entrySet()) {
      EventInstanceId id = entry.getKey();
      List<PlatformEntityReconcilerPort.DiscoveredEntity> entities = entry.getValue();

      Optional<EventInstance> instanceOpt = repository.findById(id);
      if (instanceOpt.isEmpty()) {
        // Orphan
        for (var entity : entities) {
          reconcilerPort.removeEntity(entity.platformReference());
          orphans++;
        }
        continue;
      }

      EventInstance instance = instanceOpt.get();
      if (instance.state() != EventLifecycleState.RUNNING) {
        // Terminal instance, clean up entities
        for (var entity : entities) {
          reconcilerPort.removeEntity(entity.platformReference());
          orphans++;
        }
        continue;
      }

      // If RUNNING and state exists, keep/reconnect them
      EventRuntimeState state = stateStore.getOrCreate(id);
      recovered++;

      reconcilerPort.restoreInstance(id, entities);

      for (var entity : entities) {
        if ("boss".equals(entity.role())) {
          state.setBossEntityId(entity.platformReference().toString());
        }
        reconnected++;
      }
    }

    return new EntityReconciliationReport(recovered, reconnected, orphans, failed, warnings);
  }
}
