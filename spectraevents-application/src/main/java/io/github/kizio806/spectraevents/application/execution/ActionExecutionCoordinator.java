package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ActionExecutionCoordinator {
  private static final Logger LOGGER = Logger.getLogger(ActionExecutionCoordinator.class.getName());
  private static final Set<String> REWARD_ACTION_TYPES =
      Set.of("give_item", "drop_loot", "release_ground_loot", "award_podium");
  private static final Set<String> RECOVERABLE_RESOURCE_ACTION_TYPES =
      Set.of(
          "spawn_model",
          "play_animation",
          "move_model",
          "spawn_boss",
          "spawn_entity",
          "spawn_mobs",
          "spawn_wave",
          "release_ground_loot",
          "show_bossbar",
          "create_bossbar",
          "update_bossbar",
          "show_scoreboard",
          "create_scoreboard",
          "update_scoreboard");

  private final EngineContext context;
  private final InternalActionExecutor internalActionExecutor;
  private final EventLifecycleManager lifecycleManager;

  public ActionExecutionCoordinator(
      EngineContext context,
      InternalActionExecutor internalActionExecutor,
      EventLifecycleManager lifecycleManager) {
    this.context = context;
    this.internalActionExecutor = internalActionExecutor;
    this.lifecycleManager = lifecycleManager;
  }

  public boolean containsRewardAction(List<ActionDefinition> actions) {
    return actions.stream()
        .anyMatch(
            action ->
                REWARD_ACTION_TYPES.contains(action.type().toLowerCase(java.util.Locale.ROOT)));
  }

  public CompletableFuture<Boolean> acceptRewardClaim(
      EventRuntimeState state, TriggerDefinition trigger, ExecutionContext execContext) {
    String claimantId =
        execContext != null && execContext.actor() != null
            ? execContext.actor().toString()
            : "system:" + trigger.type().toLowerCase(java.util.Locale.ROOT);
    if (state.isClaimed() || !state.tryClaim(claimantId)) {
      return CompletableFuture.completedFuture(false);
    }
    return context.repository().saveStateDurablyAsync(state).thenApply(ignored -> true);
  }

  public void observeActionCompletion(
      EventInstanceId instanceId,
      CompletableFuture<Boolean> actionFuture,
      BiConsumer<? super Boolean, ? super Throwable> completionHandler) {
    Set<CompletableFuture<Boolean>> completions =
        context
            .observedActionCompletions()
            .computeIfAbsent(instanceId, ignored -> ConcurrentHashMap.newKeySet());
    completions.removeIf(CompletableFuture::isDone);
    completions.add(actionFuture.whenComplete(completionHandler));
  }

  public CompletableFuture<Boolean> executeActions(
      EventInstance instance,
      EventRuntimeState state,
      List<ActionDefinition> actions,
      ExecutionContext execContext) {
    CompletableFuture<Boolean> chain = CompletableFuture.completedFuture(true);
    for (ActionDefinition action : actions) {
      chain =
          chain.thenCompose(
              ok -> {
                if (!ok) return CompletableFuture.completedFuture(false);
                try {
                  CompletableFuture<Boolean> internalFuture =
                      internalActionExecutor.executeInternalAction(
                          instance, state, action, execContext);
                  if (internalFuture != null) return internalFuture;
                  return context
                      .platformActionPort()
                      .executeAction(instance, state, action, execContext);
                } catch (FatalActionException e) {
                  LOGGER.log(
                      Level.SEVERE,
                      "Fatal action failure during action "
                          + action.type()
                          + " for event "
                          + instance.id(),
                      e);
                  lifecycleManager.failEvent(instance.id(), e);
                  throw e;
                } catch (RuntimeException e) {
                  FatalActionException fatal =
                      new FatalActionException(
                          "Action " + action.type() + " failed for event " + instance.id(), e);
                  LOGGER.log(Level.SEVERE, fatal.getMessage(), fatal);
                  lifecycleManager.failEvent(instance.id(), fatal);
                  throw fatal;
                }
              });
    }
    return chain;
  }

  public void recoverPlatformResources(
      EventInstance instance, EventRuntimeState state, EventDefinition definition) {
    PhaseId currentPhase = instance.currentPhase().orElse(null);
    if (currentPhase == null) return;
    PhaseDefinition phase = definition.phase(currentPhase).orElse(null);
    if (phase == null) return;

    context.platformActionPort().cleanupEvent(instance.id());
    for (ActionDefinition action : phase.onEnterActions()) {
      if (RECOVERABLE_RESOURCE_ACTION_TYPES.contains(
          action.type().toLowerCase(java.util.Locale.ROOT))) {
        try {
          observeActionCompletion(
              instance.id(),
              context
                  .platformActionPort()
                  .executeAction(instance, state, action, ExecutionContext.EMPTY),
              (ok, ex) -> {
                if (ex != null) {
                  lifecycleManager.failEvent(
                      instance.id(),
                      ex instanceof Exception exception ? exception : new RuntimeException(ex));
                }
              });
        } catch (RuntimeException exception) {
          lifecycleManager.failEvent(instance.id(), exception);
        }
      }
    }
  }
}
