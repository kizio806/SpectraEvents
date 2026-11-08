package io.github.kizio806.spectraevents.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

  @Test
  void testReconcileMarkDelivered() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RewardMailboxService service = new RewardMailboxService(repository);
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()), playerId, List.of(new RewardItem("gold", 2)));
    repository.savePendingDurably(claim);
    repository.beginDelivery(claim.id());

    assertTrue(
        service
            .reconcile(claim.id(), RewardMailboxService.ReconciliationDecision.MARK_DELIVERED)
            .join());
    assertEquals(List.of(), service.pendingClaims(playerId).join());
    assertEquals(List.of(), service.deliveringClaims().join());
  }

  @Test
  void testClaimNotAvailableWhenClaimMissing() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RewardMailboxService service = new RewardMailboxService(repository);
    UUID playerId = UUID.randomUUID();
    UUID nonExistentClaimId = UUID.randomUUID();

    RewardMailboxService.ClaimDeliveryResult result =
        service
            .claim(
                playerId,
                nonExistentClaimId,
                (ignored, items) -> CompletableFuture.completedFuture(true))
            .join();

    assertEquals(RewardMailboxService.ClaimDeliveryResult.Status.NOT_AVAILABLE, result.status());
  }

  @Test
  void testClaimBusyWhenBeginDeliveryFails() {
    InMemoryEventInstanceRepository backingRepo = new InMemoryEventInstanceRepository();
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()), playerId, List.of(new RewardItem("iron", 5)));
    backingRepo.savePendingDurably(claim);

    // Decorator that simulates beginDelivery returning false
    RewardClaimRepository busyRepo =
        new DelegatingClaimRepository(backingRepo) {
          @Override
          public CompletableFuture<Boolean> beginDeliveryAsync(UUID claimId) {
            return CompletableFuture.completedFuture(false);
          }
        };

    RewardMailboxService service = new RewardMailboxService(busyRepo);
    RewardMailboxService.ClaimDeliveryResult result =
        service
            .claim(
                playerId, claim.id(), (ignored, items) -> CompletableFuture.completedFuture(true))
            .join();

    assertEquals(RewardMailboxService.ClaimDeliveryResult.Status.BUSY, result.status());
  }

  @Test
  void testClaimDeliveryFutureFailureRetainsClaim() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RewardMailboxService service = new RewardMailboxService(repository);
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()), playerId, List.of(new RewardItem("iron", 5)));
    repository.savePendingDurably(claim);

    RewardMailboxService.ClaimDeliveryResult result =
        service
            .claim(
                playerId,
                claim.id(),
                (ignored, items) ->
                    CompletableFuture.failedFuture(new RuntimeException("Delivery network error")))
            .join();

    assertEquals(RewardMailboxService.ClaimDeliveryResult.Status.RETAINED, result.status());
    assertEquals(List.of(claim), service.pendingClaims(playerId).join());
  }

  @Test
  void testClaimDeliveryFailureWhenReturnToPendingFailsThrowsIllegalState() {
    InMemoryEventInstanceRepository backingRepo = new InMemoryEventInstanceRepository();
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()), playerId, List.of(new RewardItem("iron", 5)));
    backingRepo.savePendingDurably(claim);

    RewardClaimRepository failingReturnRepo =
        new DelegatingClaimRepository(backingRepo) {
          @Override
          public CompletableFuture<Boolean> returnToPendingAsync(UUID claimId) {
            return CompletableFuture.completedFuture(false);
          }
        };

    RewardMailboxService service = new RewardMailboxService(failingReturnRepo);
    CompletableFuture<RewardMailboxService.ClaimDeliveryResult> future =
        service.claim(
            playerId, claim.id(), (ignored, items) -> CompletableFuture.completedFuture(false));

    ExecutionException ex = assertThrows(ExecutionException.class, future::get);
    assertTrue(ex.getCause() instanceof IllegalStateException);
    assertTrue(ex.getCause().getMessage().contains("could not be returned to pending"));
  }

  @Test
  void testClaimDeliverySuccessWhenMarkDeliveredFailsThrowsIllegalState() {
    InMemoryEventInstanceRepository backingRepo = new InMemoryEventInstanceRepository();
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()), playerId, List.of(new RewardItem("iron", 5)));
    backingRepo.savePendingDurably(claim);

    RewardClaimRepository failingMarkRepo =
        new DelegatingClaimRepository(backingRepo) {
          @Override
          public CompletableFuture<Boolean> markDeliveredAsync(UUID claimId) {
            return CompletableFuture.completedFuture(false);
          }
        };

    RewardMailboxService service = new RewardMailboxService(failingMarkRepo);
    CompletableFuture<RewardMailboxService.ClaimDeliveryResult> future =
        service.claim(
            playerId, claim.id(), (ignored, items) -> CompletableFuture.completedFuture(true));

    ExecutionException ex = assertThrows(ExecutionException.class, future::get);
    assertTrue(ex.getCause() instanceof IllegalStateException);
    assertTrue(ex.getCause().getMessage().contains("acknowledgement was lost"));
  }

  @Test
  void testClaimSyncExceptionInDeliveryPortRetainsClaim() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    RewardMailboxService service = new RewardMailboxService(repository);
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()), playerId, List.of(new RewardItem("iron", 5)));
    repository.savePendingDurably(claim);

    RewardMailboxService.ClaimDeliveryResult result =
        service
            .claim(
                playerId,
                claim.id(),
                (ignored, items) -> {
                  throw new IllegalArgumentException("Invalid player parameters");
                })
            .join();

    assertEquals(RewardMailboxService.ClaimDeliveryResult.Status.RETAINED, result.status());
    assertEquals(List.of(claim), service.pendingClaims(playerId).join());
  }

  @Test
  void testClaimSyncExceptionWhenReturnToPendingFailsThrowsIllegalState() {
    InMemoryEventInstanceRepository backingRepo = new InMemoryEventInstanceRepository();
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()), playerId, List.of(new RewardItem("iron", 5)));
    backingRepo.savePendingDurably(claim);

    RewardClaimRepository failingReturnRepo =
        new DelegatingClaimRepository(backingRepo) {
          @Override
          public CompletableFuture<Boolean> returnToPendingAsync(UUID claimId) {
            return CompletableFuture.completedFuture(false);
          }
        };

    RewardMailboxService service = new RewardMailboxService(failingReturnRepo);
    CompletableFuture<RewardMailboxService.ClaimDeliveryResult> future =
        service.claim(
            playerId,
            claim.id(),
            (ignored, items) -> {
              throw new IllegalStateException("Immediate sync error");
            });

    ExecutionException ex = assertThrows(ExecutionException.class, future::get);
    assertTrue(ex.getCause() instanceof IllegalStateException);
    assertTrue(ex.getCause().getMessage().contains("could not be returned to pending"));
  }

  private static class DelegatingClaimRepository implements RewardClaimRepository {
    private final InMemoryEventInstanceRepository delegate;

    DelegatingClaimRepository(InMemoryEventInstanceRepository delegate) {
      this.delegate = delegate;
    }

    @Override
    public void savePendingDurably(RewardClaim claim) {
      delegate.savePendingDurably(claim);
    }

    @Override
    public CompletableFuture<Void> savePendingDurablyAsync(RewardClaim claim) {
      return delegate.savePendingDurablyAsync(claim);
    }

    @Override
    public List<RewardClaim> findPending(UUID playerId) {
      return delegate.findPending(playerId);
    }

    @Override
    public CompletableFuture<List<RewardClaim>> findPendingAsync(UUID playerId) {
      return delegate.findPendingAsync(playerId);
    }

    @Override
    public CompletableFuture<List<RewardClaim>> findByStatusAsync(RewardClaimStatus status) {
      return delegate.findByStatusAsync(status);
    }

    @Override
    public boolean beginDelivery(UUID claimId) {
      return delegate.beginDelivery(claimId);
    }

    @Override
    public CompletableFuture<Boolean> beginDeliveryAsync(UUID claimId) {
      return delegate.beginDeliveryAsync(claimId);
    }

    @Override
    public boolean returnToPending(UUID claimId) {
      return delegate.returnToPending(claimId);
    }

    @Override
    public CompletableFuture<Boolean> returnToPendingAsync(UUID claimId) {
      return delegate.returnToPendingAsync(claimId);
    }

    @Override
    public boolean markDelivered(UUID claimId) {
      return delegate.markDelivered(claimId);
    }

    @Override
    public CompletableFuture<Boolean> markDeliveredAsync(UUID claimId) {
      return delegate.markDeliveredAsync(claimId);
    }
  }
}
