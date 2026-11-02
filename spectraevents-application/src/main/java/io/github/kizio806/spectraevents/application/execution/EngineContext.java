package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

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

  public ReentrantLock getOrCreateInstanceLock(EventInstanceId instanceId) {
    Objects.requireNonNull(instanceId, "instanceId");
    return instanceLocks.computeIfAbsent(instanceId, ignored -> new ReentrantLock());
  }

  public <T> T withInstanceLock(EventInstanceId instanceId, Supplier<T> action) {
    Objects.requireNonNull(action, "action");
    ReentrantLock lock = getOrCreateInstanceLock(instanceId);
    lock.lock();
    try {
      return action.get();
    } finally {
      lock.unlock();
    }
  }

  public void withInstanceLock(EventInstanceId instanceId, Runnable action) {
    Objects.requireNonNull(action, "action");
    ReentrantLock lock = getOrCreateInstanceLock(instanceId);
    lock.lock();
    try {
      action.run();
    } finally {
      lock.unlock();
    }
  }

  public EventDefinition getDefinitionSnapshot(EventInstanceId instanceId) {
    Objects.requireNonNull(instanceId, "instanceId");
    return definitionSnapshots.get(instanceId);
  }

  public EventDefinition computeDefinitionSnapshotIfAbsent(
      EventInstanceId instanceId, Function<EventInstanceId, EventDefinition> mappingFunction) {
    Objects.requireNonNull(instanceId, "instanceId");
    Objects.requireNonNull(mappingFunction, "mappingFunction");
    return definitionSnapshots.computeIfAbsent(instanceId, mappingFunction);
  }

  public void putDefinitionSnapshot(EventInstanceId instanceId, EventDefinition definition) {
    Objects.requireNonNull(instanceId, "instanceId");
    Objects.requireNonNull(definition, "definition");
    definitionSnapshots.put(instanceId, definition);
  }

  public void removeDefinitionSnapshot(EventInstanceId instanceId) {
    Objects.requireNonNull(instanceId, "instanceId");
    definitionSnapshots.remove(instanceId);
  }

  public Set<CompletableFuture<Boolean>> getOrCreateObservedActionCompletions(
      EventInstanceId instanceId) {
    Objects.requireNonNull(instanceId, "instanceId");
    return observedActionCompletions.computeIfAbsent(
        instanceId, ignored -> ConcurrentHashMap.newKeySet());
  }

  public void removeObservedActionCompletions(EventInstanceId instanceId) {
    Objects.requireNonNull(instanceId, "instanceId");
    observedActionCompletions.remove(instanceId);
  }

  public void clearRuntimeState() {
    instanceLocks.clear();
    definitionSnapshots.clear();
    observedActionCompletions.clear();
  }
}
