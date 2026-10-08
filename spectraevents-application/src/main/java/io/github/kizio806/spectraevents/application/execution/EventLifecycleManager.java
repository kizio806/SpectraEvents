package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleTransition;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.locks.ReentrantLock;

final class EventLifecycleManager {
  private static final int MAX_RUNNING_INSTANCES = 64;

  private final EngineContext context;
  private PhaseExecutionController phaseController;

  public EventLifecycleManager(EngineContext context) {
    this.context = context;
  }

  public void setPhaseController(PhaseExecutionController phaseController) {
    this.phaseController = phaseController;
  }

  public EventDefinition getDefinition(String definitionId) {
    EventDefinitionId id = new EventDefinitionId(definitionId);
    return context
        .definitionRegistry()
        .get(id)
        .map(RegisteredEventDefinition::definition)
        .orElseThrow(
            () -> new IllegalArgumentException("Event definition not registered: " + definitionId));
  }

  public EventDefinition definitionForInstance(EventInstance instance) {
    return context
        .definitionSnapshots()
        .computeIfAbsent(instance.id(), ignored -> getDefinition(instance.definitionId().value()));
  }

  public EventInstance startEvent(EventDefinition definition, Object platformLocationReference) {
    try {
      return startEventAsync(definition, platformLocationReference).join();
    } catch (CompletionException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Could not start event", cause);
    }
  }

  /**
   * Starts an event after its instance and initial state have committed. Platform adapters must use
   * this asynchronous form so a SQLite flush cannot stall a server or region thread.
   */
  public CompletableFuture<EventInstance> startEventAsync(
      EventDefinition definition, Object platformLocationReference) {
    Objects.requireNonNull(definition, "definition");
    long runningInstances =
        context.repository().findAll().stream()
            .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
            .count();
    if (runningInstances >= MAX_RUNNING_INSTANCES) {
      throw new IllegalStateException(
          "Refusing to start event: maximum of " + MAX_RUNNING_INSTANCES + " running instances");
    }

    EventInstanceId id = EventInstanceId.generate();
    context.definitionSnapshots().put(id, definition);
    EventInstance created = EventInstance.create(id, definition.id());

    EventLifecycleTransition transition = created.start(definition);
    EventInstance running = transition.eventInstance();

    EventRuntimeState state = context.stateStore().getOrCreate(id);
    if (platformLocationReference != null) {
      state.setPlatformLocation(platformLocationReference);
    }
    reserveEncounterZone(definition, id, platformLocationReference, state);
    return context
        .repository()
        .saveWithStateDurablyAsync(running, state)
        .thenApply(
            ignored -> {
              scheduleEncounterDeadline(definition, id);
              if (definition.encounterSettings().reservesZone()) {
                context.checkpointService().start(id);
              }
              try {
                if (phaseController != null) {
                  phaseController.executePhaseEntry(
                      running, definition, definition.initialPhase(), ExecutionContext.EMPTY);
                }
              } catch (RuntimeException exception) {
                failEvent(running.id(), exception);
                throw exception;
              }
              return running;
            })
        .whenComplete(
            (started, failure) -> {
              if (failure != null) {
                context.observedActionCompletions().remove(id);
                context.stateStore().remove(id);
                context.definitionSnapshots().remove(id);
                context.zoneRegistry().release(id);
                context.repository().remove(id);
              }
            });
  }

  public EventInstance cancelEvent(EventInstanceId instanceId) {
    ReentrantLock lock =
        context.instanceLocks().computeIfAbsent(instanceId, ignored -> new ReentrantLock());
    lock.lock();
    try {
      Optional<EventInstance> instanceOpt = context.repository().findById(instanceId);
      if (instanceOpt.isPresent()) {
        EventInstance instance = instanceOpt.get();
        if (instance.state() == EventLifecycleState.RUNNING) {
          EventLifecycleTransition transition = instance.cancel();
          EventInstance cancelled = transition.eventInstance();
          context.repository().save(cancelled);
          cleanupInstance(instanceId);
          return cancelled;
        }
      }
      cleanupInstance(instanceId);
      return null;
    } finally {
      lock.unlock();
    }
  }

