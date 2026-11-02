package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.definition.EventEncounterSettings;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
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

class PhaseExecutionControllerTest {

  private EngineContext context;
  private EventLifecycleManager lifecycleManager;
  private ActionExecutionCoordinator actionCoordinator;
  private EventTriggerEvaluator triggerEvaluator;
  private PhaseExecutionController controller;
  private EventDefinition sampleDef;
  private final List<Runnable> scheduledTasks = new ArrayList<>();

  @BeforeEach
  void setUp() {
    EventInstanceRepository repository = new InMemoryEventInstanceRepository();
    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    EventTaskScheduler scheduler =
        new EventTaskScheduler() {
          @Override
          public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
            scheduledTasks.add(task);
          }

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
    InternalActionExecutor internalExecutor =
        new InternalActionExecutor(repository, List.of(), null, actionPort);
    actionCoordinator = new ActionExecutionCoordinator(context, internalExecutor, lifecycleManager);
    triggerEvaluator = new EventTriggerEvaluator(List.of());
    controller =
        new PhaseExecutionController(
            context, actionCoordinator, triggerEvaluator, lifecycleManager);
    lifecycleManager.setPhaseController(controller);

    PhaseId p1 = new PhaseId("p1");
    PhaseId p2 = new PhaseId("p2");

    TransitionRule ruleP1ToP2 =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
            List.of(),
            Optional.of(p2),
            List.of());

    PhaseDefinition phase1 = new PhaseDefinition(p1, Set.of(p2), List.of(ruleP1ToP2), List.of());
    PhaseDefinition phase2 = new PhaseDefinition(p2, Set.of(), List.of(), List.of());

    sampleDef =
        new EventDefinition(
            new EventDefinitionId("sample"),
            p1,
            Map.of(p1, phase1, p2, phase2),
            new EventEncounterSettings(10.0, Duration.ofMinutes(1), 5, 1));
    registry.register(sampleDef, "sample.yml");
  }

  @Test
  void testEvaluateTriggerOnNonExistentOrCancelledEvent() {
    EventInstanceId id = EventInstanceId.generate();
    assertFalse(
        controller.evaluateTrigger(
            id,
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
            ExecutionContext.EMPTY));

    EventInstance instance =
        lifecycleManager.startEvent(sampleDef, new EventLocation("world", 0, 0, 0, 0, 0));
    lifecycleManager.cancelEvent(instance.id());
    assertFalse(
        controller.evaluateTrigger(
            instance.id(),
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
            ExecutionContext.EMPTY));
  }

  @Test
  void testEvaluateTriggerTransitionsPhase() {
    EventInstance instance =
        lifecycleManager.startEvent(sampleDef, new EventLocation("world", 0, 0, 0, 0, 0));
    assertEquals(new PhaseId("p1"), instance.currentPhase().orElse(null));

    boolean evaluated =
        controller.evaluateTrigger(
            instance.id(),
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
            ExecutionContext.EMPTY);

    assertTrue(evaluated);

    EventInstance latest = context.repository().findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("p2"), latest.currentPhase().orElse(null));
  }

  @Test
  void testTimerValidationInExecutePhaseEntry() {
    PhaseId invalidPhase = new PhaseId("invalid");
    TransitionRule invalidRule =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ZERO),
            List.of(),
            Optional.empty(),
            List.of());

    EventDefinition defWithInvalidTimer =
        new EventDefinition(
            new EventDefinitionId("invalid_timer"),
            invalidPhase,
            Map.of(
                invalidPhase,
                new PhaseDefinition(invalidPhase, Set.of(), List.of(invalidRule), List.of())),
            new EventEncounterSettings(10.0, Duration.ofMinutes(1), 5, 1));

    EventInstance instance =
        EventInstance.create(EventInstanceId.generate(), defWithInvalidTimer.id());
    instance = instance.start(defWithInvalidTimer).eventInstance();
    context.repository().save(instance);

    EventInstance finalInstance = instance;
    controller.executePhaseEntry(
        finalInstance, defWithInvalidTimer, invalidPhase, ExecutionContext.EMPTY);
    EventInstance updated = context.repository().findById(finalInstance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, updated.state());
  }

  @Test
  void testTimerRecoveryWithKeyedDeadlines() {
    EventInstance instance =
        lifecycleManager.startEvent(sampleDef, new EventLocation("world", 0, 0, 0, 0, 0));
    EventRuntimeState state = context.stateStore().getOrCreate(instance.id());
    state.setTimerDeadlineMillis("0", System.currentTimeMillis() + 5000);
    context.repository().saveState(state);

    scheduledTasks.clear();
    controller.recoverTimers();
    assertFalse(scheduledTasks.isEmpty());
  }

  @Test
  void testTimerRecoveryWithUnregisteredDefinitionFails() {
    EventInstanceId unknownId = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            unknownId,
            new EventDefinitionId("unknown_def"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    context.repository().save(instance);

    controller.recoverTimers();

    EventInstance updated = context.repository().findById(unknownId).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, updated.state());
  }

  @Test
  void testTimerExceedingMaxDurationFailsEvent() {
    PhaseId invalidPhase = new PhaseId("huge_timer");
    TransitionRule hugeTimerRule =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ofDays(400)),
            List.of(),
            Optional.empty(),
            List.of());

    EventDefinition defWithHugeTimer =
        new EventDefinition(
            new EventDefinitionId("huge_timer"),
            invalidPhase,
            Map.of(
                invalidPhase,
                new PhaseDefinition(invalidPhase, Set.of(), List.of(hugeTimerRule), List.of())),
            new EventEncounterSettings(10.0, Duration.ofMinutes(1), 5, 1));

    EventInstance instance =
        EventInstance.create(EventInstanceId.generate(), defWithHugeTimer.id());
    instance = instance.start(defWithHugeTimer).eventInstance();
    context.repository().save(instance);

    controller.executePhaseEntry(instance, defWithHugeTimer, invalidPhase, ExecutionContext.EMPTY);
    EventInstance updated = context.repository().findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, updated.state());
  }

  @Test
  void testScheduledTimerExecutionAndSafeErrorHandling() {
    EventInstance instance =
        lifecycleManager.startEvent(sampleDef, new EventLocation("world", 0, 0, 0, 0, 0));
    assertNotNull(instance);
    assertFalse(scheduledTasks.isEmpty());
    Runnable task = scheduledTasks.getFirst();
    assertDoesNotThrow(task::run);
  }
}
