package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Durable boundary for the player reward mailbox. */
public interface RewardClaimRepository {
  /** Persists a newly-created pending claim before inventory delivery may begin. */
  void savePendingDurably(RewardClaim claim);

  /** Persists a pending claim without blocking a platform-owned thread. */
  default CompletableFuture<Void> savePendingDurablyAsync(RewardClaim claim) {
    try {
      savePendingDurably(claim);
      return CompletableFuture.completedFuture(null);
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }

  /** Returns claims which are safe to show to a player as ready for delivery. */
  List<RewardClaim> findPending(UUID playerId);

  /** Loads player-visible mailbox claims without blocking a platform-owned thread. */
  default CompletableFuture<List<RewardClaim>> findPendingAsync(UUID playerId) {
    try {
      return CompletableFuture.completedFuture(findPending(playerId));
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }

  /** Loads claims in a durable state for explicit operator reconciliation. */
  default CompletableFuture<List<RewardClaim>> findByStatusAsync(RewardClaimStatus status) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException(
            "Reward reconciliation is unavailable for this repository"));
  }

  /** Atomically reserves a pending claim for one delivery attempt. */
  boolean beginDelivery(UUID claimId);

  /** Atomically reserves a pending claim without blocking a platform-owned thread. */
  default CompletableFuture<Boolean> beginDeliveryAsync(UUID claimId) {
    try {
      return CompletableFuture.completedFuture(beginDelivery(claimId));
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }

  /** Returns a failed delivery attempt to the pending mailbox. */
  boolean returnToPending(UUID claimId);

  /** Returns a failed delivery attempt to the mailbox without blocking a platform thread. */
  default CompletableFuture<Boolean> returnToPendingAsync(UUID claimId) {
    try {
      return CompletableFuture.completedFuture(returnToPending(claimId));
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }

  /** Records a completed delivery. A delivered claim cannot be claimed again. */
  boolean markDelivered(UUID claimId);

  /** Marks delivery complete without blocking a platform-owned thread. */
  default CompletableFuture<Boolean> markDeliveredAsync(UUID claimId) {
    try {
      return CompletableFuture.completedFuture(markDelivered(claimId));
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }
}
