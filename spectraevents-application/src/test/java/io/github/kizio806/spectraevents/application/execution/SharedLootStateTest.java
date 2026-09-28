package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class SharedLootStateTest {
  @Test
  void aSlotCanBeTakenOnlyOnceUnderContention() throws Exception {
    EventRuntimeState state = new EventRuntimeState(EventInstanceId.generate());
    state.initializeSharedLoot(List.of(new CoreActions.LootStack("minecraft:diamond", 1)));
    try (var executor = Executors.newFixedThreadPool(16)) {
      java.util.List<java.util.concurrent.Callable<Boolean>> calls =
          java.util.Collections.nCopies(16, () -> state.takeSharedLootSlot(0).isPresent());
      var results = executor.invokeAll(calls);
      assertEquals(
          1,
          results.stream()
              .filter(
                  result -> {
                    try {
                      return result.get();
                    } catch (Exception exception) {
                      throw new AssertionError(exception);
                    }
                  })
              .count());
    }
    assertTrue(state.sharedLootEmpty());
  }
}
