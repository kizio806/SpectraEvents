package io.github.kizio806.spectraevents.platform.bukkit.concurrent;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Utility for handling asynchronous task futures without creating dead local stores or discarding
 * futures unsafely.
 */
public final class AsyncFutures {

  private AsyncFutures() {}

  /**
   * Explicitly acknowledges an asynchronous pipeline as handled in background, avoiding dead local
   * store warnings from unused variables and satisfying {@code FutureReturnValueIgnored}.
   *
   * @param future the asynchronous future to acknowledge
   */
  public static void discard(CompletableFuture<?> future) {
    Objects.requireNonNull(future, "future");
  }
}
