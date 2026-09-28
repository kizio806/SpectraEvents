package io.github.kizio806.spectraevents.core.gameplay.reward;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A player-owned reward snapshot, created before any platform inventory delivery is attempted. */
public record RewardClaim(
    UUID id,
    EventInstanceId instanceId,
    UUID playerId,
    List<RewardItem> items,
    RewardClaimStatus status,
    Instant createdAt,
    Instant deliveredAt) {
  public RewardClaim {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(instanceId, "instanceId");
    Objects.requireNonNull(playerId, "playerId");
    items = List.copyOf(Objects.requireNonNull(items, "items"));
    if (items.isEmpty()) {
      throw new IllegalArgumentException("Reward claim must contain at least one item");
    }
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(createdAt, "createdAt");
    if (status == RewardClaimStatus.DELIVERED && deliveredAt == null) {
      throw new IllegalArgumentException("Delivered reward claims require deliveredAt");
    }
    if (status != RewardClaimStatus.DELIVERED && deliveredAt != null) {
      throw new IllegalArgumentException("Only delivered reward claims may have deliveredAt");
    }
  }

  public static RewardClaim pending(
      EventInstanceId instanceId, UUID playerId, List<RewardItem> items) {
    return new RewardClaim(
        UUID.randomUUID(),
        instanceId,
        playerId,
        items,
        RewardClaimStatus.PENDING,
        Instant.now(),
        null);
  }
}
