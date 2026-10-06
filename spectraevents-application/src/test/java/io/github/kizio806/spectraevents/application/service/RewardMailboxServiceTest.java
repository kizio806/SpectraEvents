package io.github.kizio806.spectraevents.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class RewardMailboxServiceTest {
  @Test
  void keepsTheClaimWhenPlatformCannotDeliverTheCompleteSnapshot() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RewardMailboxService service = new RewardMailboxService(repository);
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()),
            playerId,
            List.of(new RewardItem("diamond", 3)));
    repository.savePendingDurably(claim);

    RewardMailboxService.ClaimDeliveryResult result =
        service
            .claim(
                playerId, claim.id(), (ignored, items) -> CompletableFuture.completedFuture(false))
            .join();

    assertEquals(RewardMailboxService.ClaimDeliveryResult.Status.RETAINED, result.status());
    assertEquals(List.of(claim), service.pendingClaims(playerId).join());
  }

  @Test
  void acknowledgesClaimOnlyAfterThePlatformConfirmsDelivery() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RewardMailboxService service = new RewardMailboxService(repository);
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()),
            playerId,
            List.of(new RewardItem("emerald", 1)));
    repository.savePendingDurably(claim);

    RewardMailboxService.ClaimDeliveryResult result =
        service
            .claim(
                playerId, claim.id(), (ignored, items) -> CompletableFuture.completedFuture(true))
            .join();

    assertEquals(RewardMailboxService.ClaimDeliveryResult.Status.DELIVERED, result.status());
    assertEquals(List.of(), service.pendingClaims(playerId).join());
  }

  @Test
  void exposesAndResolvesCrashAmbiguousDeliveriesOnlyAfterAnExplicitDecision() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RewardMailboxService service = new RewardMailboxService(repository);
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()), playerId, List.of(new RewardItem("gold", 2)));
    repository.savePendingDurably(claim);
    assertEquals(true, repository.beginDelivery(claim.id()));

    assertEquals(
        List.of(claim.id()),
        service.deliveringClaims().join().stream().map(RewardClaim::id).toList());
    assertEquals(
        true,
        service
            .reconcile(claim.id(), RewardMailboxService.ReconciliationDecision.RETURN_TO_PENDING)
            .join());
    assertEquals(
        List.of(claim.id()),
        service.pendingClaims(playerId).join().stream().map(RewardClaim::id).toList());
  }
}
