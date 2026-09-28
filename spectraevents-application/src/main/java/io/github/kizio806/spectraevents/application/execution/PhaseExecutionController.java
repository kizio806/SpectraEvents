package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleTransition;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

final class PhaseExecutionController {
  private static final Logger LOGGER = Logger.getLogger(PhaseExecutionController.class.getName());
  private static final Duration MAX_TIMER_DURATION = Duration.ofDays(7);

  private final EngineContext context;
  private final ActionExecutionCoordinator actionCoordinator;
  private final EventTriggerEvaluator triggerEvaluator;
  private final EventLifecycleManager lifecycleManager;

  public PhaseExecutionController(
      EngineContext context,
      ActionExecutionCoordinator actionCoordinator,
      EventTriggerEvaluator triggerEvaluator,
      EventLifecycleManager lifecycleManager) {
    this.context = context;
    this.actionCoordinator = actionCoordinator;
    this.triggerEvaluator = triggerEvaluator;
    this.lifecycleManager = lifecycleManager;
  }

  public boolean evaluateTrigger(
      EventInstanceId instanceId, TriggerDefinition trigger, ExecutionContext execContext) {
    ReentrantLock lock =
        context.instanceLocks().computeIfAbsent(instanceId, ignored -> new ReentrantLock());
    lock.lock();
    try {
      return evaluateTriggerLocked(instanceId, trigger, execContext);
    } finally {
      lock.unlock();
    }
  }

  private boolean evaluateTriggerLocked(
      EventInstanceId instanceId, TriggerDefinition trigger, ExecutionContext execContext) {
    Optional<EventInstance> instanceOpt = context.repository().findById(instanceId);
    if (instanceOpt.isEmpty()) return false;
    EventInstance instance = instanceOpt.get();
    if (instance.state() != EventLifecycleState.RUNNING) return false;

    EventDefinition definition = lifecycleManager.definitionForInstance(instance);
    PhaseId currentPhaseId =
        instance
            .currentPhase()
            .orElseThrow(() -> new IllegalStateException("Running instance has no current phase"));
    PhaseDefinition phaseDef =
        definition
            .phase(currentPhaseId)
            .orElseThrow(
                () -> new IllegalStateException("Phase unknown to definition: " + currentPhaseId));

    EventRuntimeState state = context.stateStore().getOrCreate(instanceId);

    for (TransitionRule rule : phaseDef.rules()) {
      if (triggerEvaluator.matchesTrigger(rule.trigger(), trigger)
          && triggerEvaluator.evaluateConditions(rule.conditions(), state, execContext)) {
        if (actionCoordinator.containsRewardAction(rule.actions())
            && !actionCoordinator.acceptRewardClaim(state, trigger, execContext)) {
          return false;
        }
        actionCoordinator.observeActionCompletion(
            instance.id(),
            actionCoordinator.executeActions(instance, state, rule.actions(), execContext),
            (actionsOk, ex) -> {
              if (ex != null) {
                lifecycleManager.failEvent(
                    instance.id(),
                    ex instanceof Exception exception ? exception : new RuntimeException(ex));
                return;
              }
              if (actionsOk == null || !actionsOk) return;

              if (rule.targetPhase().isPresent()) {
                PhaseId targetPhase = rule.targetPhase().get();
                ReentrantLock l =
                    context
                        .instanceLocks()
                        .computeIfAbsent(instance.id(), ignored -> new ReentrantLock());
                l.lock();
                try {
                  Optional<EventInstance> latestInstance =
                      context.repository().findById(instance.id());
                  if (latestInstance.isEmpty()
                      || latestInstance.get().state() != EventLifecycleState.RUNNING) return;
                  EventLifecycleTransition transition =
                      latestInstance.get().transitionPhase(definition, targetPhase);
                  EventInstance nextInstance = transition.eventInstance();
                  context.repository().save(nextInstance);

                  try {
                    executePhaseEntry(nextInstance, definition, targetPhase, execContext);
                  } catch (RuntimeException exception) {
                    LOGGER.log(Level.SEVERE, "Error during phase transition", exception);
                    lifecycleManager.failEvent(nextInstance.id(), exception);
                  }
                } catch (RuntimeException exception) {
                  LOGGER.log(Level.SEVERE, "Error during phase transition outer", exception);
                } finally {
                  l.unlock();
                }
              }
            });
        return true;
      }
    }
    return false;
  }

