package dev.spectraevents.core.gameplay.contribution;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Immutable primitive for tracking cumulative damage per participant. */
public record DamageContribution(Map<UUID, Integer> contributions) {

  public DamageContribution {
    Objects.requireNonNull(contributions, "contributions");
    contributions = Map.copyOf(contributions);
  }

  public static DamageContribution empty() {
    return new DamageContribution(Collections.emptyMap());
  }

  /**
   * Adds damage for a specific player and returns a new updated contribution state.
   *
   * @param player the UUID of the player
   * @param damage the amount of damage dealt
   * @return the new DamageContribution state
   */
  public DamageContribution addDamage(UUID player, int damage) {
    if (damage <= 0) {
      return this;
    }
    Objects.requireNonNull(player, "player");
    Map<UUID, Integer> newMap = new HashMap<>(contributions);
    newMap.merge(player, damage, Integer::sum);
    return new DamageContribution(newMap);
  }

  /**
   * Retrieves the top contributors sorted by damage descending.
   *
   * @param limit maximum number of entries to return
   * @return list of entries representing the top contributors
   */
  public List<Map.Entry<UUID, Integer>> getTopContributors(int limit) {
    if (limit <= 0) {
      return Collections.emptyList();
    }
    return contributions.entrySet().stream()
        .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
        .limit(limit)
        .collect(Collectors.toList());
  }
}
