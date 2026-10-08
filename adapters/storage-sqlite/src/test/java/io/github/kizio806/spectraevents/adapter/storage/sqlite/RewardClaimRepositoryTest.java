package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RewardClaimRepositoryTest {
  private Path dbPath;

  @BeforeEach
  void setUp() throws Exception {
    Class.forName("org.sqlite.JDBC");
    dbPath = Files.createTempFile("reward_claims", ".db");
  }

  @AfterEach
  void tearDown() throws Exception {
    Files.deleteIfExists(dbPath);
  }

  @Test
  void persistsPendingClaimAndAllowsExactlyOneDeliveryReservationAcrossRestart() {
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            EventInstanceId.generate(),
            playerId,
            List.of(
                new RewardItem("minecraft:diamond", 3), new RewardItem("minecraft:emerald", 8)));

    SQLiteEventInstanceRepository writer = new SQLiteEventInstanceRepository(dbPath);
    writer.initialize();
    writer.savePendingDurably(claim);
    writer.shutdown();

    SQLiteEventInstanceRepository reader = new SQLiteEventInstanceRepository(dbPath);
    reader.initialize();
    List<RewardClaim> pending = reader.findPending(playerId);
    assertEquals(1, pending.size());
    assertEquals(claim.items(), pending.getFirst().items());
    assertTrue(reader.beginDelivery(claim.id()));
    assertFalse(reader.beginDelivery(claim.id()));
    assertTrue(reader.markDelivered(claim.id()));
    assertTrue(reader.findPending(playerId).isEmpty());
    reader.shutdown();
  }
}
