package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Platform boundary for an all-or-nothing player inventory delivery attempt. */
@FunctionalInterface
public interface RewardDeliveryPort {
  /**
   * Delivers the complete snapshot, or returns {@code false} without changing the mailbox claim.
   *
   * <p>The platform adapter must return {@code false} for an offline player, an inventory that
   * cannot contain the entire snapshot, or an unresolved item.
   */
  CompletableFuture<Boolean> deliver(UUID playerId, List<RewardItem> items);
}