  public void executePhaseEntry(
      EventInstance instance,
      EventDefinition definition,
      PhaseId phaseId,
      ExecutionContext execContext) {
    context.scheduler().cancelAll(instance.id());
    PhaseDefinition phaseDef =
        definition
            .phase(phaseId)
            .orElseThrow(() -> new IllegalStateException("Phase unknown: " + phaseId));
    EventRuntimeState state = context.stateStore().getOrCreate(instance.id());

    actionCoordinator.observeActionCompletion(
        instance.id(),
        actionCoordinator.executeActions(instance, state, phaseDef.onEnterActions(), execContext),
        (ok, ex) -> {
          if (ex != null) {
            lifecycleManager.failEvent(
                instance.id(),
                ex instanceof Exception exception ? exception : new RuntimeException(ex));
            return;
          }

          for (TransitionRule rule : phaseDef.rules()) {
            if (rule.trigger() instanceof CoreTriggers.TimerElapsedTrigger timerTriggerRule) {
              Duration duration = timerTriggerRule.duration();
              if (duration == null || duration.isZero() || duration.isNegative())
                throw new FatalActionException(
                    "timer_elapsed.duration must be a positive duration");
              if (duration.compareTo(MAX_TIMER_DURATION) > 0)
                throw new FatalActionException(
                    "timer_elapsed.duration must not exceed " + MAX_TIMER_DURATION);

              TriggerDefinition timerTrigger = rule.trigger();
              long deadline = Math.addExact(System.currentTimeMillis(), duration.toMillis());

              ReentrantLock l =
                  context
                      .instanceLocks()
                      .computeIfAbsent(instance.id(), ignored -> new ReentrantLock());
              l.lock();
              try {
                EventRuntimeState latestState = context.stateStore().getOrCreate(instance.id());
                latestState.setTimerDeadlineMillis(deadline);
                context.repository().saveState(latestState);
              } finally {
                l.unlock();
              }

              context
                  .scheduler()
                  .schedule(
                      instance.id(),
                      duration,
                      () -> {
                        try {
                          evaluateTrigger(instance.id(), timerTrigger, execContext);
                        } catch (RuntimeException e) {
                          LOGGER.log(
                              Level.SEVERE,
                              "Error executing timer trigger for event " + instance.id(),
                              e);
                        }
                      });
            }
          }
        });
  }

  public void recoverTimers() {
    for (EventInstance instance : context.repository().findAll()) {
      if (instance.state() == EventLifecycleState.RUNNING) {
        EventDefinition definition;
        try {
          definition = lifecycleManager.definitionForInstance(instance);
        } catch (IllegalArgumentException exception) {
          LOGGER.log(
              Level.WARNING,
              "Cannot recover event instance "
                  + instance.id()
                  + " because definition '"
                  + instance.definitionId().value()
                  + "' is not registered; marking the instance as failed.");
          lifecycleManager.failEvent(instance.id(), exception);
          continue;
        }

        context.repository().findState(instance.id()).ifPresent(context.stateStore()::put);
        EventRuntimeState state = context.stateStore().getOrCreate(instance.id());
        try {
          lifecycleManager.recoverEncounter(instance, state);
        } catch (RuntimeException exception) {
          lifecycleManager.failEvent(instance.id(), exception);
          continue;
        }
        actionCoordinator.recoverPlatformResources(instance, state, definition);

        long deadline = state.timerDeadlineMillis();
        if (deadline > 0) {
          long remaining = deadline - System.currentTimeMillis();
          if (remaining < 0) remaining = 0;

          PhaseId currentPhaseId = instance.currentPhase().orElse(null);
          if (currentPhaseId != null) {
            PhaseDefinition phaseDef = definition.phase(currentPhaseId).orElse(null);
            if (phaseDef != null) {
              for (TransitionRule rule : phaseDef.rules()) {
                if (rule.trigger() instanceof CoreTriggers.TimerElapsedTrigger) {
                  TriggerDefinition timerTrigger = rule.trigger();
                  context
                      .scheduler()
                      .schedule(
                          instance.id(),
                          Duration.ofMillis(remaining),
                          () -> {
                            try {
                              evaluateTrigger(instance.id(), timerTrigger, ExecutionContext.EMPTY);
                            } catch (RuntimeException e) {
                              LOGGER.log(
                                  Level.SEVERE,
                                  "Error executing recovered timer for " + instance.id(),
                                  e);
                            }
                          });
                  break;
                }
              }
            }
          }
        }
      }
    }
  }
}
