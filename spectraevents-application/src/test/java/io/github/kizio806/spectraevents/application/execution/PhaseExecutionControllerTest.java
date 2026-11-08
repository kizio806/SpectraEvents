package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
  private EventDefinitionRegistry registry;
  private EventLifecycleManager lifecycleManager;
  private ActionExecutionCoordinator actionCoordinator;
  private EventTriggerEvaluator triggerEvaluator;
  private PhaseExecutionController controller;
  private EventDefinition sampleDef;
  private final List<Runnable> scheduledTasks = new ArrayList<>();

  @BeforeEach
  void setUp() {
    EventInstanceRepository repository = new InMemoryEventInstanceRepository();
    registry = new EventDefinitionRegistry();
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
        (executionId, action, executionContext) -> CompletableFuture.completedFuture(true);
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

  @Test
  void testRewardRuleIgnoredWhenAlreadyClaimed() {
    PhaseId p1 = new PhaseId("p1");
    TransitionRule rewardRule =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
            List.of(),
            Optional.empty(),
            List.of(
                (io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition)
                    () -> "give_item"));

    EventDefinition defWithReward =
        new EventDefinition(
            new EventDefinitionId("reward_def"),
            p1,
            Map.of(p1, new PhaseDefinition(p1, Set.of(), List.of(rewardRule), List.of())),
            EventEncounterSettings.NONE);

    registry.register(defWithReward, "reward.yml");

    EventInstance instance =
        lifecycleManager.startEvent(defWithReward, new EventLocation("world", 0, 0, 0, 0, 0));
    EventRuntimeState state = context.stateStore().getOrCreate(instance.id());
    state.tryClaim("player1");

    boolean fired =
        controller.evaluateTrigger(
            instance.id(),
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
            ExecutionContext.EMPTY);
    assertFalse(fired);
  }

  @Test
  void testActionFailureFailsEventInstance() {
    PlatformActionPort failingPort =
        (id, act, ctx) ->
            CompletableFuture.failedFuture(new RuntimeException("Simulated action failure"));

    EngineContext failingContext =
        new EngineContext(
            context.repository(), registry, context.scheduler(), failingPort, context.stateStore());

    EventLifecycleManager lm = new EventLifecycleManager(failingContext);
    InternalActionExecutor executor =
        new InternalActionExecutor(context.repository(), List.of(), null, failingPort);
    ActionExecutionCoordinator coord = new ActionExecutionCoordinator(failingContext, executor, lm);
    PhaseExecutionController failingController =
        new PhaseExecutionController(failingContext, coord, triggerEvaluator, lm);
    lm.setPhaseController(failingController);

    PhaseId p1 = new PhaseId("p1");
    TransitionRule ruleWithAction =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
            List.of(),
            Optional.empty(),
            List.of(
                (io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition)
                    () -> "custom_action"));

    EventDefinition defWithAction =
        new EventDefinition(
            new EventDefinitionId("failing_def"),
            p1,
            Map.of(p1, new PhaseDefinition(p1, Set.of(), List.of(ruleWithAction), List.of())),
            EventEncounterSettings.NONE);

    registry.register(defWithAction, "failing.yml");

    EventInstance instance =
        lm.startEvent(defWithAction, new EventLocation("world", 0, 0, 0, 0, 0));

    failingController.evaluateTrigger(
        instance.id(),
        new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
        ExecutionContext.EMPTY);

    EventInstance updated = failingContext.repository().findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, updated.state());
  }

  @Test
  void testTimerRecoveryWithDeadlines() {
    EventInstance instance =
        lifecycleManager.startEvent(sampleDef, new EventLocation("world", 0, 0, 0, 0, 0));
    EventRuntimeState state = context.stateStore().getOrCreate(instance.id());
    state.setTimerDeadlineMillis("0", System.currentTimeMillis() + 5000);
    context.repository().saveState(state);

    scheduledTasks.clear();
    controller.recoverTimers();
    assertFalse(scheduledTasks.isEmpty());
    // Execute recovered task
    Runnable recoveredTask = scheduledTasks.getFirst();
    assertDoesNotThrow(recoveredTask::run);
  }

  @Test
  void testOnEnterActionFailureFailsEvent() {
    PlatformActionPort failingPort =
        (id, act, ctx) -> CompletableFuture.failedFuture(new RuntimeException("OnEnter failure"));

    EngineContext failingContext =
        new EngineContext(
            context.repository(), registry, context.scheduler(), failingPort, context.stateStore());

    EventLifecycleManager lm = new EventLifecycleManager(failingContext);
    InternalActionExecutor executor =
        new InternalActionExecutor(context.repository(), List.of(), null, failingPort);
    ActionExecutionCoordinator coord = new ActionExecutionCoordinator(failingContext, executor, lm);
    PhaseExecutionController failingController =
        new PhaseExecutionController(failingContext, coord, triggerEvaluator, lm);
    lm.setPhaseController(failingController);

    PhaseId p1 = new PhaseId("p1");
    EventDefinition defWithOnEnter =
        new EventDefinition(
            new EventDefinitionId("on_enter_fail"),
            p1,
            Map.of(
                p1,
                new PhaseDefinition(
                    p1,
                    Set.of(),
                    List.of(),
                    List.of(
                        (io.github.kizio806.spectraevents.core.event.execution.action
                                .ActionDefinition)
                            () -> "on_enter_act"))),
            EventEncounterSettings.NONE);

    registry.register(defWithOnEnter, "on_enter_fail.yml");

    EventInstance instance =
        lm.startEvent(defWithOnEnter, new EventLocation("world", 0, 0, 0, 0, 0));

    EventInstance updated = failingContext.repository().findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, updated.state());
  }

  @Test
  void testEvaluateTriggerThrowsWhenCurrentPhaseMissingOrUnknown() {
    EventInstanceId unstartedId = EventInstanceId.generate();
    EventInstance unstarted =
        EventInstance.reconstitute(unstartedId, sampleDef.id(), EventLifecycleState.RUNNING, null);
    context.repository().save(unstarted);

    assertThrows(
        IllegalStateException.class,
        () ->
            controller.evaluateTrigger(
                unstartedId,
                new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(1)),
                ExecutionContext.EMPTY));

    EventInstanceId unknownPhaseId = EventInstanceId.generate();
    EventInstance unknownPhaseInst =
        EventInstance.reconstitute(
            unknownPhaseId,
            sampleDef.id(),
            EventLifecycleState.RUNNING,
            new PhaseId("non_existent_phase"));
    context.repository().save(unknownPhaseInst);

    assertThrows(
        IllegalStateException.class,
        () ->
            controller.evaluateTrigger(
                unknownPhaseId,
                new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(1)),
                ExecutionContext.EMPTY));
  }

  @Test
  void testActionFailureWithNonExceptionThrowableFailsEvent() {
    PlatformActionPort failingPort =
        (id, act, ctx) ->
            CompletableFuture.failedFuture(new AssertionError("Fatal assertion in action"));

    EngineContext failingContext =
        new EngineContext(
            context.repository(), registry, context.scheduler(), failingPort, context.stateStore());

    EventLifecycleManager lm = new EventLifecycleManager(failingContext);
    InternalActionExecutor executor =
        new InternalActionExecutor(context.repository(), List.of(), null, failingPort);
    ActionExecutionCoordinator coord = new ActionExecutionCoordinator(failingContext, executor, lm);
    PhaseExecutionController failingController =
        new PhaseExecutionController(failingContext, coord, triggerEvaluator, lm);
    lm.setPhaseController(failingController);

    PhaseId p1 = new PhaseId("p1");
    TransitionRule ruleWithAction =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
            List.of(),
            Optional.empty(),
            List.of(
                (io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition)
                    () -> "custom_action"));

    EventDefinition defWithAction =
        new EventDefinition(
            new EventDefinitionId("throwable_def"),
            p1,
            Map.of(p1, new PhaseDefinition(p1, Set.of(), List.of(ruleWithAction), List.of())),
            EventEncounterSettings.NONE);

    registry.register(defWithAction, "throwable.yml");

    EventInstance instance =
        lm.startEvent(defWithAction, new EventLocation("world", 0, 0, 0, 0, 0));

    failingController.evaluateTrigger(
        instance.id(),
        new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
        ExecutionContext.EMPTY);

    EventInstance updated = failingContext.repository().findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, updated.state());
  }

  @Test
  void testTargetPhaseTransitionSkippedWhenInstanceNoLongerRunning() {
    PhaseId p1 = new PhaseId("p1");
    PhaseId p2 = new PhaseId("p2");
    CompletableFuture<Boolean> actionPromise = new CompletableFuture<>();
    PlatformActionPort asyncPort = (id, act, ctx) -> actionPromise;

    EngineContext customContext =
        new EngineContext(
            context.repository(), registry, context.scheduler(), asyncPort, context.stateStore());

    EventLifecycleManager lm = new EventLifecycleManager(customContext);
    InternalActionExecutor executor =
        new InternalActionExecutor(context.repository(), List.of(), null, asyncPort);
    ActionExecutionCoordinator coord = new ActionExecutionCoordinator(customContext, executor, lm);
    PhaseExecutionController customController =
        new PhaseExecutionController(customContext, coord, triggerEvaluator, lm);
    lm.setPhaseController(customController);

    TransitionRule ruleWithTarget =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
            List.of(),
            Optional.of(p2),
            List.of(
                (io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition)
                    () -> "async_action"));

    EventDefinition defWithTarget =
        new EventDefinition(
            new EventDefinitionId("target_def"),
            p1,
            Map.of(
                p1, new PhaseDefinition(p1, Set.of(p2), List.of(ruleWithTarget), List.of()),
                p2, new PhaseDefinition(p2, Set.of())),
            EventEncounterSettings.NONE);

    registry.register(defWithTarget, "target.yml");

    EventInstance instance =
        lm.startEvent(defWithTarget, new EventLocation("world", 0, 0, 0, 0, 0));

    customController.evaluateTrigger(
        instance.id(),
        new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(10)),
        ExecutionContext.EMPTY);

    // Cancel instance before action completes
    lm.cancelEvent(instance.id());

    // Complete action successfully
    actionPromise.complete(true);

    EventInstance finalInstance = customContext.repository().findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.CANCELLED, finalInstance.state());
    assertEquals("p1", finalInstance.currentPhase().orElseThrow().value());
  }

  @Test
  void testExecutePhaseEntryThrowsOnUnknownPhase() {
    EventInstance instance =
        lifecycleManager.startEvent(sampleDef, new EventLocation("world", 0, 0, 0, 0, 0));

    assertThrows(
        IllegalStateException.class,
        () ->
            controller.executePhaseEntry(
                instance, sampleDef, new PhaseId("completely_unknown"), ExecutionContext.EMPTY));
  }

  @Test
  void testTimerRecoveryWithLegacyDeadline() {
    EventInstance instance =
        lifecycleManager.startEvent(sampleDef, new EventLocation("world", 0, 0, 0, 0, 0));
    EventRuntimeState state = context.stateStore().getOrCreate(instance.id());
    // Clear keyed deadlines and set legacy timer deadline
    state.clearTimerDeadlines();
    state.setTimerDeadlineMillis(System.currentTimeMillis() + 3000);
    state.clearTimerDeadlines(); // leaves legacyTimerDeadlineMillis at 3000ms
    context.repository().saveState(state);

    scheduledTasks.clear();
    controller.recoverTimers();
    assertFalse(scheduledTasks.isEmpty());
    Runnable recoveredTask = scheduledTasks.getFirst();
    assertDoesNotThrow(recoveredTask::run);
  }

  @Test
  void testRecoverTimersWhenDefinitionNotFoundFailsEvent() {
    EventInstanceId missingDefId = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            missingDefId,
            new EventDefinitionId("unregistered_definition"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    context.repository().save(instance);

    controller.recoverTimers();

    EventInstance failed = context.repository().findById(missingDefId).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, failed.state());
  }

  @Test
  void testTimerScheduledTaskExecutionAndExceptionHandling() {
    PhaseId p1 = new PhaseId("p1");
    TransitionRule ruleTimer =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(5)),
            List.of(),
            Optional.empty(),
            List.of());

    PhaseDefinition phase = new PhaseDefinition(p1, Set.of(), List.of(ruleTimer), List.of());
    EventDefinition def =
        new EventDefinition(new EventDefinitionId("timer_exec_def"), p1, Map.of(p1, phase));
    registry.register(def, "timer_exec.yml");

    EventInstance instance =
        lifecycleManager.startEvent(def, new EventLocation("world", 0, 0, 0, 0, 0));

    scheduledTasks.clear();
    controller.executePhaseEntry(instance, def, p1, ExecutionContext.EMPTY);
    assertFalse(scheduledTasks.isEmpty());

    // 1. Happy path run
    Runnable normalTask = scheduledTasks.getFirst();
    assertDoesNotThrow(normalTask::run);

    // 2. Exception path: cancel instance so evaluateTrigger throws IllegalStateException
    // We reconstitute an instance in RUNNING with null currentPhase so evaluateTrigger throws
    // IllegalStateException
    context
        .repository()
        .save(
            EventInstance.reconstitute(instance.id(), def.id(), EventLifecycleState.RUNNING, null));
    assertDoesNotThrow(normalTask::run);
  }

  @Test
  void testRecoveredTimerExceptionHandling() {
    EventInstance instance =
        lifecycleManager.startEvent(sampleDef, new EventLocation("world", 0, 0, 0, 0, 0));
    EventRuntimeState state = context.stateStore().getOrCreate(instance.id());
    state.setTimerDeadlineMillis("0", System.currentTimeMillis() + 5000);
    context.repository().saveState(state);

    scheduledTasks.clear();
    controller.recoverTimers();
    assertFalse(scheduledTasks.isEmpty());
    Runnable recoveredTask = scheduledTasks.getFirst();

    // Reconstitute instance so evaluateTrigger throws IllegalStateException
    context
        .repository()
        .save(
            EventInstance.reconstitute(
                instance.id(), sampleDef.id(), EventLifecycleState.RUNNING, null));
    assertDoesNotThrow(recoveredTask::run);
  }

  @Test
  void testTransitionToPhaseWithExcessiveTimerFailsEvent() {
    PhaseId p1 = new PhaseId("p1");
    PhaseId p2 = new PhaseId("p2");
    // p2 has timer duration > 24 hours
    TransitionRule ruleP2 =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ofDays(8)),
            List.of(),
            Optional.empty(),
            List.of());
    TransitionRule ruleP1 =
        new TransitionRule(
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(1)),
            List.of(),
            Optional.of(p2),
            List.of());

    PhaseDefinition phase1 = new PhaseDefinition(p1, Set.of(p2), List.of(ruleP1), List.of());
    PhaseDefinition phase2 = new PhaseDefinition(p2, Set.of(), List.of(ruleP2), List.of());
    EventDefinition def =
        new EventDefinition(
            new EventDefinitionId("excessive_timer"), p1, Map.of(p1, phase1, p2, phase2));
    registry.register(def, "excessive.yml");

    EventInstance instance =
        lifecycleManager.startEvent(def, new EventLocation("world", 0, 0, 0, 0, 0));

    boolean fired =
        controller.evaluateTrigger(
            instance.id(),
            new CoreTriggers.TimerElapsedTrigger(Duration.ofSeconds(1)),
            ExecutionContext.EMPTY);
    assertTrue(fired);

    for (CompletableFuture<?> f : context.getOrCreateObservedActionCompletions(instance.id())) {
      f.handle((res, ex) -> ex != null ? ex.getMessage() : res).join();
    }

    EventInstance updated = context.repository().findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, updated.state());
  }

  @Test
  void testRecoverTimersEncounterRecoveryExceptionFailsEvent() {
    EventInstanceId encId = EventInstanceId.generate();
    EventEncounterSettings encSettings =
        new EventEncounterSettings(50.0d, Duration.ofMinutes(10), 10, 1);
    PhaseId p1 = new PhaseId("p1");
    EventDefinition encDef =
        new EventDefinition(
            new EventDefinitionId("enc_def"),
            p1,
            Map.of(p1, new PhaseDefinition(p1, Set.of())),
            encSettings);
    registry.register(encDef, "enc.yml");

    EventZone zone = new EventZone("world", 0.0, 0.0, 50.0);
    // Pre-occupy world limit of 1
    context.zoneRegistry().reserve(EventInstanceId.generate(), encDef.id(), zone, 1);

    EventInstance instance =
        EventInstance.reconstitute(encId, encDef.id(), EventLifecycleState.RUNNING, p1);
    context.repository().save(instance);
    EventRuntimeState state = context.stateStore().getOrCreate(encId);
    state.setEventZone(zone);
    context.repository().saveState(state);

    controller.recoverTimers();

    EventInstance failed = context.repository().findById(encId).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, failed.state());
  }
}
