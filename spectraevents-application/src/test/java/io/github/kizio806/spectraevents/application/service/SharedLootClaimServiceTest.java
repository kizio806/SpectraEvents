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
