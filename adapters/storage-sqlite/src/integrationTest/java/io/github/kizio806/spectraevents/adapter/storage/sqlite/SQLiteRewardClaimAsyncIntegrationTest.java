package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises durable asynchronous reward state transitions against a real SQLite database. */
class SQLiteRewardClaimAsyncIntegrationTest {
  @TempDir Path temporaryDirectory;

  @Test
  void persistsAndReconcilesAnAmbiguousDeliveryThroughAsynchronousOperations() throws Exception {
    Class.forName("org.sqlite.JDBC");
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            EventInstanceId.generate(), playerId, List.of(new RewardItem("minecraft:diamond", 3)));
    SQLiteEventInstanceRepository repository =
        new SQLiteEventInstanceRepository(temporaryDirectory.resolve("spectraevents.db"));
    repository.initialize();
    try {
      repository.savePendingDurablyAsync(claim).join();
      List<RewardClaim> pending = repository.findPendingAsync(playerId).join();
      assertEquals(List.of(claim.id()), pending.stream().map(RewardClaim::id).toList());
      assertEquals(claim.items(), pending.getFirst().items());

      assertTrue(repository.beginDeliveryAsync(claim.id()).join());
      assertEquals(
          List.of(claim.id()),
          repository.findByStatusAsync(RewardClaimStatus.DELIVERING).join().stream()
              .map(RewardClaim::id)
              .toList());

      assertTrue(repository.returnToPendingAsync(claim.id()).join());
      assertTrue(repository.beginDeliveryAsync(claim.id()).join());
      assertTrue(repository.markDeliveredAsync(claim.id()).join());
      assertFalse(repository.findPendingAsync(playerId).join().contains(claim));
    } finally {
      repository.shutdown();
    }
  }
}
