package io.github.kizio806.spectraevents.application.service;

import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.application.port.RewardDeliveryPort;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
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

  public List<RewardClaim> pendingClaims(UUID playerId) {
    return repository.findPending(Objects.requireNonNull(playerId, "playerId"));
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

    RewardClaim claim =
        pendingClaims(playerId).stream()
            .filter(candidate -> candidate.id().equals(claimId))
            .findFirst()
            .orElse(null);
    if (claim == null) {
      return CompletableFuture.completedFuture(ClaimDeliveryResult.notAvailable());
    }
    if (!repository.beginDelivery(claim.id())) {
      return CompletableFuture.completedFuture(ClaimDeliveryResult.busy());
    }

    try {
      return deliveryPort
          .deliver(playerId, claim.items())
          .handle(
              (delivered, failure) -> {
                if (failure != null || !Boolean.TRUE.equals(delivered)) {
                  repository.returnToPending(claim.id());
                  return ClaimDeliveryResult.retained();
                }
                if (!repository.markDelivered(claim.id())) {
                  throw new IllegalStateException(
                      "Reward claim acknowledgement was lost: " + claim.id());
                }
                return ClaimDeliveryResult.delivered();
              });
    } catch (RuntimeException exception) {
      repository.returnToPending(claim.id());
      return CompletableFuture.completedFuture(ClaimDeliveryResult.retained());
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
}
