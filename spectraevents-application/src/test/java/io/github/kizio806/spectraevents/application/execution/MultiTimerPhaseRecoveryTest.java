package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiTimerPhaseRecoveryTest {
  private InMemoryEventInstanceRepository repository;
  private EventDefinitionRegistry registry;
  private RecordingEventTaskScheduler scheduler;
  private RecordingPlatformActionPort actionPort;
  private EventRuntimeStateStore stateStore;
  private EventExecutionEngine engine;

  @BeforeEach
  void setUp() {
    repository = new InMemoryEventInstanceRepository();
    registry = new EventDefinitionRegistry();
    scheduler = new RecordingEventTaskScheduler();
    actionPort = new RecordingPlatformActionPort();
    stateStore = new EventRuntimeStateStore();
    engine = new EventExecutionEngine(repository, registry, scheduler, actionPort, stateStore);
  }

  @Test
  void twoTimersWithDifferentDeadlinesPersistKeyedStateAndShorterTimerWins() {
    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("multi_timer_event"),
            new PhaseId("active"),
            Map.of(
                new PhaseId("active"),
                new PhaseDefinition(
                    new PhaseId("active"),
                    Set.of(new PhaseId("short_timeout"), new PhaseId("long_timeout")),
                    List.of(
                        new TransitionRule(
                            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
                            List.of(),
                            Optional.of(new PhaseId("short_timeout")),
                            List.of()),
                        new TransitionRule(
                            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(60)),
                            List.of(),
                            Optional.of(new PhaseId("long_timeout")),
                            List.of())),
                    List.of()),
                new PhaseId("short_timeout"),
                new PhaseDefinition(new PhaseId("short_timeout"), Set.of()),
                new PhaseId("long_timeout"),
                new PhaseDefinition(new PhaseId("long_timeout"), Set.of())));
    registry.register(definition, "multi_timer.yml");

    EventInstance instance = engine.startEvent("multi_timer_event", "mock_location");
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals(new PhaseId("active"), instance.currentPhase().orElseThrow());

    EventRuntimeState state = stateStore.get(instance.id()).orElseThrow();
    Map<String, Long> deadlines = state.timerDeadlines();
    assertEquals(2, deadlines.size());
    assertTrue(deadlines.containsKey("0"));
    assertTrue(deadlines.containsKey("1"));
    assertTrue(deadlines.get("0") < deadlines.get("1"));
    assertEquals(deadlines.get("0"), state.timerDeadlineMillis());

    assertEquals(2, scheduler.scheduled.size());
    assertEquals(Duration.ofSeconds(10), scheduler.scheduled.get(0).delay());
    assertEquals(Duration.ofSeconds(60), scheduler.scheduled.get(1).delay());

    // Simulate recovery
    scheduler.scheduled.clear();
    engine.recoverTimers();

    assertEquals(2, scheduler.scheduled.size());
    assertTrue(
        scheduler.scheduled.get(0).delay().compareTo(scheduler.scheduled.get(1).delay()) < 0);

    // Fire shorter timer
    scheduler.scheduled.get(0).task().run();

    EventInstance afterShort = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("short_timeout"), afterShort.currentPhase().orElseThrow());

    // All active timers for old phase must be cancelled and deadlines cleared
    assertTrue(scheduler.cancelledInstances.contains(instance.id()));
    EventRuntimeState stateAfter = stateStore.get(instance.id()).orElseThrow();
    assertTrue(stateAfter.timerDeadlines().isEmpty());
  }

  @Test
  void handlesLegacySingleDeadlineSnapshotExplicitly() {
    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("legacy_timer_event"),
            new PhaseId("active"),
            Map.of(
                new PhaseId("active"),
                new PhaseDefinition(
                    new PhaseId("active"),
                    Set.of(new PhaseId("completed")),
                    List.of(
                        new TransitionRule(
                            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(30)),
                            List.of(),
                            Optional.of(new PhaseId("completed")),
                            List.of())),
                    List.of()),
                new PhaseId("completed"),
                new PhaseDefinition(new PhaseId("completed"), Set.of())));
    registry.register(definition, "legacy.yml");

    EventInstanceId id = EventInstanceId.generate();
    repository.save(
        EventInstance.reconstitute(
            id, definition.id(), EventLifecycleState.RUNNING, new PhaseId("active")));
    EventRuntimeState legacyState = new EventRuntimeState(id);
    legacyState.clearTimerDeadlines();
    // Simulate legacy snapshot where only legacyTimerDeadline is set without keyed map
    legacyState.setTimerDeadlineMillis(System.currentTimeMillis() + 15_000L);
    repository.saveState(legacyState);

    engine.recoverTimers();

    assertEquals(1, scheduler.scheduled.size());
    assertNotNull(scheduler.scheduled.get(0).task());
  }

  private static record ScheduledTask(EventInstanceId eventId, Duration delay, Runnable task) {}

  private static class RecordingEventTaskScheduler implements EventTaskScheduler {
    final List<ScheduledTask> scheduled = new ArrayList<>();
    final List<EventInstanceId> cancelledInstances = new ArrayList<>();

    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
      scheduled.add(new ScheduledTask(eventId, delay, task));
    }

    @Override
    public void cancelAll(EventInstanceId eventId) {
      cancelledInstances.add(eventId);
    }

    @Override
    public void cancelAll() {
      cancelledInstances.clear();
    }
  }

  private static class RecordingPlatformActionPort implements PlatformActionPort {
    final List<ActionDefinition> executedActions = new ArrayList<>();

    @Override
    public CompletableFuture<Boolean> executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      executedActions.add(action);
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public void cleanupEvent(EventInstanceId instanceId) {}
  }
}
