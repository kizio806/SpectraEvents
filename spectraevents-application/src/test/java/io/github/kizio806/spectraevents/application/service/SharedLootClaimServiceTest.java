package io.github.kizio806.spectraevents.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class SharedLootClaimServiceTest {
  @Test
  void permitsOnlyOneDurableReservationUnderContention() throws Exception {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    SharedLootClaimService service = new SharedLootClaimService(repository);
    EventRuntimeState state = stateWithOneDiamond();

    try (var executor = Executors.newFixedThreadPool(16)) {
      var calls =
          java.util.Collections
              .<java.util.concurrent.Callable<Optional<SharedLootClaimService.Reservation>>>nCopies(
                  16, () -> service.reserve(state, 0).join());
      var results = executor.invokeAll(calls);

      assertEquals(
          1,
          results.stream()
              .map(
                  result -> {
                    try {
                      return result.get();
                    } catch (Exception exception) {
                      throw new AssertionError(exception);
                    }
                  })
              .filter(Optional::isPresent)
              .count());
    }
    assertTrue(state.sharedLootEmpty());
  }

  @Test
  void restoresAnUndeliveredRemainderDurably() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    SharedLootClaimService service = new SharedLootClaimService(repository);
    EventRuntimeState state = stateWithOneDiamond();
    SharedLootClaimService.Reservation reservation = service.reserve(state, 0).join().orElseThrow();

    service.retain(reservation, new CoreActions.LootStack("minecraft:diamond", 2)).join();

    assertEquals(
        new CoreActions.LootStack("minecraft:diamond", 2),
        repository.findState(state.instanceId()).orElseThrow().sharedLootSnapshot().get(0));
  }

  @Test
  void restoresMemoryWhenDurableReservationFails() {
    EventRuntimeState state = stateWithOneDiamond();
    SharedLootClaimService service = new SharedLootClaimService(new FailingStateRepository());

    assertThrows(CompletionException.class, () -> service.reserve(state, 0).join());

    assertEquals(1, state.sharedLootSnapshot().size());
    assertEquals(1, state.sharedLootSnapshot().get(0).amount());
  }

  @Test
  void testReserveEmptySlotReturnsEmpty() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    SharedLootClaimService service = new SharedLootClaimService(repository);
    EventRuntimeState state = stateWithOneDiamond();

    Optional<SharedLootClaimService.Reservation> res = service.reserve(state, 99).join();
    assertTrue(res.isEmpty());
  }

  @Test
  void testConsumeReservationAndInactiveRejection() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    SharedLootClaimService service = new SharedLootClaimService(repository);
    EventRuntimeState state = stateWithOneDiamond();
    SharedLootClaimService.Reservation reservation = service.reserve(state, 0).join().orElseThrow();

    service.consume(reservation);

    // Consuming or retaining again throws IllegalStateException
    assertThrows(IllegalStateException.class, () -> service.consume(reservation));
    assertThrows(IllegalStateException.class, () -> service.retain(reservation));
  }

  @Test
  void testRetainOverloadRestoresOriginalLoot() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    SharedLootClaimService service = new SharedLootClaimService(repository);
    EventRuntimeState state = stateWithOneDiamond();
    SharedLootClaimService.Reservation reservation = service.reserve(state, 0).join().orElseThrow();

    service.retain(reservation).join();

    assertEquals(
        new CoreActions.LootStack("minecraft:diamond", 1),
        repository.findState(state.instanceId()).orElseThrow().sharedLootSnapshot().get(0));
  }

  @Test
  void testRetainFailsWhenSlotAlreadyOccupied() {
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    SharedLootClaimService service = new SharedLootClaimService(repository);
    EventRuntimeState state = stateWithOneDiamond();
    SharedLootClaimService.Reservation reservation = service.reserve(state, 0).join().orElseThrow();

    // Re-occupy slot 0 directly in state
    state.restoreSharedLootSlot(0, new CoreActions.LootStack("minecraft:gold_ingot", 1));

    CompletableFuture<Void> future =
        service.retain(reservation, new CoreActions.LootStack("minecraft:diamond", 1));
    java.util.concurrent.ExecutionException ex =
        assertThrows(java.util.concurrent.ExecutionException.class, future::get);
    assertTrue(ex.getCause() instanceof IllegalStateException);
    assertTrue(ex.getCause().getMessage().contains("was occupied while restoring reservation"));
  }

  @Test
  void testReservationValidationAndFailedPersistenceSlotOccupied() {
    EventRuntimeState state = stateWithOneDiamond();
    CoreActions.LootStack loot = new CoreActions.LootStack("minecraft:diamond", 1);
    UUID token = UUID.randomUUID();

    assertThrows(
        IllegalArgumentException.class,
        () -> new SharedLootClaimService.Reservation(state, -1, loot, token));

    // When persistence fails and the slot is already occupied, restoreInMemory throws
    // IllegalStateException
    EventInstanceRepository maliciousRepo =
        new EventInstanceRepository() {
          @Override
          public void save(EventInstance eventInstance) {}

          @Override
          public Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
            return Optional.empty();
          }

          @Override
          public List<EventInstance> findAll() {
            return List.of();
          }

          @Override
          public boolean remove(EventInstanceId eventInstanceId) {
            return false;
          }

          @Override
          public CompletableFuture<Void> saveStateDurablyAsync(EventRuntimeState s) {
            s.restoreSharedLootSlot(0, new CoreActions.LootStack("minecraft:emerald", 10));
            return CompletableFuture.failedFuture(new RuntimeException("disk write error"));
          }
        };

    SharedLootClaimService maliciousService = new SharedLootClaimService(maliciousRepo);
    CompletionException ex =
        assertThrows(CompletionException.class, () -> maliciousService.reserve(state, 0).join());
    assertTrue(ex.getCause() instanceof IllegalStateException);
    assertTrue(
        ex.getCause()
            .getMessage()
            .contains("slot was occupied after failed reservation persistence"));
  }

  private static EventRuntimeState stateWithOneDiamond() {
    EventRuntimeState state = new EventRuntimeState(EventInstanceId.generate());
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 1)));
    return state;
  }

  private static final class FailingStateRepository implements EventInstanceRepository {
    @Override
    public void save(EventInstance eventInstance) {}

    @Override
    public Optional<EventInstance> findById(EventInstanceId eventInstanceId) {
      return Optional.empty();
    }

    @Override
    public List<EventInstance> findAll() {
      return List.of();
    }

    @Override
    public boolean remove(EventInstanceId eventInstanceId) {
      return false;
    }

    @Override
    public CompletableFuture<Void> saveStateDurablyAsync(EventRuntimeState state) {
      return CompletableFuture.failedFuture(new IllegalStateException("disk unavailable"));
    }
  }
}
