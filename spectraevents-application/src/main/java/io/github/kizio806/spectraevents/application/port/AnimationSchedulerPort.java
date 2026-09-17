package io.github.kizio806.spectraevents.application.port;

import java.time.Duration;

/** Platform port for scheduling delayed animation segment execution callbacks. */
@FunctionalInterface
public interface AnimationSchedulerPort {

  /**
   * Schedules a task to execute after the specified delay duration.
   *
   * @param delay time to wait before execution
   * @param task logic to execute
   */
  void schedule(Duration delay, Runnable task);
}
