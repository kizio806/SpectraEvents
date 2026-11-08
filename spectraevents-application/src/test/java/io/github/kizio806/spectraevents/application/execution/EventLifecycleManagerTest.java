package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.definition.EventEncounterSettings;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventLifecycleManagerTest {

  private EngineContext context;
  private EventDefinitionRegistry registry;
  private EventLifecycleManager lifecycleManager;
  private EventDefinition sampleDef;

  @BeforeEach
  void setUp() {
    EventInstanceRepository repository = new InMemoryEventInstanceRepository();
    registry = new EventDefinitionRegistry();
    EventTaskScheduler scheduler =
        new EventTaskScheduler() {
          @Override
          public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

          @Override
          public void cancelAll(EventInstanceId eventId) {}

          @Override
          public void cancelAll() {}
        };
    PlatformActionPort actionPort =
        (executionId, action, executionContext) -> CompletableFuture.completedFuture(null);
    EventRuntimeStateStore stateStore = new EventRuntimeStateStore();

    context = new EngineContext(repository, registry, scheduler, actionPort, stateStore);
    lifecycleManager = new EventLifecycleManager(context);

    PhaseId p1 = new PhaseId("p1");
    sampleDef =
        new EventDefinition(
            new EventDefinitionId("sample"),
            p1,
            Map.of(p1, new PhaseDefinition(p1, Set.of())),
            new EventEncounterSettings(10.0, Duration.ofMinutes(1), 5, 1));
    registry.register(sampleDef, "sample.yml");
  }

  @Test
  void testStartAndCancelEvent() {
    EventLocation location = new EventLocation("world", 100.0, 64.0, 100.0, 0f, 0f);
    EventInstance started = lifecycleManager.startEvent(sampleDef, location);
    assertNotNull(started);
    assertEquals(EventLifecycleState.RUNNING, started.state());

    // Definition lookup
    EventDefinition def = lifecycleManager.definitionForInstance(started);
    assertEquals("sample", def.id().value());

    // Cancel
    EventInstance cancelled = lifecycleManager.cancelEvent(started.id());
    assertNotNull(cancelled);
    assertEquals(EventLifecycleState.CANCELLED, cancelled.state());

    // Cancelling again returns null
    assertNull(lifecycleManager.cancelEvent(started.id()));
  }

  @Test
  void testStartAndCompleteEvent() {
    EventLocation location = new EventLocation("world", 200.0, 64.0, 200.0, 0f, 0f);
    EventInstance started = lifecycleManager.startEvent(sampleDef, location);

    EventInstance completed = lifecycleManager.completeEvent(started.id());
    assertNotNull(completed);
    assertEquals(EventLifecycleState.COMPLETED, completed.state());
  }

  @Test
  void testStartAndFailEvent() {
    EventLocation location = new EventLocation("world", 300.0, 64.0, 300.0, 0f, 0f);
    EventInstance started = lifecycleManager.startEvent(sampleDef, location);

    EventInstance failed =
        lifecycleManager.failEvent(started.id(), new RuntimeException("Forced failure"));
    assertNotNull(failed);
    assertEquals(EventLifecycleState.FAILED, failed.state());
  }

  @Test
  void testEncounterRecovery() {
    EventInstance instance = EventInstance.create(EventInstanceId.generate(), sampleDef.id());
    instance = instance.start(sampleDef).eventInstance();

    EventRuntimeState state = context.stateStore().getOrCreate(instance.id());
    state.setEventZone(new EventZone("world", 50.0, 50.0, 10.0));

    lifecycleManager.recoverEncounter(instance, state);
    assertTrue(context.zoneRegistry().find(instance.id()).isPresent());
  }

  @Test
  void testGetDefinitionThrowsForUnknown() {
    assertThrows(IllegalArgumentException.class, () -> lifecycleManager.getDefinition("unknown"));
  }

  @Test
  void testMaxRunningInstancesGuardrail() {
    EventLocation location = new EventLocation("world", 10.0, 64.0, 10.0, 0f, 0f);
    // Populate repository with 64 running instances
    for (int i = 0; i < 64; i++) {
      EventInstance instance =
          EventInstance.reconstitute(
              EventInstanceId.generate(),
              sampleDef.id(),
              EventLifecycleState.RUNNING,
              new PhaseId("p1"));
      context.repository().save(instance);
    }

    var ex =
        assertThrows(
            IllegalStateException.class, () -> lifecycleManager.startEvent(sampleDef, location));
    assertTrue(ex.getMessage().contains("maximum of 64 running instances"));
  }

  @Test
  void testLargeEncounterRequiresEventLocation() {
    var ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> lifecycleManager.startEvent(sampleDef, "invalid_location_string"));
    assertTrue(ex.getMessage().contains("requires an EventLocation"));
  }

  @Test
  void testEncounterRecoveryDeadlineElapsed() {
    EventInstance instance = EventInstance.create(EventInstanceId.generate(), sampleDef.id());
    instance = instance.start(sampleDef).eventInstance();
    context.repository().save(instance);

    EventRuntimeState state = context.stateStore().getOrCreate(instance.id());
    state.setEncounterDeadlineMillis(System.currentTimeMillis() - 5000);

    lifecycleManager.recoverEncounter(instance, state);
    EventInstance updated = context.repository().findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, updated.state());
  }

  @Test
  void testStartEventAsyncFailureCleansUp() {
    EventInstanceRepository delegate = new InMemoryEventInstanceRepository();
    EventInstanceRepository failingRepo =
        new EventInstanceRepository() {
          @Override
          public void save(EventInstance eventInstance) {
            delegate.save(eventInstance);
          }

          @Override
          public Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
            return delegate.findById(eventInstanceId);
          }

          @Override
          public List<EventInstance> findAll() {
            return delegate.findAll();
          }

          @Override
          public boolean remove(EventInstanceId eventInstanceId) {
            return delegate.remove(eventInstanceId);
          }

          @Override
          public void saveState(EventRuntimeState state) {
            delegate.saveState(state);
          }

          @Override
          public Optional<EventRuntimeState> findState(EventInstanceId eventInstanceId) {
            return delegate.findState(eventInstanceId);
          }

          @Override
          public CompletableFuture<Void> saveWithStateDurablyAsync(
              EventInstance instance, EventRuntimeState state) {
            return CompletableFuture.failedFuture(new RuntimeException("DB crash"));
          }
        };

    EngineContext failingContext =
        new EngineContext(
            failingRepo,
            registry,
            context.scheduler(),
            context.platformActionPort(),
            context.stateStore());

    EventLifecycleManager failingManager = new EventLifecycleManager(failingContext);
    EventLocation location = new EventLocation("world", 10.0, 64.0, 10.0, 0f, 0f);

    var ex =
        assertThrows(RuntimeException.class, () -> failingManager.startEvent(sampleDef, location));
    assertTrue(ex.getMessage().contains("DB crash"));
  }

  @Test
  void testScheduleGlobalSupport() {
    java.util.concurrent.atomic.AtomicBoolean globalScheduled =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    EventTaskScheduler globalScheduler =
        new EventTaskScheduler() {
          @Override
          public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

          @Override
          public void scheduleGlobal(Duration delay, Runnable task) {
            globalScheduled.set(true);
          }

          @Override
          public void cancelAll(EventInstanceId eventId) {}

          @Override
          public void cancelAll() {}
        };

    EngineContext globalContext =
        new EngineContext(
            new InMemoryEventInstanceRepository(),
            registry,
            globalScheduler,
            context.platformActionPort(),
            new EventRuntimeStateStore());

    EventLifecycleManager globalManager = new EventLifecycleManager(globalContext);
    EventLocation location = new EventLocation("world", 10.0, 64.0, 10.0, 0f, 0f);
    globalManager.startEvent(sampleDef, location);
    assertTrue(globalScheduled.get());
  }
}
