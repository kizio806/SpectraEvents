package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredActionDefinition;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredConditionDefinition;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventExecutionEngineTest {
  private InMemoryEventInstanceRepository repository;
  private EventDefinitionRegistry registry;
  private FakeEventTaskScheduler scheduler;
  private FakePlatformActionPort actionPort;
  private EventRuntimeStateStore stateStore;
  private EventExecutionEngine engine;

  @BeforeEach
  void setUp() {
    repository = new InMemoryEventInstanceRepository();
    registry = new EventDefinitionRegistry();
    scheduler = new FakeEventTaskScheduler();
    actionPort = new FakePlatformActionPort();
    stateStore = new EventRuntimeStateStore();
    engine = new EventExecutionEngine(repository, registry, scheduler, actionPort, stateStore);
  }

  @Test
  void testStartEventExecutesInitialPhaseOnEnterActions() {
    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("test_event"),
            new PhaseId("start"),
            Map.of(
                new PhaseId("start"),
                new PhaseDefinition(
                    new PhaseId("start"),
                    Set.of(new PhaseId("end")),
                    List.of(),
                    List.of(new ConfiguredActionDefinition("spawn_model"))),
                new PhaseId("end"),
                new PhaseDefinition(new PhaseId("end"), Set.of())));
    registry.register(definition, "test_event.yml");

    EventInstance instance = engine.startEvent("test_event", "mock_location");
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals(new PhaseId("start"), instance.currentPhase().orElseThrow());

    assertEquals(1, actionPort.executedActions.size());
    assertEquals("spawn_model", actionPort.executedActions.get(0).type());
  }

  @Test
  void testTriggerMatchingAndPhaseTransition() {
    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("test_event"),
            new PhaseId("phase1"),
            Map.of(
                new PhaseId("phase1"),
                new PhaseDefinition(
                    new PhaseId("phase1"),
                    Set.of(new PhaseId("phase2")),
                    List.of(
                        new TransitionRule(
                            new ConfiguredTriggerDefinition("timer_elapsed"),
                            List.of(),
                            Optional.of(new PhaseId("phase2")),
                            List.of(new ConfiguredActionDefinition("rule_action")))),
                    List.of()),
                new PhaseId("phase2"),
                new PhaseDefinition(
                    new PhaseId("phase2"),
                    Set.of(),
                    List.of(),
                    List.of(new ConfiguredActionDefinition("enter_phase2")))));
    registry.register(definition, "test_event.yml");

    EventInstance instance = engine.startEvent("test_event", null);
    boolean handled =
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("timer_elapsed"));

    assertTrue(handled);
    EventInstance updated = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("phase2"), updated.currentPhase().orElseThrow());

    // Expect rule_action then enter_phase2
    assertEquals(2, actionPort.executedActions.size());
    assertEquals("rule_action", actionPort.executedActions.get(0).type());
    assertEquals("enter_phase2", actionPort.executedActions.get(1).type());
  }

  @Test
  void testFalseConditionBlocksRule() {
    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("test_event"),
            new PhaseId("phase1"),
            Map.of(
                new PhaseId("phase1"),
                new PhaseDefinition(
                    new PhaseId("phase1"),
                    Set.of(new PhaseId("phase2")),
                    List.of(
                        new TransitionRule(
                            new ConfiguredTriggerDefinition("interaction"),
                            List.of(new ConfiguredConditionDefinition("is_locked")),
                            Optional.of(new PhaseId("phase2")),
                            List.of())),
                    List.of()),
                new PhaseId("phase2"),
                new PhaseDefinition(new PhaseId("phase2"), Set.of())));
    registry.register(definition, "test_event.yml");

    EventInstance instance = engine.startEvent("test_event", null);
    boolean handled =
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction"));

    assertFalse(handled);
    EventInstance updated = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("phase1"), updated.currentPhase().orElseThrow());
  }

  @Test
  void testCancelCleansUpTimersAndState() {
    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("test_event"),
            new PhaseId("phase1"),
            Map.of(new PhaseId("phase1"), new PhaseDefinition(new PhaseId("phase1"), Set.of())));
    registry.register(definition, "test_event.yml");

    EventInstance instance = engine.startEvent("test_event", null);
    engine.cancelEvent(instance.id());

    EventInstance updated = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.CANCELLED, updated.state());
    assertTrue(stateStore.get(instance.id()).isEmpty());
    assertTrue(scheduler.cancelledAllForInstance);
  }

  private static class FakeEventTaskScheduler implements EventTaskScheduler {
    boolean cancelledAllForInstance = false;
    List<Runnable> scheduledTasks = new ArrayList<>();

    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
      scheduledTasks.add(task);
    }

    @Override
    public void cancelAll(EventInstanceId eventId) {
      cancelledAllForInstance = true;
    }

    @Override
    public void cancelAll() {
      cancelledAllForInstance = true;
    }
  }

  private static class FakePlatformActionPort implements PlatformActionPort {
    List<ActionDefinition> executedActions = new ArrayList<>();

    @Override
    public void executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      executedActions.add(action);
    }
  }
}
