package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

final class EngineContext {
  private final EventInstanceRepository repository;
  private final EventDefinitionRegistry definitionRegistry;
  private final EventTaskScheduler scheduler;
  private final PlatformActionPort platformActionPort;
  private final EventRuntimeStateStore stateStore;
  private final EventZoneRegistry zoneRegistry = new EventZoneRegistry();
  private final EventCheckpointService checkpointService;

  private final ConcurrentHashMap<EventInstanceId, ReentrantLock> instanceLocks =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<EventInstanceId, EventDefinition> definitionSnapshots =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<EventInstanceId, Set<CompletableFuture<Boolean>>>
      observedActionCompletions = new ConcurrentHashMap<>();

  public EngineContext(
      EventInstanceRepository repository,
      EventDefinitionRegistry definitionRegistry,
      EventTaskScheduler scheduler,
      PlatformActionPort platformActionPort,
      EventRuntimeStateStore stateStore) {
    this.repository = repository;
    this.definitionRegistry = definitionRegistry;
    this.scheduler = scheduler;
    this.platformActionPort = platformActionPort;
    this.stateStore = stateStore;
    this.checkpointService =
        new EventCheckpointService(repository, scheduler, stateStore, platformActionPort);
  }

  public EventInstanceRepository repository() {
    return repository;
  }

  public EventDefinitionRegistry definitionRegistry() {
    return definitionRegistry;
  }

  public EventTaskScheduler scheduler() {
    return scheduler;
  }

  public PlatformActionPort platformActionPort() {
    return platformActionPort;
  }

  public EventRuntimeStateStore stateStore() {
    return stateStore;
  }

  public EventZoneRegistry zoneRegistry() {
    return zoneRegistry;
  }

  public EventCheckpointService checkpointService() {
    return checkpointService;
  }

  public ConcurrentHashMap<EventInstanceId, ReentrantLock> instanceLocks() {
    return instanceLocks;
  }

  public ConcurrentHashMap<EventInstanceId, EventDefinition> definitionSnapshots() {
    return definitionSnapshots;
  }

  public ConcurrentHashMap<EventInstanceId, Set<CompletableFuture<Boolean>>>
      observedActionCompletions() {
    return observedActionCompletions;
  }
}
