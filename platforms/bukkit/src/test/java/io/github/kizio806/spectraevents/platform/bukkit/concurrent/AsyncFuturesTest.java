package io.github.kizio806.spectraevents.platform.bukkit.concurrent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class AsyncFuturesTest {

  @Test
  void shouldDiscardNonNullFutureWithoutException() {
    CompletableFuture<String> future = CompletableFuture.completedFuture("test");
    assertDoesNotThrow(() -> AsyncFutures.discard(future));
  }

  @Test
  void shouldRejectNullFuture() {
    assertThrows(NullPointerException.class, () -> AsyncFutures.discard(null));
  }
}
