package io.github.kizio806.spectraevents.application.service;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Coordinates one-at-a-time shared-loot reservations with durable runtime state. */
public final class SharedLootClaimService {
  private final EventInstanceRepository repository;
  private final ConcurrentHashMap<ReservationKey, UUID> activeReservations =
      new ConcurrentHashMap<>();

  public SharedLootClaimService(EventInstanceRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  /**
   * Atomically reserves a slot in memory and durably removes it before an external inventory
   * mutation. A failed persistence attempt restores the in-memory slot before propagating failure.
   */
  public CompletableFuture<Optional<Reservation>> reserve(EventRuntimeState state, int slot) {
    Objects.requireNonNull(state, "state");
    ReservationKey key = new ReservationKey(state.instanceId(), slot);
    UUID token = UUID.randomUUID();
    if (activeReservations.putIfAbsent(key, token) != null) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    Optional<CoreActions.LootStack> taken = state.takeSharedLootSlot(slot);
    if (taken.isEmpty()) {
      activeReservations.remove(key, token);
      return CompletableFuture.completedFuture(Optional.empty());
    }
    Reservation reservation = new Reservation(state, slot, taken.orElseThrow(), token);
    return repository
        .saveStateDurablyAsync(state)
        .handle(
            (ignored, failure) -> {
              if (failure != null) {
                restoreInMemory(reservation);
                release(reservation);
                throw new java.util.concurrent.CompletionException(failure);
              }
              return Optional.of(reservation);
            });
  }

  /**
   * Restores a rejected or partially delivered reservation and commits the new durable snapshot.
   */
  public CompletableFuture<Void> retain(
      Reservation reservation, CoreActions.LootStack undelivered) {
    Objects.requireNonNull(reservation, "reservation");
    Objects.requireNonNull(undelivered, "undelivered");
    requireActive(reservation);
    if (!reservation.state().restoreSharedLootSlot(reservation.slot(), undelivered)) {
      release(reservation);
      return CompletableFuture.failedFuture(
          new IllegalStateException(
              "Shared-loot slot was occupied while restoring reservation: " + reservation.slot()));
    }
    return repository
        .saveStateDurablyAsync(reservation.state())
        .whenComplete((ignored, failure) -> release(reservation));
  }

  public CompletableFuture<Void> retain(Reservation reservation) {
    return retain(reservation, reservation.loot());
  }

  /** Finishes a fully delivered reservation after its removal was already committed. */
  public void consume(Reservation reservation) {
    requireActive(Objects.requireNonNull(reservation, "reservation"));
    release(reservation);
  }

  private void requireActive(Reservation reservation) {
    ReservationKey key = new ReservationKey(reservation.state().instanceId(), reservation.slot());
    if (!reservation.token().equals(activeReservations.get(key))) {
      throw new IllegalStateException("Shared-loot reservation is no longer active");
    }
  }

  private void release(Reservation reservation) {
    activeReservations.remove(
        new ReservationKey(reservation.state().instanceId(), reservation.slot()),
        reservation.token());
  }

  private static void restoreInMemory(Reservation reservation) {
    if (!reservation.state().restoreSharedLootSlot(reservation.slot(), reservation.loot())) {
      throw new IllegalStateException(
          "Shared-loot slot was occupied after failed reservation persistence: "
              + reservation.slot());
    }
  }

  public record Reservation(
      EventRuntimeState state, int slot, CoreActions.LootStack loot, UUID token) {
    public Reservation {
      Objects.requireNonNull(state, "state");
      Objects.requireNonNull(loot, "loot");
      Objects.requireNonNull(token, "token");
      if (slot < 0) {
        throw new IllegalArgumentException("slot must not be negative");
      }
    }
  }

  private record ReservationKey(
      io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId instanceId, int slot) {}
}
