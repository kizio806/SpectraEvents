package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class InternalActionExecutorRewardTest {
  @Test
  void createsDurablePodiumMailboxClaimForAnOfflineWinner() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RecordingPlatformActionPort platform = new RecordingPlatformActionPort(false);
    EventExecutionEngine engine =
        new EventExecutionEngine(
            repository,
            new EventDefinitionRegistry(),
            new NoOpScheduler(),
            platform,
            new EventRuntimeStateStore());
    InternalActionExecutor executor =
        new InternalActionExecutor(repository, List.of(), engine, platform);
    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            instanceId,
            new EventDefinitionId("podium_test"),
            EventLifecycleState.RUNNING,
            new PhaseId("victory"));
    EventRuntimeState state = new EventRuntimeState(instanceId);
    UUID offlineWinner = UUID.randomUUID();
    state.setMinimumContribution(10);
    state.recordDamage(offlineWinner, 100);
    PlatformActions.PodiumPool first =
        new PlatformActions.PodiumPool(
            List.of(new PlatformActions.LootItem("minecraft:diamond", 3, 100)));
    PlatformActions.PodiumPool empty = new PlatformActions.PodiumPool(List.of());

    boolean completed =
        executor
            .executeInternalAction(
                instance,
                state,
                new PlatformActions.AwardPodiumAction(List.of(first, empty, empty)),
                ExecutionContext.EMPTY)
            .join();

    assertFalse(completed);
    assertEquals(List.of(offlineWinner), platform.announcedPodium);
    assertEquals(List.of(offlineWinner), platform.deliveryAttempts);
    assertEquals(1, repository.findPending(offlineWinner).size());
    assertEquals(
        List.of(new RewardItem("minecraft:diamond", 3)),
        repository.findPending(offlineWinner).getFirst().items());
  }

  @Test
  void deliversPodiumDirectlyForAnOnlineWinner() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RecordingPlatformActionPort platform = new RecordingPlatformActionPort(true);
    EventExecutionEngine engine =
        new EventExecutionEngine(
            repository,
            new EventDefinitionRegistry(),
            new NoOpScheduler(),
            platform,
            new EventRuntimeStateStore());
    InternalActionExecutor executor =
        new InternalActionExecutor(repository, List.of(), engine, platform);
    EventInstanceId instanceId = EventInstanceId.generate();
    EventInstance instance =
        EventInstance.reconstitute(
            instanceId,
            new EventDefinitionId("podium_test"),
            EventLifecycleState.RUNNING,
            new PhaseId("victory"));
    EventRuntimeState state = new EventRuntimeState(instanceId);
    UUID onlineWinner = UUID.randomUUID();
    state.setMinimumContribution(10);
    state.recordDamage(onlineWinner, 100);
    PlatformActions.PodiumPool first =
        new PlatformActions.PodiumPool(
            List.of(new PlatformActions.LootItem("minecraft:diamond", 3, 100)));
    PlatformActions.PodiumPool empty = new PlatformActions.PodiumPool(List.of());

    boolean completed =
        executor
            .executeInternalAction(
                instance,
                state,
                new PlatformActions.AwardPodiumAction(List.of(first, empty, empty)),
                ExecutionContext.EMPTY)
            .join();

    assertTrue(completed);
    assertEquals(List.of(onlineWinner), platform.announcedPodium);
    assertEquals(List.of(onlineWinner), platform.deliveryAttempts);
    assertEquals(0, repository.findPending(onlineWinner).size());
  }

  @Test
  void executesIntegrationResolverWhenSupported() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RecordingPlatformActionPort platform = new RecordingPlatformActionPort(true);
    EventExecutionEngine engine =
        new EventExecutionEngine(
            repository,
            new EventDefinitionRegistry(),
            new NoOpScheduler(),
            platform,
            new EventRuntimeStateStore());

    IntegrationActionResolver resolver =
        new IntegrationActionResolver() {
          @Override
          public boolean supports(String actionType) {
            return "mock_integration_action".equals(actionType);
          }

          @Override
          public CompletableFuture<Boolean> execute(
              ActionDefinition action,
              EventInstance instance,
              EventRuntimeState state,
              ExecutionContext context) {
            return CompletableFuture.completedFuture(true);
          }
        };

    InternalActionExecutor executor =
        new InternalActionExecutor(repository, List.of(resolver), engine, platform);
    EventInstance instance =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("test"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    EventRuntimeState state = new EventRuntimeState(instance.id());

    boolean result =
        executor
            .executeInternalAction(
                instance,
                state,
                (ActionDefinition) () -> "mock_integration_action",
                ExecutionContext.EMPTY)
            .join();
    assertTrue(result);
  }

  @Test
  void testTryClaimActionBranches() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RecordingPlatformActionPort platform = new RecordingPlatformActionPort(true);
    InternalActionExecutor executor =
        new InternalActionExecutor(repository, List.of(), null, platform);
    EventInstance instance =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("test"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    EventRuntimeState state = new EventRuntimeState(instance.id());

    // 1. Context null -> claimant is "unknown"
    boolean c1 =
        executor
            .executeInternalAction(instance, state, new CoreActions.TryClaimAction(), null)
            .join();
    assertTrue(c1);
    assertTrue(state.isClaimed());
    assertEquals("unknown", state.claimant());

    // 2. Second claim with another actor fails
    ExecutionContext playerCtx = ExecutionContext.withActor("player1", UUID.randomUUID());
    boolean c2 =
        executor
            .executeInternalAction(instance, state, new CoreActions.TryClaimAction(), playerCtx)
            .join();
    assertFalse(c2);
  }

  @Test
  void testCreateParticipantRewardClaimsBranches() {
    // 1. Non-claim repository throws IllegalStateException
    EventInstanceRepository nonClaimRepo =
        new EventInstanceRepository() {
          @Override
          public void save(EventInstance eventInstance) {}

          @Override
          public java.util.Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
            return java.util.Optional.empty();
          }

          @Override
          public List<EventInstance> findAll() {
            return List.of();
          }

          @Override
          public boolean remove(EventInstanceId eventInstanceId) {
            return false;
          }

          @Override
          public void saveState(EventRuntimeState state) {}

          @Override
          public java.util.Optional<EventRuntimeState> findState(EventInstanceId eventInstanceId) {
            return java.util.Optional.empty();
          }
        };

    InternalActionExecutor executorNoClaim =
        new InternalActionExecutor(
            nonClaimRepo, List.of(), null, new RecordingPlatformActionPort(true));
    EventInstance instance =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("test"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    EventRuntimeState state = new EventRuntimeState(instance.id());

    CoreActions.GroundLootEntry entry =
        new CoreActions.GroundLootEntry("minecraft:diamond", 1, 100);
    var ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () ->
                executorNoClaim.executeInternalAction(
                    instance,
                    state,
                    new CoreActions.CreateParticipantRewardClaimsAction(List.of(entry)),
                    ExecutionContext.EMPTY));
    assertTrue(ex.getMessage().contains("Reward-claim storage is unavailable"));

    // 2. Empty rolled snapshot returns true
    InMemoryEventInstanceRepository claimRepo = new InMemoryEventInstanceRepository();
    InternalActionExecutor executorClaim =
        new InternalActionExecutor(
            claimRepo, List.of(), null, new RecordingPlatformActionPort(true));
    CoreActions.GroundLootEntry zeroChance =
        new CoreActions.GroundLootEntry("minecraft:diamond", 1, 0); // 0% chance
    boolean emptyResult =
        executorClaim
            .executeInternalAction(
                instance,
                state,
                new CoreActions.CreateParticipantRewardClaimsAction(List.of(zeroChance)),
                ExecutionContext.EMPTY)
            .join();
    assertTrue(emptyResult);
  }

  @Test
  void testCombatDamageActionBranches() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RecordingPlatformActionPort platform = new RecordingPlatformActionPort(true);
    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    PhaseId p1 = new PhaseId("p1");
    EventDefinition def =
        new EventDefinition(
            new EventDefinitionId("test"), p1, Map.of(p1, new PhaseDefinition(p1, Set.of())));
    registry.register(def, "test.yml");

    EventExecutionEngine engine =
        new EventExecutionEngine(
            repository, registry, new NoOpScheduler(), platform, new EventRuntimeStateStore());
    InternalActionExecutor executor =
        new InternalActionExecutor(repository, List.of(), engine, platform);

    EventInstance instance =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("test"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    EventRuntimeState state = new EventRuntimeState(instance.id());
    state.setHealth(new Health(100, 100));

    CoreActions.ApplyCombatDamageAction dmgAct =
        new CoreActions.ApplyCombatDamageAction(20, 0, Duration.ofMillis(500));

    // 1. context == null -> false
    assertFalse(executor.executeInternalAction(instance, state, dmgAct, null).join());

    // 2. context without combat damage -> false
    assertFalse(
        executor.executeInternalAction(instance, state, dmgAct, ExecutionContext.EMPTY).join());

    // 3. context with combat damage
    UUID attacker = UUID.randomUUID();
    ExecutionContext combatCtx = ExecutionContext.withCombatDamage("attacker", attacker, 15.0);
    assertTrue(executor.executeInternalAction(instance, state, dmgAct, combatCtx).join());
    assertEquals(85, state.health().orElseThrow().current());

    // 4. Repeated damage within cooldown -> returns true without reducing health
    assertTrue(executor.executeInternalAction(instance, state, dmgAct, combatCtx).join());
    assertEquals(85, state.health().orElseThrow().current());
  }

  @Test
  void testLifecycleActionsAndHitCounter() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RecordingPlatformActionPort platform = new RecordingPlatformActionPort(true);
    EventExecutionEngine engine =
        new EventExecutionEngine(
            repository,
            new EventDefinitionRegistry(),
            new NoOpScheduler(),
            platform,
            new EventRuntimeStateStore());
    InternalActionExecutor executor =
        new InternalActionExecutor(repository, List.of(), engine, platform);

    EventInstance instance =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("test"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    repository.save(instance);
    EventRuntimeState state = new EventRuntimeState(instance.id());

    // Hit counter null -> returns true
    assertTrue(
        executor
            .executeInternalAction(
                instance, state, new CoreActions.IncrementHitsAction(1), ExecutionContext.EMPTY)
            .join());

    // Complete action
    assertTrue(
        executor
            .executeInternalAction(
                instance, state, new CoreActions.CompleteEventAction(), ExecutionContext.EMPTY)
            .join());
    assertEquals(
        EventLifecycleState.COMPLETED, repository.findById(instance.id()).orElseThrow().state());

    // Cancel action
    EventInstance instance2 =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("test2"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    repository.save(instance2);
    assertTrue(
        executor
            .executeInternalAction(
                instance2, state, new CoreActions.CancelEventAction(), ExecutionContext.EMPTY)
            .join());
    assertEquals(
        EventLifecycleState.CANCELLED, repository.findById(instance2.id()).orElseThrow().state());

    // Fail action
    EventInstance instance3 =
        EventInstance.reconstitute(
            EventInstanceId.generate(),
            new EventDefinitionId("test3"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    repository.save(instance3);
    assertTrue(
        executor
            .executeInternalAction(
                instance3, state, new CoreActions.FailEventAction(), ExecutionContext.EMPTY)
            .join());
    assertEquals(
        EventLifecycleState.FAILED, repository.findById(instance3.id()).orElseThrow().state());
  }

  @Test
  void testInternalActionsComprehensiveCoverage() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RecordingPlatformActionPort platform = new RecordingPlatformActionPort(true);
    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    EventExecutionEngine engine =
        new EventExecutionEngine(
            repository, registry, new NoOpScheduler(), platform, new EventRuntimeStateStore());

    // 1. IntegrationActionResolver support
    IntegrationActionResolver customResolver =
        new IntegrationActionResolver() {
          @Override
          public CompletableFuture<Boolean> execute(
              ActionDefinition action,
              EventInstance instance,
              EventRuntimeState state,
              ExecutionContext context) {
            return CompletableFuture.completedFuture(true);
          }

          @Override
          public boolean supports(String actionType) {
            return "custom_resolver_action".equals(actionType);
          }
        };

    InternalActionExecutor executor =
        new InternalActionExecutor(repository, List.of(customResolver), engine, platform);

    EventInstanceId instanceId = EventInstanceId.generate();
    EventDefinition hitDef =
        new EventDefinition(
            new EventDefinitionId("test_def"),
            new PhaseId("p1"),
            Map.of(
                new PhaseId("p1"),
                new PhaseDefinition(
                    new PhaseId("p1"),
                    Set.of(),
                    List.of(
                        new io.github.kizio806.spectraevents.core.event.execution.TransitionRule(
                            new io.github.kizio806.spectraevents.core.event.execution.trigger
                                .CoreTriggers.HitsReachedTrigger(),
                            List.of(),
                            java.util.Optional.empty(),
                            List.of())),
                    List.of())));
    registry.register(hitDef, "test.yml");

    EventInstance instance =
        EventInstance.reconstitute(
            instanceId,
            new EventDefinitionId("test_def"),
            EventLifecycleState.RUNNING,
            new PhaseId("p1"));
    repository.save(instance);
    EventRuntimeState state = new EventRuntimeState(instanceId);

    // Custom resolver
    ActionDefinition customAction =
        new io.github.kizio806.spectraevents.application.config.compiled.ConfiguredActionDefinition(
            "custom_resolver_action");
    assertTrue(
        executor
            .executeInternalAction(instance, state, customAction, ExecutionContext.EMPTY)
            .join());

    // Unhandled action returns null
    ActionDefinition unhandledAction =
        new io.github.kizio806.spectraevents.application.config.compiled.ConfiguredActionDefinition(
            "completely_unhandled");
    assertNull(
        executor.executeInternalAction(instance, state, unhandledAction, ExecutionContext.EMPTY));

    // InitializeHealthAction
    assertTrue(
        executor
            .executeInternalAction(
                instance,
                state,
                new CoreActions.InitializeHealthAction(100),
                ExecutionContext.EMPTY)
            .join());
    assertTrue(state.health().isPresent());
    assertEquals(100, state.health().orElseThrow().max());

    // InitializeHitCounterAction
    assertTrue(
        executor
            .executeInternalAction(
                instance,
                state,
                new CoreActions.InitializeHitCounterAction(50),
                ExecutionContext.EMPTY)
            .join());
    assertNotNull(state.hitCounter());
    assertEquals(50, state.hitCounter().maximum());

    // SetLockedAction
    assertTrue(
        executor
            .executeInternalAction(
                instance,
                state,
                new CoreActions.SetLockedAction(Duration.ofMinutes(5)),
                ExecutionContext.EMPTY)
            .join());
    assertTrue(state.isLocked());

    // InitializeSharedLootAction
    assertTrue(
        executor
            .executeInternalAction(
                instance,
                state,
                new CoreActions.InitializeSharedLootAction(
                    List.of(new CoreActions.LootStack("minecraft:diamond", 5))),
                ExecutionContext.EMPTY)
            .join());
    assertTrue(state.sharedLootInitialized());

    // InitializeGroundLootAction (100% chance)
    EventRuntimeState stateGround = new EventRuntimeState(instanceId);
    assertTrue(
        executor
            .executeInternalAction(
                instance,
                stateGround,
                new CoreActions.InitializeGroundLootAction(
                    List.of(new CoreActions.GroundLootEntry("minecraft:gold_ingot", 10, 100))),
                ExecutionContext.EMPTY)
            .join());
    assertTrue(stateGround.sharedLootInitialized());

    // ApplyDamageAction
    assertTrue(
        executor
            .executeInternalAction(
                instance, state, new CoreActions.ApplyDamageAction(20), ExecutionContext.EMPTY)
            .join());
    assertEquals(80, state.health().orElseThrow().current());

    // ApplyCombatDamageAction with no combat damage in context -> returns false
    assertFalse(
        executor
            .executeInternalAction(
                instance,
                state,
                new CoreActions.ApplyCombatDamageAction(50, 0, Duration.ZERO),
                ExecutionContext.EMPTY)
            .join());

    // ApplyCombatDamageAction with combat damage in context
    UUID actor = UUID.randomUUID();
    ExecutionContext combatCtx = ExecutionContext.withCombatDamage(actor, actor, 25.0);
    assertTrue(
        executor
            .executeInternalAction(
                instance,
                state,
                new CoreActions.ApplyCombatDamageAction(50, 0, Duration.ZERO),
                combatCtx)
            .join());
    assertEquals(55, state.health().orElseThrow().current());

    // IncrementHitsAction with hit counter and reaching threshold
    assertTrue(
        executor
            .executeInternalAction(
                instance, state, new CoreActions.IncrementHitsAction(50), combatCtx)
            .join());
    assertTrue(state.hitCounter().isReached());

    // CreateParticipantRewardClaimsAction with participant and 100% chance
    UUID partUser = UUID.randomUUID();
    state.recordParticipant(partUser);
    CoreActions.GroundLootEntry guaranteedItem =
        new CoreActions.GroundLootEntry("minecraft:diamond", 1, 100);
    assertTrue(
        executor
            .executeInternalAction(
                instance,
                state,
                new CoreActions.CreateParticipantRewardClaimsAction(List.of(guaranteedItem)),
                ExecutionContext.EMPTY)
            .join());
    assertTrue(platform.deliveryAttempts.contains(partUser));
  }

  private static final class RecordingPlatformActionPort implements PlatformActionPort {
    private final boolean deliveryResult;
    private List<UUID> announcedPodium = List.of();
    private final List<UUID> deliveryAttempts = new java.util.ArrayList<>();

    private RecordingPlatformActionPort(boolean deliveryResult) {
      this.deliveryResult = deliveryResult;
    }

    @Override
    public CompletableFuture<Boolean> executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> announcePodium(
        EventInstanceId instanceId, List<UUID> rankedPlayerIds) {
      announcedPodium = List.copyOf(rankedPlayerIds);
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> deliverRewardOrDrop(UUID playerId, List<RewardItem> items) {
      deliveryAttempts.add(playerId);
      return CompletableFuture.completedFuture(deliveryResult);
    }
  }

  private static final class NoOpScheduler implements EventTaskScheduler {
    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}
  }
}
