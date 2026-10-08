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
    if (action instanceof PlatformActions.SpawnModelAction spawnModelAction) {
      return handleSpawnModel(instance, state, spawnModelAction, context);
    } else if (action instanceof PlatformActions.MoveModelAction moveModelAction) {
      return handleMoveModel(instance, state, moveModelAction, context);
    } else if (action instanceof PlatformActions.RemoveModelAction removeModelAction) {
      return handleRemoveModel(instance, state, removeModelAction, context);
    } else if (action instanceof PlatformActions.PlayAnimationAction playAnimationAction) {
      return handlePlayAnimation(instance, state, playAnimationAction, context);
    } else if (action instanceof PlatformActions.PlaySoundAction playSoundAction) {
      return handlePlaySound(instance, state, playSoundAction, context);
    } else if (action instanceof PlatformActions.SpawnParticlesAction spawnParticlesAction) {
      return handleSpawnParticles(instance, state, spawnParticlesAction, context);
    } else if (action instanceof PlatformActions.GiveItemAction giveItemAction) {
      return handleGiveItem(instance, state, giveItemAction, context);
    } else if (action instanceof PlatformActions.OpenSharedLootAction openSharedLootAction) {
      return handleOpenSharedLoot(instance, state, openSharedLootAction, context);
    } else if (action instanceof PlatformActions.DropLootAction dropLootAction) {
      return handleDropLoot(instance, state, dropLootAction, context);
    } else if (action instanceof PlatformActions.ReleaseGroundLootAction releaseGroundLootAction) {
      return handleReleaseGroundLoot(instance, state, releaseGroundLootAction, context);
    } else if (action instanceof PlatformActions.AwardPodiumAction awardPodiumAction) {
      return handleAwardPodium(instance, state, awardPodiumAction, context);
    } else if (action instanceof PlatformActions.SendMessageAction sendMessageAction) {
      return handleSendMessage(instance, state, sendMessageAction, context);
    } else if (action instanceof PlatformActions.BroadcastMessageAction broadcastMessageAction) {
      return handleBroadcastMessage(instance, state, broadcastMessageAction, context);
    } else if (action instanceof PlatformActions.ShowTitleAction showTitleAction) {
      return handleShowTitle(instance, state, showTitleAction, context);
    } else if (action instanceof PlatformActions.SpawnBossAction spawnBossAction) {
      return handleSpawnBoss(instance, state, spawnBossAction, context);
    } else if (action instanceof PlatformActions.SpawnMobsAction spawnMobsAction) {
      return handleSpawnMobs(instance, state, spawnMobsAction, context);
    } else if (action instanceof PlatformActions.ShowBossbarAction showBossbarAction) {
      return handleShowBossbar(instance, state, showBossbarAction, context);
    } else if (action instanceof PlatformActions.UpdateBossbarAction updateBossbarAction) {
      return handleUpdateBossbar(instance, state, updateBossbarAction, context);
    } else if (action instanceof PlatformActions.RemoveBossbarAction removeBossbarAction) {
      return handleRemoveBossbar(instance, state, removeBossbarAction, context);
    } else if (action instanceof PlatformActions.ShowScoreboardAction showScoreboardAction) {
      return handleShowScoreboard(instance, state, showScoreboardAction, context);
    } else if (action instanceof PlatformActions.UpdateScoreboardAction updateScoreboardAction) {
      return handleUpdateScoreboard(instance, state, updateScoreboardAction, context);
    } else if (action instanceof PlatformActions.RemoveScoreboardAction removeScoreboardAction) {
      return handleRemoveScoreboard(instance, state, removeScoreboardAction, context);
    }

    throw new FatalActionException(
        "Unsupported action type '" + action.type() + "' for instance " + instance.id());
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
