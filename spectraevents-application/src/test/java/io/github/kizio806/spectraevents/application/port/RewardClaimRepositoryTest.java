package io.github.kizio806.spectraevents.application.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class RewardClaimRepositoryTest {

  @Test
  void testDefaultMethodsSuccess() {
    MinimalRewardClaimRepository repo = new MinimalRewardClaimRepository();
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()),
            playerId,
            List.of(new RewardItem("emerald", 3)));

    repo.savePendingDurablyAsync(claim).join();
    assertEquals(1, repo.findPendingAsync(playerId).join().size());
    assertTrue(repo.beginDeliveryAsync(claim.id()).join());
    assertTrue(repo.returnToPendingAsync(claim.id()).join());
    assertTrue(repo.markDeliveredAsync(claim.id()).join());
  }

  @Test
  void testDefaultFindByStatusThrowsUnsupported() {
    MinimalRewardClaimRepository repo = new MinimalRewardClaimRepository();
    ExecutionException ex =
        assertThrows(
            ExecutionException.class,
            () -> repo.findByStatusAsync(RewardClaimStatus.PENDING).get());
    assertTrue(ex.getCause() instanceof UnsupportedOperationException);
  }

  @Test
  void testDefaultMethodsCatchRuntimeException() {
    FailingRewardClaimRepository repo = new FailingRewardClaimRepository();
    UUID id = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            new EventInstanceId(UUID.randomUUID()), id, List.of(new RewardItem("iron", 1)));

    ExecutionException ex1 =
        assertThrows(ExecutionException.class, () -> repo.savePendingDurablyAsync(claim).get());
    assertTrue(ex1.getCause() instanceof IllegalStateException);

    ExecutionException ex2 =
        assertThrows(ExecutionException.class, () -> repo.findPendingAsync(id).get());
    assertTrue(ex2.getCause() instanceof IllegalStateException);

    ExecutionException ex3 =
        assertThrows(ExecutionException.class, () -> repo.beginDeliveryAsync(id).get());
    assertTrue(ex3.getCause() instanceof IllegalStateException);

    ExecutionException ex4 =
        assertThrows(ExecutionException.class, () -> repo.returnToPendingAsync(id).get());
    assertTrue(ex4.getCause() instanceof IllegalStateException);

    ExecutionException ex5 =
        assertThrows(ExecutionException.class, () -> repo.markDeliveredAsync(id).get());
    assertTrue(ex5.getCause() instanceof IllegalStateException);
  }

  private static final class MinimalRewardClaimRepository implements RewardClaimRepository {
    private final List<RewardClaim> claims = new ArrayList<>();

    @Override
    public void savePendingDurably(RewardClaim claim) {
      claims.add(claim);
    }

    @Override
    public List<RewardClaim> findPending(UUID playerId) {
      return claims.stream().filter(c -> c.playerId().equals(playerId)).toList();
    }

    @Override
    public boolean beginDelivery(UUID claimId) {
      return true;
    }

    @Override
    public boolean returnToPending(UUID claimId) {
      return true;
    }

    @Override
    public boolean markDelivered(UUID claimId) {
      return true;
    }
  }

  private static final class FailingRewardClaimRepository implements RewardClaimRepository {
    @Override
    public void savePendingDurably(RewardClaim claim) {
      throw new IllegalStateException("DB disk full");
    }

    @Override
    public List<RewardClaim> findPending(UUID playerId) {
      throw new IllegalStateException("DB timeout");
    }

    @Override
    public boolean beginDelivery(UUID claimId) {
      throw new IllegalStateException("DB locked");
    }

    @Override
    public boolean returnToPending(UUID claimId) {
      throw new IllegalStateException("DB connection error");
    }

    @Override
    public boolean markDelivered(UUID claimId) {
      throw new IllegalStateException("DB crash");
    }
  }
}
