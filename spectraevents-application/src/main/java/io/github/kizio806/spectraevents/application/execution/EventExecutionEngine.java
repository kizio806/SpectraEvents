package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic execution engine for data-driven event definitions. This acts as a facade,
 * delegating to focused components.
 */
public final class EventExecutionEngine {
  private final EngineContext context;
  private final EventLifecycleManager lifecycleManager;
  private final PhaseExecutionController phaseController;
  private final ActionExecutionCoordinator actionCoordinator;

  private final java.util.List<IntegrationConditionResolver> conditionResolvers =
      new java.util.ArrayList<>();
  private final java.util.List<IntegrationActionResolver> actionResolvers =
      new java.util.ArrayList<>();
  private final InternalActionExecutor internalActionExecutor;
  private final EventTriggerEvaluator triggerEvaluator;

  public EventExecutionEngine(
      EventInstanceRepository repository,
      EventDefinitionRegistry definitionRegistry,
      EventTaskScheduler scheduler,
      PlatformActionPort platformActionPort,
      EventRuntimeStateStore stateStore) {
    this.context =
        new EngineContext(
            repository, definitionRegistry, scheduler, platformActionPort, stateStore);
    this.internalActionExecutor = new InternalActionExecutor(repository, actionResolvers, this);
    this.triggerEvaluator = new EventTriggerEvaluator(conditionResolvers);

    this.lifecycleManager = new EventLifecycleManager(this.context);
    this.actionCoordinator =
        new ActionExecutionCoordinator(
            this.context, this.internalActionExecutor, this.lifecycleManager);
    this.phaseController =
        new PhaseExecutionController(
            this.context, this.actionCoordinator, this.triggerEvaluator, this.lifecycleManager);
    this.lifecycleManager.setPhaseController(this.phaseController);

    this.context
        .platformActionPort()
        .setFatalActionHandler((instanceId, cause) -> failEvent(instanceId, cause));
  }

  public EventRuntimeStateStore stateStore() {
    return context.stateStore();
  }

  public void registerConditionResolver(IntegrationConditionResolver resolver) {
    conditionResolvers.add(resolver);
  }

  public void registerActionResolver(IntegrationActionResolver resolver) {
    actionResolvers.add(resolver);
  }

  public EventInstance startEvent(String definitionId, Object platformLocationReference) {
    return lifecycleManager.startEvent(
        lifecycleManager.getDefinition(definitionId), platformLocationReference);
  }

  public EventInstance startEvent(EventDefinition definition, Object platformLocationReference) {
    return lifecycleManager.startEvent(definition, platformLocationReference);
  }

  public boolean evaluateTrigger(EventInstanceId instanceId, TriggerDefinition trigger) {
    return phaseController.evaluateTrigger(instanceId, trigger, ExecutionContext.EMPTY);
  }

  public EventInstance triggerManualTransition(EventInstanceId instanceId) {
    Objects.requireNonNull(instanceId, "instanceId");
    boolean handled = evaluateTrigger(instanceId, new ConfiguredTriggerDefinition("manual"));
    if (!handled) {
      throw new IllegalStateException(
          "No manual transition is available for event instance " + instanceId);
    }
    return context
        .repository()
        .findById(instanceId)
        .orElseThrow(() -> new IllegalStateException("Event instance disappeared: " + instanceId));
  }

  public boolean evaluateTrigger(
      EventInstanceId instanceId, TriggerDefinition trigger, ExecutionContext context) {
    return phaseController.evaluateTrigger(instanceId, trigger, context);
  }

  /** Records one owned wave entity death and emits {@code wave_cleared} exactly once. */
  public boolean recordWaveEntityDeath(
      EventInstanceId instanceId,
      String waveId,
      java.util.UUID entityId,
      ExecutionContext context) {
    Objects.requireNonNull(instanceId, "instanceId");
    Objects.requireNonNull(waveId, "waveId");
    Objects.requireNonNull(entityId, "entityId");
    EventRuntimeState state = context().stateStore().get(instanceId).orElse(null);
    if (state == null || !state.recordWaveEntityDeath(waveId, entityId)) {
      return false;
    }
    return evaluateTrigger(
        instanceId,
        new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
            .WaveClearedTrigger(waveId),
        context);
  }

  /**
   * Records real platform combat against a tagged event entity without changing its native health.
   */
  public void recordExternalContribution(
      EventInstanceId instanceId, java.util.UUID playerId, double damage) {
    if (playerId == null || damage <= 0.0d) {
      return;
    }
    context()
        .stateStore()
        .get(instanceId)
        .ifPresent(state -> state.recordDamage(playerId, Math.max(1, (int) Math.ceil(damage))));
  }

  private EngineContext context() {
    return context;
  }

  public void executePhaseEntry(
      EventInstance instance, EventDefinition definition, PhaseId phaseId) {
    phaseController.executePhaseEntry(instance, definition, phaseId, ExecutionContext.EMPTY);
  }

  public void executePhaseEntry(
      EventInstance instance,
      EventDefinition definition,
      PhaseId phaseId,
      ExecutionContext context) {
    phaseController.executePhaseEntry(instance, definition, phaseId, context);
  }

  public EventInstance cancelEvent(EventInstanceId instanceId) {
    return lifecycleManager.cancelEvent(instanceId);
  }

  public EventInstance completeEvent(EventInstanceId instanceId) {
    return lifecycleManager.completeEvent(instanceId);
  }

  public EventInstance failEvent(EventInstanceId instanceId, Throwable cause) {
    return lifecycleManager.failEvent(instanceId, cause);
  }

  public void recoverTimers() {
    phaseController.recoverTimers();
  }

  public void shutdown() {
    context.scheduler().cancelAll();
    context.platformActionPort().cleanupAll();
    context.stateStore().clear();
    context.instanceLocks().clear();
    context.definitionSnapshots().clear();
  }

  public EventDefinition definitionForInstance(EventInstance instance) {
    return lifecycleManager.definitionForInstance(instance);
  }

  public ExecutionDiagnostics diagnostics(EventInstanceId instanceId) {
    Optional<EventRuntimeState> state = context.stateStore().get(instanceId);
    return new ExecutionDiagnostics(
        state.isPresent(),
        context.scheduler().pendingTaskCount(instanceId),
        context.platformActionPort().resourceCount(instanceId),
        state.map(EventRuntimeState::claimant).orElse(null));
  }

  public Optional<ExecutionStatus> status(EventInstanceId instanceId) {
    return context
        .stateStore()
        .get(Objects.requireNonNull(instanceId, "instanceId"))
        .map(
            state ->
                new ExecutionStatus(
                    state
                        .platformLocation()
                        .filter(EventLocation.class::isInstance)
                        .map(EventLocation.class::cast)
                        .orElse(null),
                    state.currentHealth(),
                    state.maxHealth(),
                    state.hitCounter() != null ? state.hitCounter().current() : 0,
                    state.hitCounter() != null ? state.hitCounter().maximum() : 0,
                    state.timerDeadlineMillis(),
                    state.isLocked(),
                    state.claimant()));
  }

  public record ExecutionDiagnostics(
      boolean runtimeStatePresent, int pendingTasks, int platformResources, String claimant) {}

  public record ExecutionStatus(
      EventLocation location,
      int currentHealth,
      int maxHealth,
      int currentHits,
      int maxHits,
      long timerDeadlineMillis,
      boolean locked,
      String claimant) {}
}