  public EventInstance completeEvent(EventInstanceId instanceId) {
    ReentrantLock lock =
        context.instanceLocks().computeIfAbsent(instanceId, ignored -> new ReentrantLock());
    lock.lock();
    try {
      Optional<EventInstance> instanceOpt = context.repository().findById(instanceId);
      if (instanceOpt.isPresent()) {
        EventInstance instance = instanceOpt.get();
        if (instance.state() == EventLifecycleState.RUNNING) {
          context.stateStore().get(instanceId).ifPresent(context.checkpointService()::flushDurably);
          EventLifecycleTransition transition = instance.complete();
          EventInstance completed = transition.eventInstance();
          context.repository().save(completed);
          cleanupInstance(instanceId);
          return completed;
        }
      }
      cleanupInstance(instanceId);
      return null;
    } finally {
      lock.unlock();
    }
  }

  public EventInstance failEvent(EventInstanceId instanceId, Throwable cause) {
    ReentrantLock lock =
        context.instanceLocks().computeIfAbsent(instanceId, ignored -> new ReentrantLock());
    lock.lock();
    try {
      Optional<EventInstance> instanceOpt = context.repository().findById(instanceId);
      if (instanceOpt.isPresent()) {
        EventInstance instance = instanceOpt.get();
        if (instance.state() == EventLifecycleState.RUNNING) {
          EventLifecycleTransition transition = instance.fail();
          EventInstance failed = transition.eventInstance();
          context.repository().save(failed);
          cleanupInstance(instanceId);
          return failed;
        }
      }
      cleanupInstance(instanceId);
      return null;
    } finally {
      lock.unlock();
    }
  }

  void recoverEncounter(EventInstance instance, EventRuntimeState state) {
    state
        .eventZone()
        .ifPresent(
            zone -> {
              EventDefinition definition = definitionForInstance(instance);
              context
                  .zoneRegistry()
                  .reserve(
                      instance.id(),
                      definition.id(),
                      zone,
                      definition.encounterSettings().maxActivePerWorld());
            });
    long deadline = state.encounterDeadlineMillis();
    if (deadline > 0L) {
      long remaining = deadline - System.currentTimeMillis();
      if (remaining <= 0L) {
        failEvent(
            instance.id(), new IllegalStateException("Encounter deadline elapsed during recovery"));
        return;
      }
      Runnable expiry =
          () -> failEvent(instance.id(), new IllegalStateException("Encounter deadline elapsed"));
      try {
        context.scheduler().scheduleGlobal(Duration.ofMillis(remaining), expiry);
      } catch (UnsupportedOperationException unsupported) {
        context.scheduler().schedule(instance.id(), Duration.ofMillis(remaining), expiry);
      }
    }
    if (state.eventZone().isPresent() || state.encounterDeadlineMillis() > 0L) {
      context.checkpointService().start(instance.id());
    }
  }

  private void cleanupInstance(EventInstanceId instanceId) {
    context.scheduler().cancelAll(instanceId);
    context.platformActionPort().cleanupEvent(instanceId);
    context.observedActionCompletions().remove(instanceId);
    context.stateStore().remove(instanceId);
    context.definitionSnapshots().remove(instanceId);
    context.zoneRegistry().release(instanceId);
  }

  private void reserveEncounterZone(
      EventDefinition definition,
      EventInstanceId instanceId,
      Object platformLocationReference,
      EventRuntimeState state) {
    var settings = definition.encounterSettings();
    if (!settings.reservesZone()) {
      return;
    }
    if (!(platformLocationReference instanceof EventLocation location)) {
      throw new IllegalArgumentException(
          "Large encounter '" + definition.id().value() + "' requires an EventLocation");
    }
    EventZone zone = EventZone.at(location, settings.zoneRadius());
    context.zoneRegistry().reserve(instanceId, definition.id(), zone, settings.maxActivePerWorld());
    state.setEventZone(zone);
    state.setMinimumContribution(settings.minimumContribution());
    if (settings.hasDeadline()) {
      state.setEncounterDeadlineMillis(
          Math.addExact(System.currentTimeMillis(), settings.deadline().toMillis()));
    }
  }

  private void scheduleEncounterDeadline(EventDefinition definition, EventInstanceId instanceId) {
    if (!definition.encounterSettings().hasDeadline()) {
      return;
    }
    Duration deadline = definition.encounterSettings().deadline();
    Runnable expiry =
        () ->
            failEvent(
                instanceId,
                new IllegalStateException(
                    "Encounter deadline elapsed for " + definition.id().value()));
    try {
      context.scheduler().scheduleGlobal(deadline, expiry);
    } catch (UnsupportedOperationException unsupported) {
      // Test/minimal schedulers may not provide global tasks. Platform schedulers must provide it
      // so phase transitions cannot accidentally cancel the hard encounter deadline.
      context.scheduler().schedule(instanceId, deadline, expiry);
    }
  }
}
