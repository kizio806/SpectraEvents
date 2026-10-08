package io.github.kizio806.spectraevents.application.schedule;

import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Starts configured events on their matching minute. It deliberately performs no catch-up after a
 * restart: only a minute observed while this service is running is eligible.
 */
public final class EventScheduleService {
  private final EventTaskScheduler scheduler;
  private final EventOrchestrationService orchestrationService;
  private final Clock clock;
  private final Consumer<String> diagnostics;
  private volatile boolean running;
  private volatile List<EventSchedule> schedules = List.of();

  public EventScheduleService(
      EventTaskScheduler scheduler,
      EventOrchestrationService orchestrationService,
      Clock clock,
      Consumer<String> diagnostics) {
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    this.orchestrationService =
        Objects.requireNonNull(orchestrationService, "orchestrationService");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
  }

  /** Replaces the schedule declaration atomically; a reload never starts missed executions. */
  public synchronized void start(List<EventSchedule> configuredSchedules) {
    schedules = List.copyOf(Objects.requireNonNull(configuredSchedules, "configuredSchedules"));
    if (running) {
      return;
    }
    running = true;
    scheduleNextMinute();
  }

  public synchronized void replace(List<EventSchedule> configuredSchedules) {
    schedules = List.copyOf(Objects.requireNonNull(configuredSchedules, "configuredSchedules"));
  }

  public synchronized void stop() {
    running = false;
    schedules = List.of();
  }

  public List<EventSchedule> schedules() {
    return schedules;
  }

  private void scheduleNextMinute() {
    ZonedDateTime now = ZonedDateTime.now(clock);
    ZonedDateTime next = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
    Duration delay = Duration.between(now, next);
    scheduler.scheduleGlobal(delay, this::onMinute);
  }

  @SuppressWarnings(
      "FutureReturnValueIgnored") // Completion is observed for per-schedule diagnostics.
  private void onMinute() {
    if (!running) {
      return;
    }
    for (EventSchedule schedule : schedules) {
      try {
        ZonedDateTime localTime = ZonedDateTime.now(clock).withZoneSameInstant(schedule.zoneId());
        if (schedule.cron().matches(localTime)) {
          orchestrationService
              .startDefinitionAsync(
                  schedule.definitionId(), schedule.location(), schedule.parameterOverrides())
              .whenComplete(
                  (started, failure) -> {
                    if (failure == null) {
                      diagnostics.accept(
                          "Schedule " + schedule.id() + " started " + schedule.definitionId());
                    } else {
                      Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                      diagnostics.accept(
                          "Schedule "
                              + schedule.id()
                              + " skipped: "
                              + (cause.getMessage() == null
                                  ? cause.getClass().getSimpleName()
                                  : cause.getMessage()));
                    }
                  });
        }
      } catch (RuntimeException exception) {
        diagnostics.accept(
            "Schedule "
                + schedule.id()
                + " skipped: "
                + (exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage()));
      }
    }
    if (running) {
      scheduleNextMinute();
    }
  }
}
