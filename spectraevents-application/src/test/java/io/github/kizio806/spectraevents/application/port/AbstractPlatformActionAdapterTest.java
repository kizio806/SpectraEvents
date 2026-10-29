package io.github.kizio806.spectraevents.application.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractPlatformActionAdapterTest {

  private TestActionAdapter adapter;
  private EventInstance instance;
  private EventRuntimeState state;

  @BeforeEach
  void setUp() {
    adapter = new TestActionAdapter();
    instance =
        EventInstance.create(EventInstanceId.generate(), new EventDefinitionId("test_event"));
    state = new EventRuntimeState(instance.id());
  }

  @Test
  void dispatchesAllSupportedPlatformActions() {
    List<ActionDefinition> actions =
        List.of(
            new PlatformActions.SpawnModelAction("model", 0),
            new PlatformActions.MoveModelAction(),
            new PlatformActions.RemoveModelAction(),
            new PlatformActions.PlayAnimationAction("idle"),
            new PlatformActions.PlaySoundAction("bell", 1.0f, 1.0f),
            new PlatformActions.SpawnParticlesAction("flame", 10),
            new PlatformActions.GiveItemAction("diamond", 1),
            new PlatformActions.OpenSharedLootAction("Loot"),
            new PlatformActions.DropLootAction(5.0, List.of()),
            new PlatformActions.ReleaseGroundLootAction(5.0),
            new PlatformActions.AwardPodiumAction(
                List.of(
                    new PlatformActions.PodiumPool(List.of()),
                    new PlatformActions.PodiumPool(List.of()),
                    new PlatformActions.PodiumPool(List.of()))),
            new PlatformActions.SendMessageAction("hello"),
            new PlatformActions.BroadcastMessageAction("broadcast"),
            new PlatformActions.ShowTitleAction("title", "sub", 10, 20, 10),
            new PlatformActions.SpawnBossAction(0, 0, 0, "zombie", "Boss"),
            new PlatformActions.SpawnMobsAction("wave1", List.of()),
            new PlatformActions.ShowBossbarAction("RED", "SOLID", "Boss", "1.0"),
            new PlatformActions.UpdateBossbarAction("Boss 50%", "0.5"),
            new PlatformActions.RemoveBossbarAction(),
            new PlatformActions.ShowScoreboardAction("Score", List.of("line1")),
            new PlatformActions.UpdateScoreboardAction(List.of("line2")),
            new PlatformActions.RemoveScoreboardAction());

    for (ActionDefinition action : actions) {
      boolean result =
          adapter.executeAction(instance, state, action, ExecutionContext.EMPTY).join();
      assertTrue(result);
    }

    assertEquals(22, adapter.handledActions.size());
  }

  @Test
  void threeArgumentOverloadDelegatesWithEmptyContext() {
    ActionDefinition action = new PlatformActions.MoveModelAction();
    boolean result = adapter.executeAction(instance, state, action).join();
    assertTrue(result);
    assertEquals(1, adapter.handledActions.size());
    assertEquals(ExecutionContext.EMPTY, adapter.lastContext);
  }

  @Test
  void unsupportedActionThrowsFatalActionException() {
    record UnknownAction() implements ActionDefinition {
      @Override
      public String type() {
        return "unsupported_action";
      }
    }

    assertThrows(
        FatalActionException.class,
        () -> adapter.executeAction(instance, state, new UnknownAction(), ExecutionContext.EMPTY));
  }

  private static final class TestActionAdapter extends AbstractPlatformActionAdapter {
    final List<ActionDefinition> handledActions = new ArrayList<>();
    ExecutionContext lastContext;

    private CompletableFuture<Boolean> record(ActionDefinition action, ExecutionContext context) {
      handledActions.add(action);
      this.lastContext = context;
      return CompletableFuture.completedFuture(true);
    }

    @Override
    protected CompletableFuture<Boolean> handleSpawnModel(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.SpawnModelAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleMoveModel(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.MoveModelAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleRemoveModel(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.RemoveModelAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handlePlayAnimation(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.PlayAnimationAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handlePlaySound(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.PlaySoundAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleSpawnParticles(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.SpawnParticlesAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleGiveItem(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.GiveItemAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleOpenSharedLoot(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.OpenSharedLootAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleDropLoot(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.DropLootAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleReleaseGroundLoot(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.ReleaseGroundLootAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleAwardPodium(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.AwardPodiumAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleSendMessage(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.SendMessageAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleBroadcastMessage(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.BroadcastMessageAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleShowTitle(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.ShowTitleAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleSpawnBoss(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.SpawnBossAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleSpawnMobs(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.SpawnMobsAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleShowBossbar(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.ShowBossbarAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleUpdateBossbar(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.UpdateBossbarAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleRemoveBossbar(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.RemoveBossbarAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleShowScoreboard(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.ShowScoreboardAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleUpdateScoreboard(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.UpdateScoreboardAction action,
        ExecutionContext context) {
      return record(action, context);
    }

    @Override
    protected CompletableFuture<Boolean> handleRemoveScoreboard(
        EventInstance instance,
        EventRuntimeState state,
        PlatformActions.RemoveScoreboardAction action,
        ExecutionContext context) {
      return record(action, context);
    }
  }
}
