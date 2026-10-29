package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractPlatformActionAdapter implements PlatformActionPort {

  @Override
  public CompletableFuture<Boolean> executeAction(
      EventInstance instance, EventRuntimeState state, ActionDefinition action) {
    return executeAction(instance, state, action, ExecutionContext.EMPTY);
  }

  @Override
  public CompletableFuture<Boolean> executeAction(
      EventInstance instance,
      EventRuntimeState state,
      ActionDefinition action,
      ExecutionContext context) {
    return switch (action) {
      case PlatformActions.SpawnModelAction a -> handleSpawnModel(instance, state, a, context);
      case PlatformActions.MoveModelAction a -> handleMoveModel(instance, state, a, context);
      case PlatformActions.RemoveModelAction a -> handleRemoveModel(instance, state, a, context);
      case PlatformActions.PlayAnimationAction a ->
          handlePlayAnimation(instance, state, a, context);
      case PlatformActions.PlaySoundAction a -> handlePlaySound(instance, state, a, context);
      case PlatformActions.SpawnParticlesAction a ->
          handleSpawnParticles(instance, state, a, context);
      case PlatformActions.GiveItemAction a -> handleGiveItem(instance, state, a, context);
      case PlatformActions.OpenSharedLootAction a ->
          handleOpenSharedLoot(instance, state, a, context);
      case PlatformActions.DropLootAction a -> handleDropLoot(instance, state, a, context);
      case PlatformActions.ReleaseGroundLootAction a ->
          handleReleaseGroundLoot(instance, state, a, context);
      case PlatformActions.AwardPodiumAction a -> handleAwardPodium(instance, state, a, context);
      case PlatformActions.SendMessageAction a -> handleSendMessage(instance, state, a, context);
      case PlatformActions.BroadcastMessageAction a ->
          handleBroadcastMessage(instance, state, a, context);
      case PlatformActions.ShowTitleAction a -> handleShowTitle(instance, state, a, context);
      case PlatformActions.SpawnBossAction a -> handleSpawnBoss(instance, state, a, context);
      case PlatformActions.SpawnMobsAction a -> handleSpawnMobs(instance, state, a, context);
      case PlatformActions.ShowBossbarAction a -> handleShowBossbar(instance, state, a, context);
      case PlatformActions.UpdateBossbarAction a ->
          handleUpdateBossbar(instance, state, a, context);
      case PlatformActions.RemoveBossbarAction a ->
          handleRemoveBossbar(instance, state, a, context);
      case PlatformActions.ShowScoreboardAction a ->
          handleShowScoreboard(instance, state, a, context);
      case PlatformActions.UpdateScoreboardAction a ->
          handleUpdateScoreboard(instance, state, a, context);
      case PlatformActions.RemoveScoreboardAction a ->
          handleRemoveScoreboard(instance, state, a, context);
      default ->
          throw new FatalActionException(
              "Unsupported action type '" + action.type() + "' for instance " + instance.id());
    };
  }

  protected abstract CompletableFuture<Boolean> handleSpawnModel(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SpawnModelAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleMoveModel(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.MoveModelAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleRemoveModel(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveModelAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handlePlayAnimation(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.PlayAnimationAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handlePlaySound(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.PlaySoundAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleSpawnParticles(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SpawnParticlesAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleGiveItem(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.GiveItemAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleOpenSharedLoot(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.OpenSharedLootAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleDropLoot(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.DropLootAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleReleaseGroundLoot(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ReleaseGroundLootAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleAwardPodium(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.AwardPodiumAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleSendMessage(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SendMessageAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleBroadcastMessage(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.BroadcastMessageAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleShowTitle(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowTitleAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleSpawnBoss(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SpawnBossAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleSpawnMobs(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.SpawnMobsAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleShowBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowBossbarAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleUpdateBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.UpdateBossbarAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleRemoveBossbar(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveBossbarAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleShowScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.ShowScoreboardAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleUpdateScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.UpdateScoreboardAction action,
      ExecutionContext context);

  protected abstract CompletableFuture<Boolean> handleRemoveScoreboard(
      EventInstance instance,
      EventRuntimeState state,
      PlatformActions.RemoveScoreboardAction action,
      ExecutionContext context);
}
