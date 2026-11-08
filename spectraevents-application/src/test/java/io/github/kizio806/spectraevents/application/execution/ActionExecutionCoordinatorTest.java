package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredActionDefinition;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActionExecutionCoordinatorTest {

  private InMemoryEventInstanceRepository repository;
  private EventDefinitionRegistry registry;
  private EventRuntimeStateStore stateStore;
  private EngineContext context;
  private EventLifecycleManager lifecycleManager;
  private InternalActionExecutor internalActionExecutor;
  private ActionExecutionCoordinator coordinator;
  private TestPlatformActionPort platformActionPort;

  @BeforeEach
  void setUp() {
    repository = new InMemoryEventInstanceRepository();
    registry = new EventDefinitionRegistry();
    stateStore = new EventRuntimeStateStore();
    platformActionPort = new TestPlatformActionPort();
    context =
        new EngineContext(
            repository, registry, new StubScheduler(), platformActionPort, stateStore);
    lifecycleManager = new EventLifecycleManager(context);
    internalActionExecutor =
        new InternalActionExecutor(repository, List.of(), null, platformActionPort);
    coordinator = new ActionExecutionCoordinator(context, internalActionExecutor, lifecycleManager);
  }

  @Test
  void testContainsRewardAction() {
    assertFalse(coordinator.containsRewardAction(List.of()));
    assertFalse(
        coordinator.containsRewardAction(List.of(new ConfiguredActionDefinition("spawn_entity"))));

    assertTrue(
        coordinator.containsRewardAction(List.of(new ConfiguredActionDefinition("give_item"))));
    assertTrue(
        coordinator.containsRewardAction(List.of(new ConfiguredActionDefinition("drop_loot"))));
    assertTrue(
        coordinator.containsRewardAction(
            List.of(new ConfiguredActionDefinition("release_ground_loot"))));
    assertTrue(
        coordinator.containsRewardAction(List.of(new ConfiguredActionDefinition("award_podium"))));
  }

  @Test
  void testAcceptRewardClaimWithActor() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    UUID actor = UUID.randomUUID();
    ExecutionContext execContext = new ExecutionContext(actor, Map.of());

    CompletableFuture<Boolean> future =
        coordinator.acceptRewardClaim(
            state, new ConfiguredTriggerDefinition("interaction"), execContext);

    assertTrue(future.join());
    assertTrue(state.isClaimed());
    assertEquals(actor.toString(), state.claimant());

    // Second claim fails
    CompletableFuture<Boolean> secondFuture =
        coordinator.acceptRewardClaim(
            state, new ConfiguredTriggerDefinition("interaction"), execContext);
    assertFalse(secondFuture.join());
  }

  @Test
  void testAcceptRewardClaimWithSystemClaimant() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);

    CompletableFuture<Boolean> futureNullContext =
        coordinator.acceptRewardClaim(state, new ConfiguredTriggerDefinition("timer"), null);
    assertTrue(futureNullContext.join());
    assertTrue(state.isClaimed());
    assertEquals("system:timer", state.claimant());
  }

  @Test
  void testAcceptRewardClaimWithEmptyActor() {
    EventInstanceId id = EventInstanceId.generate();
    EventRuntimeState state = new EventRuntimeState(id);
    ExecutionContext emptyActorContext = new ExecutionContext(null, Map.of());

    CompletableFuture<Boolean> future =
        coordinator.acceptRewardClaim(
            state, new ConfiguredTriggerDefinition("chest_click"), emptyActorContext);
    assertTrue(future.join());
    assertTrue(state.isClaimed());
    assertEquals("system:chest_click", state.claimant());
  }

  @Test
  void testAcceptRewardClaimConcurrentRace() throws Exception {
    for (int i = 0; i < 300; i++) {
      EventInstanceId id = EventInstanceId.generate();
      EventRuntimeState state = new EventRuntimeState(id);
      java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
      try (java.util.concurrent.ExecutorService pool =
          java.util.concurrent.Executors.newFixedThreadPool(4)) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
          final UUID actor = UUID.randomUUID();
          futures.add(
              CompletableFuture.supplyAsync(
                  () -> {
                    try {
                      latch.await();
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                    }
                    return coordinator
                        .acceptRewardClaim(
                            state,
                            new ConfiguredTriggerDefinition("interaction"),
                            new ExecutionContext(actor, Map.of()))
                        .join();
                  },
                  pool));
        }
        latch.countDown();
        for (CompletableFuture<Boolean> f : futures) {
          f.join();
        }
      }
    }
  }

  @Test
  void testObserveActionCompletionPruning() {
    EventInstanceId id = EventInstanceId.generate();
    CompletableFuture<Boolean> doneFuture = CompletableFuture.completedFuture(true);
    AtomicBoolean called = new AtomicBoolean(false);

    coordinator.observeActionCompletion(id, doneFuture, (ok, ex) -> called.set(true));
    assertTrue(called.get());

    // Next observation should prune the completed one
    CompletableFuture<Boolean> pendingFuture = new CompletableFuture<>();
    coordinator.observeActionCompletion(id, pendingFuture, (ok, ex) -> {});

    Set<CompletableFuture<Boolean>> completions = context.getOrCreateObservedActionCompletions(id);
    assertEquals(1, completions.size());
  }

  @Test
  void testExecuteActionsSequentialSuccessAndEarlyExit() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            id, new EventDefinitionId("test"), EventLifecycleState.RUNNING, new PhaseId("p1"));
    EventRuntimeState state = new EventRuntimeState(id);

    List<ActionDefinition> actions =
        List.of(
            new ConfiguredActionDefinition("custom_ok"),
            new ConfiguredActionDefinition("custom_fail"),
            new ConfiguredActionDefinition("custom_never_run"));

    platformActionPort.actionHandler =
        action -> {
          if (action.type().equals("custom_ok")) {
            return CompletableFuture.completedFuture(true);
          }
          if (action.type().equals("custom_fail")) {
            return CompletableFuture.completedFuture(false);
          }
          throw new AssertionError("Should not run custom_never_run");
        };

    CompletableFuture<Boolean> result =
        coordinator.executeActions(instance, state, actions, ExecutionContext.EMPTY);
    assertFalse(result.join());
  }

  @Test
  void testExecuteActionsFatalActionExceptionRethrown() {
    EventInstanceId id = EventInstanceId.generate();
    EventDefinition def = createTestDefinition("test_fatal");
    registry.register(def, "test.yml");
    EventInstance instance = lifecycleManager.startEvent(def, null);
    EventRuntimeState state = new EventRuntimeState(id);

    List<ActionDefinition> actions = List.of(new ConfiguredActionDefinition("fatal_action"));

    platformActionPort.actionHandler =
        action -> {
          throw new FatalActionException("Critical failure", new IllegalStateException("cause"));
        };

    CompletableFuture<Boolean> future =
        coordinator.executeActions(instance, state, actions, ExecutionContext.EMPTY);
    java.util.concurrent.ExecutionException ex =
        assertThrows(java.util.concurrent.ExecutionException.class, future::get);
    assertTrue(ex.getCause() instanceof FatalActionException);

    EventInstance failed = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, failed.state());
  }

  @Test
  void testExecuteActionsStandardExceptionsWrappedInFatal() {
    EventInstanceId id = EventInstanceId.generate();
    EventDefinition def = createTestDefinition("test_standard_ex");
    registry.register(def, "test.yml");
    EventInstance instance = lifecycleManager.startEvent(def, null);
    EventRuntimeState state = new EventRuntimeState(id);

    List<ActionDefinition> actions = List.of(new ConfiguredActionDefinition("bad_action"));

    platformActionPort.actionHandler =
        action -> {
          throw new IllegalArgumentException("Bad argument");
        };

    CompletableFuture<Boolean> future =
        coordinator.executeActions(instance, state, actions, ExecutionContext.EMPTY);
    java.util.concurrent.ExecutionException ex =
        assertThrows(java.util.concurrent.ExecutionException.class, future::get);
    assertTrue(ex.getCause() instanceof FatalActionException);

    EventInstance failed = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, failed.state());
  }

  @Test
  void testExecuteActionsUnsupportedOperationExceptionWrapped() {
    EventInstanceId id = EventInstanceId.generate();
    EventDefinition def = createTestDefinition("test_unsupported");
    registry.register(def, "test.yml");
    EventInstance instance = lifecycleManager.startEvent(def, null);
    EventRuntimeState state = new EventRuntimeState(id);

    List<ActionDefinition> actions = List.of(new ConfiguredActionDefinition("bad_action"));

    platformActionPort.actionHandler =
        action -> {
          throw new UnsupportedOperationException("Unsupported");
        };

    CompletableFuture<Boolean> future =
        coordinator.executeActions(instance, state, actions, ExecutionContext.EMPTY);
    java.util.concurrent.ExecutionException ex =
        assertThrows(java.util.concurrent.ExecutionException.class, future::get);
    assertTrue(ex.getCause() instanceof FatalActionException);

    EventInstance failed = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, failed.state());
  }

  @Test
  void testRecoverPlatformResourcesEarlyReturnWhenNoPhase() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance unstarted = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);
    EventDefinition def = createTestDefinition("test");

    coordinator.recoverPlatformResources(unstarted, state, def);
    assertFalse(platformActionPort.cleanupCalled);
  }

  @Test
  void testRecoverPlatformResourcesEarlyReturnWhenUnknownPhase() {
    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            id, new EventDefinitionId("test"), EventLifecycleState.RUNNING, new PhaseId("unknown"));
    EventRuntimeState state = new EventRuntimeState(id);
    EventDefinition def = createTestDefinition("test");

    coordinator.recoverPlatformResources(instance, state, def);
    assertFalse(platformActionPort.cleanupCalled);
  }

  @Test
  void testRecoverPlatformResourcesExecutesOnlyRecoverableActions() {
    PhaseId p1 = new PhaseId("p1");
    ActionDefinition recoverable = new ConfiguredActionDefinition("spawn_model");
    ActionDefinition nonRecoverable = new ConfiguredActionDefinition("give_item");

    PhaseDefinition phaseDef =
        new PhaseDefinition(p1, Set.of(), List.of(), List.of(recoverable, nonRecoverable));
    EventDefinition def =
        new EventDefinition(new EventDefinitionId("rec_test"), p1, Map.of(p1, phaseDef));
    registry.register(def, "rec_test.yml");

    EventInstance instance = lifecycleManager.startEvent(def, null);
    EventRuntimeState state = new EventRuntimeState(instance.id());

    List<String> executedActions = new ArrayList<>();
    platformActionPort.actionHandler =
        action -> {
          executedActions.add(action.type());
          return CompletableFuture.completedFuture(true);
        };

    coordinator.recoverPlatformResources(instance, state, def);

    assertTrue(platformActionPort.cleanupCalled);
    assertEquals(List.of("spawn_model"), executedActions);
  }

  @Test
  void testRecoverPlatformResourcesFailureAsync() {
    PhaseId p1 = new PhaseId("p1");
    ActionDefinition recoverable = new ConfiguredActionDefinition("spawn_boss");
    PhaseDefinition phaseDef = new PhaseDefinition(p1, Set.of(), List.of(), List.of(recoverable));
    EventDefinition def =
        new EventDefinition(new EventDefinitionId("async_fail"), p1, Map.of(p1, phaseDef));
    registry.register(def, "async_fail.yml");

    EventInstance instance = lifecycleManager.startEvent(def, null);
    EventRuntimeState state = new EventRuntimeState(instance.id());

    platformActionPort.actionHandler =
        action -> CompletableFuture.failedFuture(new RuntimeException("Boss spawn failed"));

    coordinator.recoverPlatformResources(instance, state, def);

    EventInstance failed = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, failed.state());
  }

  @Test
  void testRecoverPlatformResourcesFailureAsyncWithCustomThrowable() {
    PhaseId p1 = new PhaseId("p1");
    ActionDefinition recoverable = new ConfiguredActionDefinition("spawn_boss");
    PhaseDefinition phaseDef = new PhaseDefinition(p1, Set.of(), List.of(), List.of(recoverable));
    EventDefinition def =
        new EventDefinition(new EventDefinitionId("throwable_fail"), p1, Map.of(p1, phaseDef));
    registry.register(def, "throwable_fail.yml");

    EventInstance instance = lifecycleManager.startEvent(def, null);
    EventRuntimeState state = new EventRuntimeState(instance.id());

    platformActionPort.actionHandler =
        action -> CompletableFuture.failedFuture(new AssertionError("Fatal assertion"));

    coordinator.recoverPlatformResources(instance, state, def);

    EventInstance failed = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, failed.state());
  }

  @Test
  void testRecoverPlatformResourcesSyncException() {
    PhaseId p1 = new PhaseId("p1");
    ActionDefinition recoverable = new ConfiguredActionDefinition("spawn_boss");
    PhaseDefinition phaseDef = new PhaseDefinition(p1, Set.of(), List.of(), List.of(recoverable));
    EventDefinition def =
        new EventDefinition(new EventDefinitionId("sync_fail"), p1, Map.of(p1, phaseDef));
    registry.register(def, "sync_fail.yml");

    EventInstance instance = lifecycleManager.startEvent(def, null);
    EventRuntimeState state = new EventRuntimeState(instance.id());

    platformActionPort.actionHandler =
        action -> {
          throw new IllegalStateException("Immediate sync failure");
        };

    coordinator.recoverPlatformResources(instance, state, def);

    EventInstance failed = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.FAILED, failed.state());
  }

  private EventDefinition createTestDefinition(String id) {
    PhaseId p1 = new PhaseId("p1");
    return new EventDefinition(
        new EventDefinitionId(id), p1, Map.of(p1, new PhaseDefinition(p1, Set.of())));
  }

  private static final class StubScheduler implements EventTaskScheduler {
    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
      task.run();
    }

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}
  }

  private static final class TestPlatformActionPort implements PlatformActionPort {
    boolean cleanupCalled = false;
    java.util.function.Function<ActionDefinition, CompletableFuture<Boolean>> actionHandler =
        action -> CompletableFuture.completedFuture(true);

    @Override
    public CompletableFuture<Boolean> executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      return actionHandler.apply(action);
    }

    @Override
    public void cleanupEvent(EventInstanceId instanceId) {
      cleanupCalled = true;
    }
  }
}
