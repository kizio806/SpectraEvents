package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import java.util.List;
import java.util.UUID;

/** Durable boundary for the player reward mailbox. */
public interface RewardClaimRepository {
  /** Persists a newly-created pending claim before inventory delivery may begin. */
  void savePendingDurably(RewardClaim claim);

  /** Returns claims which are safe to show to a player as ready for delivery. */
  List<RewardClaim> findPending(UUID playerId);

  /** Atomically reserves a pending claim for one delivery attempt. */
  boolean beginDelivery(UUID claimId);

  /** Returns a failed delivery attempt to the pending mailbox. */
  void returnToPending(UUID claimId);

  /** Records a completed delivery. A delivered claim cannot be claimed again. */
  boolean markDelivered(UUID claimId);
}
