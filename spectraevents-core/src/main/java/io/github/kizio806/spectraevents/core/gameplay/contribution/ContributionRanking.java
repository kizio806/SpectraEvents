package io.github.kizio806.spectraevents.core.gameplay.contribution;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Deterministic podium ranking for an encounter's damage contributors. */
public final class ContributionRanking {
  private ContributionRanking() {}

  /**
   * Ranks only players currently eligible for the finale. Equal damage is resolved by the earliest
   * attainment of the configured minimum contribution, then by UUID for stable cross-restart order.
   */
  public static List<UUID> podium(
      DamageContribution contribution,
      Collection<UUID> eligibleAtFinale,
      int minimumContribution,
      Map<UUID, Long> thresholdAttainedMillis) {
    Objects.requireNonNull(contribution, "contribution");
    Objects.requireNonNull(eligibleAtFinale, "eligibleAtFinale");
    Objects.requireNonNull(thresholdAttainedMillis, "thresholdAttainedMillis");
    if (minimumContribution < 0) {
      throw new IllegalArgumentException("minimumContribution must not be negative");
    }
    return eligibleAtFinale.stream()
        .filter(
            player -> contribution.contributions().getOrDefault(player, 0) >= minimumContribution)
        .sorted(
            Comparator.<UUID>comparingInt(
                    player -> contribution.contributions().getOrDefault(player, 0))
                .reversed()
                .thenComparingLong(
                    player -> thresholdAttainedMillis.getOrDefault(player, Long.MAX_VALUE))
                .thenComparing(UUID::toString))
        .limit(3)
        .toList();
  }
}
