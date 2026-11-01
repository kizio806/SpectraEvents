package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Dedicated SQLite implementation of RewardClaimRepository. */
public final class SQLiteRewardClaimRepository implements RewardClaimRepository {
  private final SQLiteDatabaseManager database;

  public SQLiteRewardClaimRepository(SQLiteDatabaseManager database) {
    this.database = Objects.requireNonNull(database, "database");
  }

  @Override
  public void savePendingDurably(RewardClaim claim) {
    Objects.requireNonNull(claim, "claim");
    if (claim.status() != RewardClaimStatus.PENDING) {
      throw new IllegalArgumentException("Only pending reward claims may be created");
    }
    database.executor().executeAndWait(() -> persistPendingClaim(claim));
  }

  @Override
  public CompletableFuture<Void> savePendingDurablyAsync(RewardClaim claim) {
    Objects.requireNonNull(claim, "claim");
    if (claim.status() != RewardClaimStatus.PENDING) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Only pending reward claims may be created"));
    }
    return database.executor().executeAsync(() -> persistPendingClaim(claim));
  }

  private void persistPendingClaim(RewardClaim claim) {
    String claimSql =
        "INSERT OR IGNORE INTO spectra_reward_claims (claim_id, instance_id, player_uuid, status, created_at, delivered_at) VALUES (?, ?, ?, ?, ?, ?)";
    String itemSql =
        "INSERT INTO spectra_reward_items (claim_id, slot, material, amount) VALUES (?, ?, ?, ?)";
    Connection writerConnection = database.writerConnection();
    try (PreparedStatement claimStatement = writerConnection.prepareStatement(claimSql);
        PreparedStatement itemStatement = writerConnection.prepareStatement(itemSql)) {
      claimStatement.setString(1, claim.id().toString());
      claimStatement.setString(2, claim.instanceId().toString());
      claimStatement.setString(3, claim.playerId().toString());
      claimStatement.setString(4, claim.status().name());
      claimStatement.setLong(5, claim.createdAt().toEpochMilli());
      setNullableLong(
          claimStatement,
          6,
          claim.deliveredAt() == null ? null : claim.deliveredAt().toEpochMilli());
      if (claimStatement.executeUpdate() == 0) {
        database.commitOrThrow();
        return;
      }
      int slot = 0;
      for (RewardItem item : claim.items()) {
        itemStatement.setString(1, claim.id().toString());
        itemStatement.setInt(2, slot++);
        itemStatement.setString(3, item.material());
        itemStatement.setInt(4, item.amount());
        itemStatement.addBatch();
      }
      itemStatement.executeBatch();
      database.commitOrThrow();
    } catch (SQLException exception) {
      database.rollbackQuietly();
      throw new IllegalStateException("Failed to persist reward claim", exception);
    }
  }

  @Override
  public List<RewardClaim> findPending(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return readRewardClaims(
        "SELECT claim_id, instance_id, player_uuid, status, created_at, delivered_at FROM spectra_reward_claims WHERE player_uuid = ? AND status = ? ORDER BY created_at",
        statement -> {
          statement.setString(1, playerId.toString());
          statement.setString(2, RewardClaimStatus.PENDING.name());
        });
  }

  @Override
  public CompletableFuture<List<RewardClaim>> findPendingAsync(UUID playerId) {
    UUID nonNullPlayerId = Objects.requireNonNull(playerId, "playerId");
    return CompletableFuture.supplyAsync(() -> findPending(nonNullPlayerId));
  }

  @Override
  public CompletableFuture<List<RewardClaim>> findByStatusAsync(RewardClaimStatus status) {
    RewardClaimStatus nonNullStatus = Objects.requireNonNull(status, "status");
    return CompletableFuture.supplyAsync(
        () ->
            readRewardClaims(
                "SELECT claim_id, instance_id, player_uuid, status, created_at, delivered_at FROM spectra_reward_claims WHERE status = ? ORDER BY created_at",
                statement -> statement.setString(1, nonNullStatus.name())));
  }

  private List<RewardClaim> readRewardClaims(String sql, SqlStatementBinder binder) {
    List<RewardClaim> claims = new ArrayList<>();
    try (Connection connection = database.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      binder.bind(statement);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          claims.add(readRewardClaim(connection, resultSet));
        }
      }
      return List.copyOf(claims);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to load pending reward claims", exception);
    }
  }

  @Override
  public boolean beginDelivery(UUID claimId) {
    return transitionRewardClaim(
        Objects.requireNonNull(claimId, "claimId"),
        RewardClaimStatus.PENDING,
        RewardClaimStatus.DELIVERING);
  }

  @Override
  public CompletableFuture<Boolean> beginDeliveryAsync(UUID claimId) {
    UUID nonNullClaimId = Objects.requireNonNull(claimId, "claimId");
    return transitionRewardClaimAsync(
        nonNullClaimId, RewardClaimStatus.PENDING, RewardClaimStatus.DELIVERING);
  }

  @Override
  public boolean returnToPending(UUID claimId) {
    return transitionRewardClaim(
        Objects.requireNonNull(claimId, "claimId"),
        RewardClaimStatus.DELIVERING,
        RewardClaimStatus.PENDING);
  }

  @Override
  public CompletableFuture<Boolean> returnToPendingAsync(UUID claimId) {
    UUID nonNullClaimId = Objects.requireNonNull(claimId, "claimId");
    return transitionRewardClaimAsync(
        nonNullClaimId, RewardClaimStatus.DELIVERING, RewardClaimStatus.PENDING);
  }

  @Override
  public boolean markDelivered(UUID claimId) {
    UUID nonNullClaimId = Objects.requireNonNull(claimId, "claimId");
    AtomicBoolean updated = new AtomicBoolean();
    database
        .executor()
        .executeAndWait(
            () -> {
              String sql =
                  "UPDATE spectra_reward_claims SET status = ?, delivered_at = ? WHERE claim_id = ? AND status = ?";
              try (PreparedStatement statement =
                  database.writerConnection().prepareStatement(sql)) {
                statement.setString(1, RewardClaimStatus.DELIVERED.name());
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, nonNullClaimId.toString());
                statement.setString(4, RewardClaimStatus.DELIVERING.name());
                updated.set(statement.executeUpdate() == 1);
                database.commitOrThrow();
              } catch (SQLException exception) {
                database.rollbackQuietly();
                throw new IllegalStateException("Failed to mark reward claim delivered", exception);
              }
            });
    return updated.get();
  }

  @Override
  public CompletableFuture<Boolean> markDeliveredAsync(UUID claimId) {
    UUID nonNullClaimId = Objects.requireNonNull(claimId, "claimId");
    return database
        .executor()
        .supplyAsync(
            () -> {
              String sql =
                  "UPDATE spectra_reward_claims SET status = ?, delivered_at = ? WHERE claim_id = ? AND status = ?";
              try (PreparedStatement statement =
                  database.writerConnection().prepareStatement(sql)) {
                statement.setString(1, RewardClaimStatus.DELIVERED.name());
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, nonNullClaimId.toString());
                statement.setString(4, RewardClaimStatus.DELIVERING.name());
                boolean updated = statement.executeUpdate() == 1;
                database.commitOrThrow();
                return updated;
              } catch (SQLException exception) {
                database.rollbackQuietly();
                throw new IllegalStateException("Failed to mark reward claim delivered", exception);
              }
            });
  }

  private boolean transitionRewardClaim(
      UUID claimId, RewardClaimStatus expected, RewardClaimStatus next) {
    AtomicBoolean updated = new AtomicBoolean();
    database
        .executor()
        .executeAndWait(
            () -> {
              String sql =
                  "UPDATE spectra_reward_claims SET status = ? WHERE claim_id = ? AND status = ?";
              try (PreparedStatement statement =
                  database.writerConnection().prepareStatement(sql)) {
                statement.setString(1, next.name());
                statement.setString(2, claimId.toString());
                statement.setString(3, expected.name());
                updated.set(statement.executeUpdate() == 1);
                database.commitOrThrow();
              } catch (SQLException exception) {
                database.rollbackQuietly();
                throw new IllegalStateException("Failed to transition reward claim", exception);
              }
            });
    return updated.get();
  }

  private CompletableFuture<Boolean> transitionRewardClaimAsync(
      UUID claimId, RewardClaimStatus expected, RewardClaimStatus next) {
    return database
        .executor()
        .supplyAsync(
            () -> {
              String sql =
                  "UPDATE spectra_reward_claims SET status = ? WHERE claim_id = ? AND status = ?";
              try (PreparedStatement statement =
                  database.writerConnection().prepareStatement(sql)) {
                statement.setString(1, next.name());
                statement.setString(2, claimId.toString());
                statement.setString(3, expected.name());
                boolean updated = statement.executeUpdate() == 1;
                database.commitOrThrow();
                return updated;
              } catch (SQLException exception) {
                database.rollbackQuietly();
                throw new IllegalStateException("Failed to transition reward claim", exception);
              }
            });
  }

  private RewardClaim readRewardClaim(Connection connection, ResultSet resultSet)
      throws SQLException {
    UUID claimId = UUID.fromString(resultSet.getString("claim_id"));
    List<RewardItem> items = new ArrayList<>();
    try (PreparedStatement itemStatement =
        connection.prepareStatement(
            "SELECT material, amount FROM spectra_reward_items WHERE claim_id = ? ORDER BY slot")) {
      itemStatement.setString(1, claimId.toString());
      try (ResultSet itemResults = itemStatement.executeQuery()) {
        while (itemResults.next()) {
          items.add(
              new RewardItem(itemResults.getString("material"), itemResults.getInt("amount")));
        }
      }
    }
    long deliveredAt = resultSet.getLong("delivered_at");
    boolean deliveredAtMissing = resultSet.wasNull();
    return new RewardClaim(
        claimId,
        new EventInstanceId(UUID.fromString(resultSet.getString("instance_id"))),
        UUID.fromString(resultSet.getString("player_uuid")),
        items,
        RewardClaimStatus.valueOf(resultSet.getString("status")),
        Instant.ofEpochMilli(resultSet.getLong("created_at")),
        deliveredAtMissing ? null : Instant.ofEpochMilli(deliveredAt));
  }

  private void setNullableLong(PreparedStatement statement, int index, Long value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, java.sql.Types.INTEGER);
    } else {
      statement.setLong(index, value);
    }
  }

  @FunctionalInterface
  private interface SqlStatementBinder {
    void bind(PreparedStatement statement) throws SQLException;
  }
}
