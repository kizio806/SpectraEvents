package io.github.kizio806.spectraevents.application.repository;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** In-memory event-instance repository for the runtime's initial application composition. */
public final class InMemoryEventInstanceRepository
    implements EventInstanceRepository, RewardClaimRepository {
  private final ConcurrentMap<EventInstanceId, EventInstance> eventInstances =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<EventInstanceId, EventRuntimeState> eventStates =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<UUID, RewardClaim> rewardClaims = new ConcurrentHashMap<>();

  @Override
  public void save(EventInstance eventInstance) {
    EventInstance nonNullEventInstance = Objects.requireNonNull(eventInstance, "eventInstance");
    eventInstances.put(nonNullEventInstance.id(), nonNullEventInstance);
  }

  @Override
  public Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
    return Optional.ofNullable(
        eventInstances.get(Objects.requireNonNull(eventInstanceId, "eventInstanceId")));
  }

  @Override
  public List<EventInstance> findAll() {
    return List.copyOf(eventInstances.values());
  }

  @Override
  public boolean remove(EventInstanceId eventInstanceId) {
    eventStates.remove(Objects.requireNonNull(eventInstanceId, "eventInstanceId"));
    return eventInstances.remove(eventInstanceId) != null;
  }

  @Override
  public void saveState(EventRuntimeState state) {
    eventStates.put(Objects.requireNonNull(state.instanceId(), "instanceId"), state);
  }

  @Override
  public Optional<EventRuntimeState> findState(EventInstanceId eventInstanceId) {
    return Optional.ofNullable(
        eventStates.get(Objects.requireNonNull(eventInstanceId, "eventInstanceId")));
  }

  @Override
  public void savePendingDurably(RewardClaim claim) {
    RewardClaim nonNullClaim = Objects.requireNonNull(claim, "claim");
    if (nonNullClaim.status() != RewardClaimStatus.PENDING) {
      throw new IllegalArgumentException("Only pending reward claims may be created");
    }
    rewardClaims.putIfAbsent(nonNullClaim.id(), nonNullClaim);
  }

  @Override
  public List<RewardClaim> findPending(UUID playerId) {
    return rewardClaims.values().stream()
        .filter(claim -> claim.playerId().equals(Objects.requireNonNull(playerId, "playerId")))
        .filter(claim -> claim.status() == RewardClaimStatus.PENDING)
        .sorted(Comparator.comparing(RewardClaim::createdAt))
        .toList();
  }

  @Override
  public CompletableFuture<List<RewardClaim>> findByStatusAsync(RewardClaimStatus status) {
    Objects.requireNonNull(status, "status");
    return CompletableFuture.completedFuture(
        rewardClaims.values().stream()
            .filter(claim -> claim.status() == status)
            .sorted(Comparator.comparing(RewardClaim::createdAt))
            .toList());
  }

  @Override
  public boolean beginDelivery(UUID claimId) {
    AtomicBoolean claimed = new AtomicBoolean();
    rewardClaims.computeIfPresent(
        Objects.requireNonNull(claimId, "claimId"),
        (ignored, claim) -> {
          if (claim.status() != RewardClaimStatus.PENDING) {
            return claim;
          }
          claimed.set(true);
          return new RewardClaim(
              claim.id(),
              claim.instanceId(),
              claim.playerId(),
              claim.items(),
              RewardClaimStatus.DELIVERING,
              claim.createdAt(),
              null);
        });
    return claimed.get();
  }

  @Override
  public boolean returnToPending(UUID claimId) {
    AtomicBoolean returned = new AtomicBoolean();
    rewardClaims.computeIfPresent(
        Objects.requireNonNull(claimId, "claimId"),
        (ignored, claim) ->
            claim.status() == RewardClaimStatus.DELIVERING ? pendingClaim(claim, returned) : claim);
    return returned.get();
  }

  private static RewardClaim pendingClaim(RewardClaim claim, AtomicBoolean returned) {
    returned.set(true);
    return new RewardClaim(
        claim.id(),
        claim.instanceId(),
        claim.playerId(),
        claim.items(),
        RewardClaimStatus.PENDING,
        claim.createdAt(),
        null);
  }

  @Override
  public boolean markDelivered(UUID claimId) {
    AtomicBoolean delivered = new AtomicBoolean();
    rewardClaims.computeIfPresent(
        Objects.requireNonNull(claimId, "claimId"),
        (ignored, claim) -> {
          if (claim.status() != RewardClaimStatus.DELIVERING) {
            return claim;
          }
          delivered.set(true);
          return new RewardClaim(
              claim.id(),
              claim.instanceId(),
              claim.playerId(),
              claim.items(),
              RewardClaimStatus.DELIVERED,
              claim.createdAt(),
              java.time.Instant.now());
        });
    return delivered.get();
  }
}
