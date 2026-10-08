package io.github.kizio806.spectraevents.application.schedule;

import java.util.List;
import java.util.Objects;

/** Result of loading the independent schedules file without discarding valid sibling entries. */
public record ScheduleLoadResult(List<EventSchedule> schedules, List<Failure> failures) {
  public ScheduleLoadResult {
    schedules = List.copyOf(Objects.requireNonNull(schedules, "schedules"));
    failures = List.copyOf(Objects.requireNonNull(failures, "failures"));
  }

  /** One invalid YAML path, always qualified with its source file. */
  public record Failure(String path, String message) {
    public Failure {
      if (Objects.requireNonNull(path, "path").isBlank()) {
        throw new IllegalArgumentException("path cannot be blank");
      }
      if (Objects.requireNonNull(message, "message").isBlank()) {
        throw new IllegalArgumentException("message cannot be blank");
      }
    }
  }
}
