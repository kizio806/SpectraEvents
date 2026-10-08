package io.github.kizio806.spectraevents.application.service;

import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.application.port.RewardDeliveryPort;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Coordinates durable player reward claims with platform inventory delivery. */
public final class RewardMailboxService {
  private final RewardClaimRepository repository;

  public RewardMailboxService(RewardClaimRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public CompletableFuture<List<RewardClaim>> pendingClaims(UUID playerId) {
    return repository.findPendingAsync(Objects.requireNonNull(playerId, "playerId"));
  }

  /** Lists claims left ambiguous by a process failure for an explicit operator decision. */
  public CompletableFuture<List<RewardClaim>> deliveringClaims() {
    return repository.findByStatusAsync(RewardClaimStatus.DELIVERING);
  }

  /**
   * Resolves an ambiguous delivery only after an operator has established whether the external
   * inventory mutation happened. This method never guesses and never reissues automatically.
   */
  public CompletableFuture<Boolean> reconcile(UUID claimId, ReconciliationDecision decision) {
    Objects.requireNonNull(claimId, "claimId");
    return switch (Objects.requireNonNull(decision, "decision")) {
      case MARK_DELIVERED -> repository.markDeliveredAsync(claimId);
      case RETURN_TO_PENDING -> repository.returnToPendingAsync(claimId);
    };
  }

  /**
   * Attempts to deliver one claim owned by the given player.
   *
   * <p>The claim is made unavailable to competing requests before the platform operation starts. A
   * rejected or failed platform operation returns it to the mailbox. A process failure between
   * inventory insertion and durable acknowledgement intentionally leaves the claim in {@code
   * DELIVERING}; it is never silently reissued and must be reconciled by an operator.
   */
  public CompletableFuture<ClaimDeliveryResult> claim(
      UUID playerId, UUID claimId, RewardDeliveryPort deliveryPort) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(claimId, "claimId");
    Objects.requireNonNull(deliveryPort, "deliveryPort");

    return pendingClaims(playerId)
        .thenCompose(
            claims -> {
              RewardClaim claim =
                  claims.stream()
                      .filter(candidate -> candidate.id().equals(claimId))
                      .findFirst()
                      .orElse(null);
              if (claim == null) {
                return CompletableFuture.completedFuture(ClaimDeliveryResult.notAvailable());
              }
              return repository
                  .beginDeliveryAsync(claim.id())
                  .thenCompose(
                      reserved -> {
                        if (!reserved) {
                          return CompletableFuture.completedFuture(ClaimDeliveryResult.busy());
                        }
                        return deliverReservedClaim(playerId, claim, deliveryPort);
                      });
            });
  }

  private CompletableFuture<ClaimDeliveryResult> deliverReservedClaim(
      UUID playerId, RewardClaim claim, RewardDeliveryPort deliveryPort) {
    try {
      return deliveryPort
          .deliver(playerId, claim.items())
          .handle((delivered, failure) -> failure == null && Boolean.TRUE.equals(delivered))
          .thenCompose(
              delivered -> {
                if (!delivered) {
                  return repository
                      .returnToPendingAsync(claim.id())
                      .thenApply(
                          restored -> {
                            if (!restored) {
                              throw new IllegalStateException(
                                  "Reward claim could not be returned to pending: " + claim.id());
                            }
                            return ClaimDeliveryResult.retained();
                          });
                }
                return repository
                    .markDeliveredAsync(claim.id())
                    .thenApply(
                        acknowledged -> {
                          if (!acknowledged) {
                            throw new IllegalStateException(
                                "Reward claim acknowledgement was lost: " + claim.id());
                          }
                          return ClaimDeliveryResult.delivered();
                        });
              });
    } catch (RuntimeException exception) {
      return repository
          .returnToPendingAsync(claim.id())
          .thenApply(
              restored -> {
                if (!restored) {
                  throw new IllegalStateException(
                      "Reward claim could not be returned to pending: " + claim.id());
                }
                return ClaimDeliveryResult.retained();
              });
    }
  }

  /** Result that commands can render without interpreting persistence state. */
  public record ClaimDeliveryResult(Status status) {
    public ClaimDeliveryResult {
      Objects.requireNonNull(status, "status");
    }

    public static ClaimDeliveryResult delivered() {
      return new ClaimDeliveryResult(Status.DELIVERED);
    }

    public static ClaimDeliveryResult retained() {
      return new ClaimDeliveryResult(Status.RETAINED);
    }

    public static ClaimDeliveryResult notAvailable() {
      return new ClaimDeliveryResult(Status.NOT_AVAILABLE);
    }

    public static ClaimDeliveryResult busy() {
      return new ClaimDeliveryResult(Status.BUSY);
    }

    public enum Status {
      DELIVERED,
      RETAINED,
      NOT_AVAILABLE,
      BUSY
    }
  }

  /** Explicitly records the result of investigating a crash-ambiguous inventory operation. */
  public enum ReconciliationDecision {
    MARK_DELIVERED,
    RETURN_TO_PENDING
  }
}
