package io.github.kizio806.spectraevents.core.gameplay.reward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RewardClaimTest {

  @Test
  void testRewardItem() {
    RewardItem item = new RewardItem("minecraft:netherite_ingot", 3);
    assertEquals("minecraft:netherite_ingot", item.material());
    assertEquals(3, item.amount());

    assertThrows(IllegalArgumentException.class, () -> new RewardItem(null, 1));
    assertThrows(IllegalArgumentException.class, () -> new RewardItem("   ", 1));
    assertThrows(IllegalArgumentException.class, () -> new RewardItem("stone", 0));
    assertThrows(IllegalArgumentException.class, () -> new RewardItem("stone", -5));
  }

  @Test
  void testRewardClaimPending() {
    EventInstanceId instanceId = EventInstanceId.generate();
    UUID playerId = UUID.randomUUID();
    RewardItem item = new RewardItem("minecraft:diamond", 5);

    RewardClaim claim = RewardClaim.pending(instanceId, playerId, List.of(item));
    assertNotNull(claim.id());
    assertEquals(instanceId, claim.instanceId());
    assertEquals(playerId, claim.playerId());
    assertEquals(1, claim.items().size());
    assertEquals(RewardClaimStatus.PENDING, claim.status());
    assertNotNull(claim.createdAt());
    assertNull(claim.deliveredAt());
  }

  @Test
  void testRewardClaimValidation() {
    EventInstanceId instanceId = EventInstanceId.generate();
    UUID playerId = UUID.randomUUID();
    UUID claimId = UUID.randomUUID();
    RewardItem item = new RewardItem("minecraft:diamond", 1);
    Instant now = Instant.now();

    // DELIVERED status requires deliveredAt
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RewardClaim(
                claimId,
                instanceId,
                playerId,
                List.of(item),
                RewardClaimStatus.DELIVERED,
                now,
                null));

    // Non-DELIVERED status cannot have deliveredAt
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RewardClaim(
                claimId, instanceId, playerId, List.of(item), RewardClaimStatus.PENDING, now, now));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RewardClaim(
                claimId,
                instanceId,
                playerId,
                List.of(item),
                RewardClaimStatus.DELIVERING,
                now,
                now));

    // Valid DELIVERED claim
    RewardClaim delivered =
        new RewardClaim(
            claimId, instanceId, playerId, List.of(item), RewardClaimStatus.DELIVERED, now, now);
    assertEquals(RewardClaimStatus.DELIVERED, delivered.status());
    assertEquals(now, delivered.deliveredAt());

    // Empty items
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RewardClaim(
                claimId, instanceId, playerId, List.of(), RewardClaimStatus.PENDING, now, null));

    // Null checks
    assertThrows(
        NullPointerException.class,
        () ->
            new RewardClaim(
                null, instanceId, playerId, List.of(item), RewardClaimStatus.PENDING, now, null));
    assertThrows(
        NullPointerException.class,
        () ->
            new RewardClaim(
                claimId, null, playerId, List.of(item), RewardClaimStatus.PENDING, now, null));
    assertThrows(
        NullPointerException.class,
        () ->
            new RewardClaim(
                claimId, instanceId, null, List.of(item), RewardClaimStatus.PENDING, now, null));
    assertThrows(
        NullPointerException.class,
        () ->
            new RewardClaim(
                claimId, instanceId, playerId, null, RewardClaimStatus.PENDING, now, null));
    assertThrows(
        NullPointerException.class,
        () -> new RewardClaim(claimId, instanceId, playerId, List.of(item), null, now, null));
    assertThrows(
        NullPointerException.class,
        () ->
            new RewardClaim(
                claimId,
                instanceId,
                playerId,
                List.of(item),
                RewardClaimStatus.PENDING,
                null,
                null));
  }
}
