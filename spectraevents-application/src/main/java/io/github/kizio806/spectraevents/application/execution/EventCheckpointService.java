package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.time.Duration;
import java.util.Objects;

/** Bounded, one-second persistence checkpoint for mutable encounter state. */
final class EventCheckpointService {
  private static final Duration INTERVAL = Duration.ofSeconds(1);

  private final EventInstanceRepository repository;
  private final EventTaskScheduler scheduler;
  private final EventRuntimeStateStore stateStore;
  private final PlatformActionPort platformActionPort;

  EventCheckpointService(
      EventInstanceRepository repository,
      EventTaskScheduler scheduler,
      EventRuntimeStateStore stateStore,
      PlatformActionPort platformActionPort) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    this.platformActionPort = Objects.requireNonNull(platformActionPort, "platformActionPort");
  }

  void start(EventInstanceId instanceId) {
    scheduleNext(instanceId);
  }

  void flushDurably(EventRuntimeState state) {
    repository.saveStateDurably(state);
  }

  private void scheduleNext(EventInstanceId instanceId) {
    Runnable checkpoint =
        () -> {
          var instance = repository.findById(instanceId).orElse(null);
          if (instance != null && instance.state() == EventLifecycleState.RUNNING) {
            stateStore
                .get(instanceId)
                .ifPresent(
                    state -> {
                      repository.saveState(state);
                      platformActionPort.refreshHud(instance, state);
                    });
            scheduleNext(instanceId);
          }
        };
    try {
      scheduler.scheduleGlobal(INTERVAL, checkpoint);
    } catch (UnsupportedOperationException unsupported) {
      scheduler.schedule(instanceId, INTERVAL, checkpoint);
    }
  }
}
