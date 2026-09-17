package io.github.kizio806.spectraevents.core.gameplay.health;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable primitive for tracking and evaluating health thresholds. Evaluates whether health
 * dropped below a certain threshold exactly once.
 */
public record HealthThresholds(List<Integer> thresholds) {
  public HealthThresholds {
    Objects.requireNonNull(thresholds, "thresholds");
    // Ensure immutable copy
    thresholds = List.copyOf(thresholds);
  }

  /**
   * Checks which thresholds were crossed exactly once between oldHealth and newHealth.
   *
   * @param oldHealth the previous health state
   * @param newHealth the new health state
   * @return a list of crossed threshold percentages, ordered from highest health threshold to
   *     lowest.
   */
  public List<Integer> checkCrossed(Health oldHealth, Health newHealth) {
    if (oldHealth == null || newHealth == null) {
      return Collections.emptyList();
    }

    // A threshold is crossed if old health was strictly greater than the threshold
    // and new health is less than or equal to the threshold.
    return thresholds.stream()
        .filter(threshold -> oldHealth.current() > threshold && newHealth.current() <= threshold)
        // Sort descending by threshold to trigger highest thresholds first
        .sorted((t1, t2) -> Integer.compare(t2, t1))
        .collect(Collectors.toList());
  }
}
