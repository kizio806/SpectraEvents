package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Platform-neutral background persistence executor. Takes persistence requests, adds them to a
 * bounded queue, and writes them cohesively.
 */
public class SingleWriterPersistenceExecutor {
  private static final Logger LOGGER =
      Logger.getLogger(SingleWriterPersistenceExecutor.class.getName());

  private final BlockingQueue<Runnable> queue;
  private final Thread workerThread;
  private final AtomicBoolean running = new AtomicBoolean(true);

  // Metrics
  private final AtomicLong peakQueueDepth = new AtomicLong(0);
  private final AtomicLong totalWrites = new AtomicLong(0);
  private final AtomicLong totalErrors = new AtomicLong(0);

  @FunctionalInterface
  private interface TrackedWrite extends Runnable {}

  private final class RawTrackedWrite implements TrackedWrite {
    private final Runnable delegate;

    RawTrackedWrite(Runnable delegate) {
      this.delegate = delegate;
    }

    @Override
    public void run() {
      try {
        delegate.run();
      } finally {
        totalWrites.incrementAndGet();
      }
    }
  }

  public SingleWriterPersistenceExecutor(int capacity) {
    this.queue = new ArrayBlockingQueue<>(capacity);
    this.workerThread = new Thread(this::runLoop, "SpectraEvents-Persistence-Writer");
    this.workerThread.setDaemon(false);
  }

  public void start() {
    workerThread.start();
  }

  public void enqueue(Runnable writeOperation) {
    if (!running.get()) {
      throw new IllegalStateException("Persistence writer is not accepting writes");
    }
    Runnable task =
        (writeOperation instanceof TrackedWrite)
            ? writeOperation
            : new RawTrackedWrite(writeOperation);
    if (!queue.offer(task)) {
      throw new RejectedExecutionException(
          "Persistence queue is full; refusing the write without blocking a platform thread");
    }
    long depth = queue.size();
    peakQueueDepth.accumulateAndGet(depth, Math::max);
  }

  /** Executes a write on the single writer thread and waits for its actual result. */
  public void executeAndWait(Runnable writeOperation) {
    try {
      executeAsync(writeOperation).join();
    } catch (CompletionException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Persistence write failed", cause);
    }
  }

  /** Queues a write and completes only after it has run on the single writer thread. */
  public CompletableFuture<Void> executeAsync(Runnable writeOperation) {
    if (Thread.currentThread().equals(workerThread)) {
      try {
        writeOperation.run();
        totalWrites.incrementAndGet();
        return CompletableFuture.completedFuture(null);
      } catch (Exception exception) {
        totalErrors.incrementAndGet();
        return CompletableFuture.failedFuture(exception);
      }
    }
    CompletableFuture<Void> completion = new CompletableFuture<>();
    enqueue(
        (TrackedWrite)
            () -> {
              try {
                writeOperation.run();
                totalWrites.incrementAndGet();
                completion.complete(null);
              } catch (Exception exception) {
                totalErrors.incrementAndGet();
                completion.completeExceptionally(exception);
              }
            });
    return completion;
  }

  /** Queues a result-producing write without blocking the caller. */
  public <T> CompletableFuture<T> supplyAsync(Callable<T> writeOperation) {
    if (Thread.currentThread().equals(workerThread)) {
      try {
        T result = writeOperation.call();
        totalWrites.incrementAndGet();
        return CompletableFuture.completedFuture(result);
      } catch (Exception exception) {
        totalErrors.incrementAndGet();
        return CompletableFuture.failedFuture(exception);
      }
    }
    CompletableFuture<T> completion = new CompletableFuture<>();
    enqueue(
        (TrackedWrite)
            () -> {
              try {
                T result = writeOperation.call();
                totalWrites.incrementAndGet();
                completion.complete(result);
              } catch (Exception exception) {
                totalErrors.incrementAndGet();
                completion.completeExceptionally(exception);
              }
            });
    return completion;
  }

  private void runLoop() {
    while (running.get() || !queue.isEmpty()) {
      try {
        Runnable op = queue.poll(100, TimeUnit.MILLISECONDS);
        if (op != null) {
          op.run();
        }
      } catch (InterruptedException e) {
        if (running.get()) {
          Thread.currentThread().interrupt();
          break;
        }
        // Shutdown interrupts the poll so the worker can drain the remaining queue immediately.
        // Keep the loop alive until all accepted writes have been processed.
      } catch (RuntimeException e) {
        totalErrors.incrementAndGet();
        LOGGER.log(Level.SEVERE, "Error in persistence writer thread", e);
      }
    }
    LOGGER.info("Persistence writer thread shut down cleanly.");
  }

  public void shutdown() {
    if (!running.compareAndSet(true, false)) {
      return;
    }
    workerThread.interrupt();
    try {
      workerThread.join(15000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while draining persistence writes", e);
    }
    if (Thread.currentThread().isInterrupted()) {
      throw new IllegalStateException("Interrupted while draining persistence writes");
    }
    if (workerThread.isAlive()) {
      throw new IllegalStateException(
          "Persistence writer did not drain within 15 seconds; connection remains open");
    }
  }

  public int getQueueDepth() {
    return queue.size();
  }

  public long getPeakQueueDepth() {
    return peakQueueDepth.get();
  }

  public long getTotalWrites() {
    return totalWrites.get();
  }

  public long getTotalErrors() {
    return totalErrors.get();
  }
}
