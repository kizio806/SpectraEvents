package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
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
    assertTrue(reader.returnToPending(claim.id()));
    assertEquals(1, reader.findPending(playerId).size());

    assertTrue(reader.beginDeliveryAsync(claim.id()).join());
    assertTrue(reader.returnToPendingAsync(claim.id()).join());
    assertTrue(reader.beginDelivery(claim.id()));
    assertTrue(reader.markDeliveredAsync(claim.id()).join());
    assertTrue(reader.findPending(playerId).isEmpty());
    reader.close();
  }

  @Test
  void synchronousMarkDeliveredTransitionsDeliveringToDelivered() {
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            EventInstanceId.generate(),
            playerId,
            List.of(new RewardItem("minecraft:gold_ingot", 5)));

    SQLiteEventInstanceRepository repo = new SQLiteEventInstanceRepository(dbPath);
    repo.initialize();

    repo.savePendingDurably(claim);
    assertTrue(repo.beginDelivery(claim.id()));
    assertTrue(repo.markDelivered(claim.id()));

    // Marking delivered again should return false (already delivered)
    assertFalse(repo.markDelivered(claim.id()));
    repo.close();
  }

  @Test
  void rejectsNonPendingClaimsDurably() {
    UUID playerId = UUID.randomUUID();
    RewardClaim nonPendingClaim =
        new RewardClaim(
            UUID.randomUUID(),
            EventInstanceId.generate(),
            playerId,
            List.of(new RewardItem("minecraft:iron_ingot", 1)),
            RewardClaimStatus.DELIVERED,
            Instant.now(),
            Instant.now());

    SQLiteEventInstanceRepository repo = new SQLiteEventInstanceRepository(dbPath);
    repo.initialize();

    assertThrows(IllegalArgumentException.class, () -> repo.savePendingDurably(nonPendingClaim));

    CompletionException ex =
        assertThrows(
            CompletionException.class, () -> repo.savePendingDurablyAsync(nonPendingClaim).join());
    assertTrue(ex.getCause() instanceof IllegalArgumentException);

    repo.close();
  }

  @Test
  void duplicatePendingClaimIsIgnoredCleanly() {
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            EventInstanceId.generate(),
            playerId,
            List.of(new RewardItem("minecraft:netherite_ingot", 1)));

    SQLiteEventInstanceRepository repo = new SQLiteEventInstanceRepository(dbPath);
    repo.initialize();

    repo.savePendingDurably(claim);
    // Persist again should be ignored without error
    repo.savePendingDurably(claim);

    List<RewardClaim> pending = repo.findPending(playerId);
    assertEquals(1, pending.size());

    repo.close();
  }

  @Test
  void asyncSavePendingDurablySuccess() {
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            EventInstanceId.generate(), playerId, List.of(new RewardItem("minecraft:apple", 10)));

    SQLiteEventInstanceRepository repo = new SQLiteEventInstanceRepository(dbPath);
    repo.initialize();

    repo.savePendingDurablyAsync(claim).join();
    List<RewardClaim> pending = repo.findPending(playerId);
    assertEquals(1, pending.size());
    assertEquals(claim.id(), pending.getFirst().id());

    repo.close();
  }

  @Test
  void testSaveAndReloadClaimWithDeliveredAt() {
    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            EventInstanceId.generate(), playerId, List.of(new RewardItem("minecraft:emerald", 2)));

    SQLiteEventInstanceRepository repo = new SQLiteEventInstanceRepository(dbPath);
    repo.initialize();

    repo.savePendingDurably(claim);
    assertTrue(repo.beginDelivery(claim.id()));
    assertTrue(repo.markDelivered(claim.id()));

    List<RewardClaim> delivered = repo.findByStatusAsync(RewardClaimStatus.DELIVERED).join();
    assertEquals(1, delivered.size());
    assertNotNull(delivered.getFirst().deliveredAt());
    assertEquals(claim.id(), delivered.getFirst().id());

    repo.close();
  }

  @Test
  void testClaimRejectsNonDeliveredWithDeliveredAt() {
    UUID playerId = UUID.randomUUID();
    // Claim constructed directly with PENDING status and non-null deliveredAt must fail domain
    // invariant
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RewardClaim(
                UUID.randomUUID(),
                EventInstanceId.generate(),
                playerId,
                List.of(new RewardItem("minecraft:iron_ingot", 4)),
                RewardClaimStatus.PENDING,
                Instant.now(),
                Instant.now()));
  }

  @Test
  void testNullArgumentValidations() {
    SQLiteEventInstanceRepository repo = new SQLiteEventInstanceRepository(dbPath);
    repo.initialize();

    assertThrows(NullPointerException.class, () -> repo.savePendingDurably(null));
    assertThrows(NullPointerException.class, () -> repo.savePendingDurablyAsync(null));
    assertThrows(NullPointerException.class, () -> repo.findPending(null));
    assertThrows(NullPointerException.class, () -> repo.findPendingAsync(null));
    assertThrows(NullPointerException.class, () -> repo.findByStatusAsync(null));
    assertThrows(NullPointerException.class, () -> repo.beginDelivery(null));
    assertThrows(NullPointerException.class, () -> repo.beginDeliveryAsync(null));
    assertThrows(NullPointerException.class, () -> repo.returnToPending(null));
    assertThrows(NullPointerException.class, () -> repo.returnToPendingAsync(null));
    assertThrows(NullPointerException.class, () -> repo.markDelivered(null));
    assertThrows(NullPointerException.class, () -> repo.markDeliveredAsync(null));

    repo.close();
  }

  @Test
  void testOperationsFailWithIllegalStateExceptionWhenTablesDropped() throws Exception {
    SQLiteEventInstanceRepository repo = new SQLiteEventInstanceRepository(dbPath);
    repo.initialize();

    UUID playerId = UUID.randomUUID();
    RewardClaim claim =
        RewardClaim.pending(
            EventInstanceId.generate(), playerId, List.of(new RewardItem("minecraft:diamond", 1)));

    // Drop tables to induce SQLExceptions
    try (java.sql.Connection conn = repo.databaseManager().getConnection();
        java.sql.Statement stmt = conn.createStatement()) {
      stmt.execute("DROP TABLE spectra_reward_items;");
      stmt.execute("DROP TABLE spectra_reward_claims;");
    }

    assertThrows(IllegalStateException.class, () -> repo.savePendingDurably(claim));
    assertThrows(IllegalStateException.class, () -> repo.findPending(playerId));
    assertThrows(IllegalStateException.class, () -> repo.beginDelivery(claim.id()));
    assertThrows(IllegalStateException.class, () -> repo.returnToPending(claim.id()));
    assertThrows(IllegalStateException.class, () -> repo.markDelivered(claim.id()));

    // Async equivalents also wrap failures in CompletionException -> IllegalStateException
    CompletionException ex1 =
        assertThrows(CompletionException.class, () -> repo.findPendingAsync(playerId).join());
    assertTrue(ex1.getCause() instanceof IllegalStateException);

    CompletionException ex3 =
        assertThrows(
            CompletionException.class,
            () -> repo.findByStatusAsync(RewardClaimStatus.PENDING).join());
    assertTrue(ex3.getCause() instanceof IllegalStateException);

    CompletionException ex4 =
        assertThrows(CompletionException.class, () -> repo.beginDeliveryAsync(claim.id()).join());
    assertTrue(ex4.getCause() instanceof IllegalStateException);

    CompletionException ex5 =
        assertThrows(CompletionException.class, () -> repo.returnToPendingAsync(claim.id()).join());
    assertTrue(ex5.getCause() instanceof IllegalStateException);

    CompletionException ex6 =
        assertThrows(CompletionException.class, () -> repo.markDeliveredAsync(claim.id()).join());
    assertTrue(ex6.getCause() instanceof IllegalStateException);

    repo.close();
  }
}
