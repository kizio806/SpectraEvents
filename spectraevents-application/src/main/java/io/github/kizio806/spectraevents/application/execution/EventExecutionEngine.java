package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleTransition;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deterministic execution engine for data-driven event definitions. Evaluates incoming triggers
 * against phase transition rules, checks conditions, executes actions in order, and manages phase
 * entry side-effects and timers.
 */
public final class EventExecutionEngine {
  private static final Logger LOGGER = Logger.getLogger(EventExecutionEngine.class.getName());

  private final EventInstanceRepository repository;
  private final EventDefinitionRegistry definitionRegistry;
  private final EventTaskScheduler scheduler;
  private final PlatformActionPort platformActionPort;
  private final EventRuntimeStateStore stateStore;

  private final java.util.List<IntegrationConditionResolver> conditionResolvers =
      new java.util.ArrayList<>();
  private final java.util.List<IntegrationActionResolver> actionResolvers =
      new java.util.ArrayList<>();

  public EventExecutionEngine(
      EventInstanceRepository repository,
      EventDefinitionRegistry definitionRegistry,
      EventTaskScheduler scheduler,
      PlatformActionPort platformActionPort,
      EventRuntimeStateStore stateStore) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.definitionRegistry = Objects.requireNonNull(definitionRegistry, "definitionRegistry");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    this.platformActionPort = Objects.requireNonNull(platformActionPort, "platformActionPort");
    this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
  }

  public EventRuntimeStateStore stateStore() {
    return stateStore;
  }

  public void registerConditionResolver(IntegrationConditionResolver resolver) {
    conditionResolvers.add(resolver);
  }

  public void registerActionResolver(IntegrationActionResolver resolver) {
    actionResolvers.add(resolver);
  }

  /** Starts a config-driven event instance. */
  public EventInstance startEvent(String definitionId, Object platformLocationReference) {
    EventDefinition definition = getDefinition(definitionId);
    EventInstanceId id = EventInstanceId.generate();
    EventInstance created = EventInstance.create(id, definition.id());

    EventLifecycleTransition transition = created.start(definition);
    EventInstance running = transition.eventInstance();

    repository.save(running);

    EventRuntimeState state = stateStore.getOrCreate(id);
    if (platformLocationReference != null) {
      state.setPlatformLocation(platformLocationReference);
    }

    executePhaseEntry(running, definition, definition.initialPhase());
    return running;
  }

  /** Evaluates an incoming trigger against the current phase of the event instance. */
  public boolean evaluateTrigger(EventInstanceId instanceId, TriggerDefinition trigger) {
    return evaluateTrigger(instanceId, trigger, ExecutionContext.EMPTY);
  }

  /** Evaluates an incoming trigger with execution context. */
  public boolean evaluateTrigger(
      EventInstanceId instanceId, TriggerDefinition trigger, ExecutionContext context) {
    Optional<EventInstance> instanceOpt = repository.findById(instanceId);
    if (instanceOpt.isEmpty()) {
      return false;
    }

    EventInstance instance = instanceOpt.get();
    if (instance.state() != EventLifecycleState.RUNNING) {
      return false;
    }

    EventDefinition definition = getDefinition(instance.definitionId().value());
    PhaseId currentPhaseId =
        instance
            .currentPhase()
            .orElseThrow(() -> new IllegalStateException("Running instance has no current phase"));

    PhaseDefinition phaseDef =
        definition
            .phase(currentPhaseId)
            .orElseThrow(
                () -> new IllegalStateException("Phase unknown to definition: " + currentPhaseId));

    EventRuntimeState state = stateStore.getOrCreate(instanceId);

    // Evaluate rules in deterministic list order
    for (TransitionRule rule : phaseDef.rules()) {
      if (matchesTrigger(rule.trigger(), trigger)
          && evaluateConditions(rule.conditions(), state, context)) {
        // Execute rule actions
        boolean actionsOk = executeActions(instance, state, rule.actions(), context);
        if (!actionsOk) {
          return false;
        }

        // Execute phase transition if target phase specified
        if (rule.targetPhase().isPresent()) {
          PhaseId targetPhase = rule.targetPhase().get();
          EventLifecycleTransition transition = instance.transitionPhase(definition, targetPhase);
          EventInstance nextInstance = transition.eventInstance();
          repository.save(nextInstance);

          executePhaseEntry(nextInstance, definition, targetPhase, context);
        }
        return true;
      }
    }
    return false;
  }

  /** Executes phase entry side-effects and schedules phase timers. */
  public void executePhaseEntry(
      EventInstance instance, EventDefinition definition, PhaseId phaseId) {
    executePhaseEntry(instance, definition, phaseId, ExecutionContext.EMPTY);
  }

  /** Executes phase entry side-effects with context and schedules phase timers. */
  public void executePhaseEntry(
      EventInstance instance,
      EventDefinition definition,
      PhaseId phaseId,
      ExecutionContext context) {
    // Cancel previous phase timers
    scheduler.cancelAll(instance.id());

    PhaseDefinition phaseDef =
        definition
            .phase(phaseId)
            .orElseThrow(() -> new IllegalStateException("Phase unknown: " + phaseId));

    EventRuntimeState state = stateStore.getOrCreate(instance.id());

    // Execute on-enter actions deterministically in list order
    executeActions(instance, state, phaseDef.onEnterActions(), context);

    // Inspect rules for timer_elapsed triggers and schedule tasks
    for (TransitionRule rule : phaseDef.rules()) {
      if ("timer_elapsed".equalsIgnoreCase(rule.trigger().type())) {
        Duration duration = parseDuration(rule.trigger().parameters().get("duration"));
        if (duration != null) {
          TriggerDefinition timerTrigger = rule.trigger();
          long deadline = System.currentTimeMillis() + duration.toMillis();
          state.setTimerDeadlineMillis(deadline);
          repository.saveState(state);
          scheduler.schedule(
              instance.id(),
              duration,
              () -> {
                try {
                  evaluateTrigger(instance.id(), timerTrigger, context);
                } catch (Exception e) {
                  LOGGER.log(
                      Level.SEVERE, "Error executing timer trigger for event " + instance.id(), e);
                }
              });
        }
      }
    }
  }

  /** Cancels an event instance and cleans up runtime state and scheduled timers. */
  public EventInstance cancelEvent(EventInstanceId instanceId) {
    scheduler.cancelAll(instanceId);
    Optional<EventInstance> instanceOpt = repository.findById(instanceId);
    if (instanceOpt.isPresent()) {
      EventInstance instance = instanceOpt.get();
      if (instance.state() == EventLifecycleState.RUNNING) {
        EventLifecycleTransition transition = instance.cancel();
        EventInstance cancelled = transition.eventInstance();
        repository.save(cancelled);
        stateStore.remove(instanceId);
        return cancelled;
      }
    }
    stateStore.remove(instanceId);
    return null;
  }

  /** Completes an event instance and cleans up. */
  public EventInstance completeEvent(EventInstanceId instanceId) {
    scheduler.cancelAll(instanceId);
    Optional<EventInstance> instanceOpt = repository.findById(instanceId);
    if (instanceOpt.isPresent()) {
      EventInstance instance = instanceOpt.get();
      if (instance.state() == EventLifecycleState.RUNNING) {
        EventLifecycleTransition transition = instance.complete();
        EventInstance completed = transition.eventInstance();
        repository.save(completed);
        stateStore.remove(instanceId);
        return completed;
      }
    }
    stateStore.remove(instanceId);
    return null;
  }

  /** Fails an event instance cleanly. */
  public EventInstance failEvent(EventInstanceId instanceId, Throwable cause) {
    scheduler.cancelAll(instanceId);
    Optional<EventInstance> instanceOpt = repository.findById(instanceId);
    if (instanceOpt.isPresent()) {
      EventInstance instance = instanceOpt.get();
      if (instance.state() == EventLifecycleState.RUNNING) {
        EventLifecycleTransition transition = instance.fail();
        EventInstance failed = transition.eventInstance();
        repository.save(failed);
        stateStore.remove(instanceId);
        return failed;
      }
    }
    stateStore.remove(instanceId);
    return null;
  }

  /** Recovers persistent phase timers after a system restart. */
  public void recoverTimers() {
    for (EventInstance instance : repository.findAll()) {
      if (instance.state() == EventLifecycleState.RUNNING) {
        EventRuntimeState state = stateStore.getOrCreate(instance.id());
        long deadline = state.timerDeadlineMillis();
        if (deadline > 0) {
          long remaining = deadline - System.currentTimeMillis();
          if (remaining < 0) remaining = 0;

          EventDefinition definition = getDefinition(instance.definitionId().value());
          PhaseId currentPhaseId = instance.currentPhase().orElse(null);
          if (currentPhaseId != null) {
            PhaseDefinition phaseDef = definition.phase(currentPhaseId).orElse(null);
            if (phaseDef != null) {
              for (TransitionRule rule : phaseDef.rules()) {
                if ("timer_elapsed".equalsIgnoreCase(rule.trigger().type())) {
                  TriggerDefinition timerTrigger = rule.trigger();
                  scheduler.schedule(
                      instance.id(),
                      Duration.ofMillis(remaining),
                      () -> {
                        try {
                          evaluateTrigger(instance.id(), timerTrigger, ExecutionContext.EMPTY);
                        } catch (Exception e) {
                          LOGGER.log(
                              Level.SEVERE,
                              "Error executing recovered timer for " + instance.id(),
                              e);
                        }
                      });
                  break; // Assume max 1 timer rule per phase
                }
              }
            }
          }
        }
      }
    }
  }

  private boolean executeActions(
      EventInstance instance,
      EventRuntimeState state,
      List<ActionDefinition> actions,
      ExecutionContext context) {
    for (ActionDefinition action : actions) {
      try {
        Boolean internalResult = executeInternalAction(instance, state, action, context);
        if (internalResult != null) {
          if (!internalResult) {
            return false;
          }
          continue;
        }
        platformActionPort.executeAction(instance, state, action, context);
      } catch (FatalActionException e) {
        LOGGER.log(
            Level.SEVERE,
            "Fatal action failure during action " + action.type() + " for event " + instance.id(),
            e);
        failEvent(instance.id(), e);
        throw e;
      } catch (Exception e) {
        LOGGER.log(
            Level.WARNING,
            "Non-fatal action failure during action "
                + action.type()
                + " for event "
                + instance.id()
                + ": "
                + e.getMessage(),
            e);
      }
    }
    return true;
  }

  private Boolean executeInternalAction(
      EventInstance instance,
      EventRuntimeState state,
      ActionDefinition action,
      ExecutionContext context)
      throws FatalActionException {

    for (IntegrationActionResolver resolver : actionResolvers) {
      if (resolver.supports(action.type())) {
        return resolver.execute(action, instance, state, context);
      }
    }

    String type = action.type().toLowerCase();
    switch (type) {
      case "initialize_health":
        int maxHealth = getIntParam(action.parameters(), "max", 20);
        state.setHealth(new Health(maxHealth, maxHealth));
        repository.saveState(state);
        return Boolean.TRUE;
      case "set_locked":
        Duration lockDuration = parseDuration(action.parameters().get("duration"));
        long durationMs = lockDuration != null ? lockDuration.toMillis() : 10000L;
        state.setLockedUntilMillis(System.currentTimeMillis() + durationMs);
        repository.saveState(state);
        return Boolean.TRUE;
      case "try_claim":
        String claimantId =
            context != null && context.actor() != null ? context.actor().toString() : "unknown";
        boolean claimed = state.tryClaim(claimantId);
        if (claimed) {
          repository.saveState(state);
        }
        return claimed ? Boolean.TRUE : Boolean.FALSE;
      case "apply_damage":
        int amount = getIntParam(action.parameters(), "amount", 1);
        if (state.health().isPresent()) {
          Health oldHealth = state.health().get();
          Health updated = state.updateHealth(h -> h.damage(amount));

          if (context != null && context.actor() != null) {
            if (context.actor() instanceof java.util.UUID uuid) {
              state.recordDamage(uuid, amount);
            } else {
              try {
                java.lang.reflect.Method m = context.actor().getClass().getMethod("getUniqueId");
                Object res = m.invoke(context.actor());
                if (res instanceof java.util.UUID uuid) {
                  state.recordDamage(uuid, amount);
                }
              } catch (Exception ignored) {
              }
            }
          }

          if (updated != null) {
            EventDefinition definition = getDefinition(instance.definitionId().value());
            List<Integer> declaredThresholds = new java.util.ArrayList<>();
            if (definition != null && instance.currentPhase().isPresent()) {
              PhaseDefinition phaseDef =
                  definition.phase(instance.currentPhase().get()).orElse(null);
              if (phaseDef != null) {
                for (TransitionRule rule : phaseDef.rules()) {
                  if ("health_threshold_crossed".equalsIgnoreCase(rule.trigger().type())) {
                    int t = getIntParam(rule.trigger().parameters(), "threshold", -1);
                    if (t != -1) {
                      declaredThresholds.add(t);
                    }
                  }
                }
              }
            }
            if (!declaredThresholds.isEmpty()) {
              io.github.kizio806.spectraevents.core.gameplay.health.HealthThresholds thresholds =
                  new io.github.kizio806.spectraevents.core.gameplay.health.HealthThresholds(
                      declaredThresholds);
              List<Integer> crossed = thresholds.checkCrossed(oldHealth, updated);
              for (Integer threshold : crossed) {
                evaluateTrigger(
                    instance.id(),
                    new ConfiguredTriggerDefinition(
                        "health_threshold_crossed", java.util.Map.of("threshold", threshold)),
                    context);
              }
            }
            if (updated.isDepleted()) {
              evaluateTrigger(
                  instance.id(), new ConfiguredTriggerDefinition("health_depleted"), context);
            }
          }
          repository.saveState(state);
        }
        return Boolean.TRUE;
      case "complete_event":
        completeEvent(instance.id());
        return Boolean.TRUE;
      case "cancel_event":
        cancelEvent(instance.id());
        return Boolean.TRUE;
      default:
        return null;
    }
  }

  private boolean matchesTrigger(TriggerDefinition ruleTrigger, TriggerDefinition incomingTrigger) {
    if (!ruleTrigger.type().equalsIgnoreCase(incomingTrigger.type())) {
      return false;
    }
    if ("health_threshold_crossed".equalsIgnoreCase(ruleTrigger.type())) {
      int ruleThreshold = getIntParam(ruleTrigger.parameters(), "threshold", -1);
      int incomingThreshold = getIntParam(incomingTrigger.parameters(), "threshold", -2);
      if (ruleThreshold != -1 && incomingThreshold != -2 && ruleThreshold != incomingThreshold) {
        return false;
      }
    }
    return true;
  }

  private boolean evaluateConditions(
      List<ConditionDefinition> conditions, EventRuntimeState state, ExecutionContext context) {
    for (ConditionDefinition cond : conditions) {
      String type = cond.type().toLowerCase();

      boolean resolvedByIntegration = false;
      for (IntegrationConditionResolver resolver : conditionResolvers) {
        if (resolver.supports(type)) {
          if (!resolver.resolve(cond, null, state, context)) {
            return false;
          }
          resolvedByIntegration = true;
          break;
        }
      }
      if (resolvedByIntegration) continue;

      if ("not_locked".equals(type) && state.isLocked()) {
        return false;
      }
      if ("is_locked".equals(type) && !state.isLocked()) {
        return false;
      }
    }
    return true;
  }

  private EventDefinition getDefinition(String definitionId) {
    EventDefinitionId id = new EventDefinitionId(definitionId);
    return definitionRegistry
        .get(id)
        .map(RegisteredEventDefinition::definition)
        .orElseThrow(
            () -> new IllegalArgumentException("Event definition not registered: " + definitionId));
  }

  private Duration parseDuration(Object obj) {
    if (obj == null) return null;
    if (obj instanceof Duration d) return d;
    String str = String.valueOf(obj).trim();
    if (str.endsWith("s")) {
      long sec = Long.parseLong(str.substring(0, str.length() - 1));
      return Duration.ofSeconds(sec);
    }
    if (str.endsWith("ms")) {
      long ms = Long.parseLong(str.substring(0, str.length() - 2));
      return Duration.ofMillis(ms);
    }
    if (str.endsWith("m")) {
      long min = Long.parseLong(str.substring(0, str.length() - 1));
      return Duration.ofMinutes(min);
    }
    try {
      long sec = Long.parseLong(str);
      return Duration.ofSeconds(sec);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private int getIntParam(java.util.Map<String, Object> params, String key, int defaultValue) {
    if (params == null) return defaultValue;
    Object val = params.get(key);
    if (val == null) return defaultValue;
    if (val instanceof Number n) return n.intValue();
    try {
      return Integer.parseInt(String.valueOf(val));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
