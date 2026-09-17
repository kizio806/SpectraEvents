package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
  private final java.util.concurrent.atomic.AtomicLong peakQueueDepth =
      new java.util.concurrent.atomic.AtomicLong(0);
  private final java.util.concurrent.atomic.AtomicLong totalWrites =
      new java.util.concurrent.atomic.AtomicLong(0);
  private final java.util.concurrent.atomic.AtomicLong totalErrors =
      new java.util.concurrent.atomic.AtomicLong(0);

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
      return;
    }
    if (!queue.offer(writeOperation)) {
      LOGGER.warning("Persistence queue full! Blocking until space is available.");
      try {
        queue.put(writeOperation); // Backpressure
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    long depth = queue.size();
    if (depth > peakQueueDepth.get()) {
      peakQueueDepth.set(depth);
    }
  }

  private void runLoop() {
    while (running.get() || !queue.isEmpty()) {
      try {
        Runnable op = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (op != null) {
          op.run();
          totalWrites.incrementAndGet();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        totalErrors.incrementAndGet();
        LOGGER.log(Level.SEVERE, "Error in persistence writer thread", e);
      }
    }
    LOGGER.info("Persistence writer thread shut down cleanly.");
  }

  public void shutdown() {
    running.set(false);
    try {
      workerThread.join(10000); // wait max 10 seconds for flush
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
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
