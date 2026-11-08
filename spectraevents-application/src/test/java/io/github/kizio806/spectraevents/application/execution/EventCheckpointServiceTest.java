package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventCheckpointServiceTest {

  private EventInstanceId instanceId;
  private EventDefinitionId definitionId;
  private EventRuntimeStateStore stateStore;
  private FakeRepository repository;
  private FakeScheduler scheduler;
  private FakePlatformActionPort actionPort;
  private EventCheckpointService service;

  @BeforeEach
  void setUp() {
    instanceId = EventInstanceId.generate();
    definitionId = new EventDefinitionId("test_event");
    stateStore = new EventRuntimeStateStore();
    repository = new FakeRepository();
    scheduler = new FakeScheduler();
    actionPort = new FakePlatformActionPort();
    service = new EventCheckpointService(repository, scheduler, stateStore, actionPort);
  }

  @Test
  void flushDurablyDelegatesToRepository() {
    EventRuntimeState state = stateStore.getOrCreate(instanceId);
    service.flushDurably(state);
    assertTrue(repository.savedDurably);
    assertEquals(state, repository.lastSavedDurableState);
  }

  @Test
  void startSchedulesGlobalAndExecutesCheckpointSuccessfully() {
    EventInstance instance =
        EventInstance.reconstitute(
            instanceId, definitionId, EventLifecycleState.RUNNING, new PhaseId("p1"));
    repository.instance = instance;
    EventRuntimeState state = stateStore.getOrCreate(instanceId);

    service.start(instanceId);

    assertEquals(1, scheduler.globalTasks.size());
    assertEquals(Duration.ofSeconds(1), scheduler.lastInterval);

    // Run the scheduled checkpoint task
    Runnable task = scheduler.globalTasks.get(0);
    task.run();

    assertTrue(repository.savedState);
    assertEquals(state, repository.lastSavedState);
    assertTrue(actionPort.hudRefreshed);
    assertEquals(instance, actionPort.lastHudInstance);
    assertEquals(state, actionPort.lastHudState);

    // Should have rescheduled next checkpoint
    assertEquals(2, scheduler.globalTasks.size());
  }

  @Test
  void doesNotSaveOrRescheduleWhenInstanceMissingOrNotRunning() {
    // 1. Missing instance
    repository.instance = null;
    service.start(instanceId);
    assertEquals(1, scheduler.globalTasks.size());

    scheduler.globalTasks.get(0).run();
    assertFalse(repository.savedState);
    assertFalse(actionPort.hudRefreshed);
    assertEquals(1, scheduler.globalTasks.size());

    // 2. Instance not in RUNNING state
    repository.instance =
        EventInstance.reconstitute(
            instanceId, definitionId, EventLifecycleState.COMPLETED, new PhaseId("p1"));
    scheduler.globalTasks.get(0).run();
    assertFalse(repository.savedState);
    assertFalse(actionPort.hudRefreshed);
    assertEquals(1, scheduler.globalTasks.size());
  }

  @Test
  void fallsBackToRegionalSchedulerWhenGlobalSchedulingUnsupported() {
    scheduler.throwUnsupportedOnGlobal = true;
    EventInstance instance =
        EventInstance.reconstitute(
            instanceId, definitionId, EventLifecycleState.RUNNING, new PhaseId("p1"));
    repository.instance = instance;
    stateStore.getOrCreate(instanceId);

    service.start(instanceId);

    assertEquals(1, scheduler.regionalTasks.size());
    assertEquals(Duration.ofSeconds(1), scheduler.lastInterval);

    scheduler.regionalTasks.get(0).run();
    assertTrue(repository.savedState);
    assertTrue(actionPort.hudRefreshed);
    assertEquals(2, scheduler.regionalTasks.size());
  }

  private static final class FakeRepository implements EventInstanceRepository {
    EventInstance instance;
    boolean savedState;
    EventRuntimeState lastSavedState;
    boolean savedDurably;
    EventRuntimeState lastSavedDurableState;

    @Override
    public Optional<EventInstance> findById(EventInstanceId id) {
      return Optional.ofNullable(instance);
    }

    @Override
    public void save(EventInstance eventInstance) {}

    @Override
    public List<EventInstance> findAll() {
      return List.of();
    }

    @Override
    public boolean remove(EventInstanceId eventInstanceId) {
      return true;
    }

    @Override
    public void saveState(EventRuntimeState state) {
      this.savedState = true;
      this.lastSavedState = state;
    }

    @Override
    public void saveStateDurably(EventRuntimeState state) {
      this.savedDurably = true;
      this.lastSavedDurableState = state;
    }
  }

  private static final class FakeScheduler implements EventTaskScheduler {
    boolean throwUnsupportedOnGlobal;
    Duration lastInterval;
    final List<Runnable> globalTasks = new ArrayList<>();
    final List<Runnable> regionalTasks = new ArrayList<>();

    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
      this.lastInterval = delay;
      this.regionalTasks.add(task);
    }

    @Override
    public void scheduleGlobal(Duration delay, Runnable task) {
      if (throwUnsupportedOnGlobal) {
        throw new UnsupportedOperationException("Folia region scheduler required");
      }
      this.lastInterval = delay;
      this.globalTasks.add(task);
    }

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}
  }

  private static final class FakePlatformActionPort implements PlatformActionPort {
    boolean hudRefreshed;
    EventInstance lastHudInstance;
    EventRuntimeState lastHudState;

    @Override
    public CompletableFuture<Boolean> executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public void refreshHud(EventInstance instance, EventRuntimeState state) {
      this.hudRefreshed = true;
      this.lastHudInstance = instance;
      this.lastHudState = state;
    }
  }
}
