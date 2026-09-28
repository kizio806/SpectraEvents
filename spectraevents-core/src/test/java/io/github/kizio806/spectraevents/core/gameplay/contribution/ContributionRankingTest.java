package io.github.kizio806.spectraevents.core.gameplay.contribution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContributionRankingTest {
  @Test
  void podiumFiltersFinaleEligibilityAndResolvesTiesDeterministically() {
    UUID early = UUID.fromString("00000000-0000-0000-0000-000000000003");
    UUID later = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID outsideZone = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID belowMinimum = UUID.fromString("00000000-0000-0000-0000-000000000004");
    DamageContribution contribution =
        DamageContribution.empty()
            .addDamage(early, 100)
            .addDamage(later, 100)
            .addDamage(outsideZone, 200)
            .addDamage(belowMinimum, 99);

    assertEquals(
        List.of(early, later),
        ContributionRanking.podium(
            contribution,
            List.of(early, later, belowMinimum),
            100,
            Map.of(early, 10L, later, 20L, outsideZone, 1L)));
  }
}
