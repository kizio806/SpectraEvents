package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.time.Duration;

/**
 * Port for scheduling temporal logic (e.g., phase transitions) for event instances. Implementations
 * must map these tasks to safe, appropriate platform schedulers (e.g. Paper/Folia).
 */
public interface EventTaskScheduler {

  /**
   * Schedules a task to run after the specified delay for a given event instance.
   *
   * @param eventId the event instance ID owning the task
   * @param delay the duration to delay before executing the task
   * @param task the logic to execute
   */
  void schedule(EventInstanceId eventId, Duration delay, Runnable task);

  /**
   * Cancels all pending tasks associated with a given event instance.
   *
   * @param eventId the event instance ID
   */
  void cancelAll(EventInstanceId eventId);

  /** Cancels all pending tasks globally across all event instances. */
  void cancelAll();
}
