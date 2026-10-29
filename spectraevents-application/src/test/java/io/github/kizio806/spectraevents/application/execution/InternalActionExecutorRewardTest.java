package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.time.Duration;
import java.util.List;
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
