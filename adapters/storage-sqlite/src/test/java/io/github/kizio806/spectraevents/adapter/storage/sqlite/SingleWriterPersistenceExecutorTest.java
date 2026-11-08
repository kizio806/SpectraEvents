package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SingleWriterPersistenceExecutorTest {

  private SingleWriterPersistenceExecutor executor;

  @BeforeEach
  void setUp() {
    executor = new SingleWriterPersistenceExecutor(10);
    executor.start();
  }

  @AfterEach
  void tearDown() {
    if (executor != null) {
      executor.shutdown();
    }
  }

  @Test
  void testExecuteAndWaitSuccess() {
    AtomicInteger count = new AtomicInteger(0);
    executor.executeAndWait(count::incrementAndGet);
    assertEquals(1, count.get());
    assertEquals(1, executor.getTotalWrites());
  }

  @Test
  void testExecuteAndWaitFailureThrowsException() {
    assertThrows(
        RuntimeException.class,
        () ->
            executor.executeAndWait(
                () -> {
                  throw new IllegalArgumentException("test error");
                }));
  }

  @Test
  void testSupplyAsyncSuccess() {
    String result = executor.supplyAsync(() -> "hello").join();
    assertEquals("hello", result);
  }

  @Test
  void testSupplyAsyncFailure() {
    CompletionException ex =
        assertThrows(
            CompletionException.class,
            () ->
                executor
                    .supplyAsync(
                        () -> {
                          throw new IllegalStateException("failed");
                        })
                    .join());
    assertTrue(ex.getCause() instanceof IllegalStateException);
  }

  @Test
  void testQueueMetricsAndRejection() {
    SingleWriterPersistenceExecutor tinyExecutor = new SingleWriterPersistenceExecutor(1);
    // Don't start it so queue fills up
    tinyExecutor.enqueue(() -> {});
    assertThrows(RejectedExecutionException.class, () -> tinyExecutor.enqueue(() -> {}));
    assertTrue(tinyExecutor.getPeakQueueDepth() >= 1);
    assertEquals(1, tinyExecutor.getQueueDepth());
    tinyExecutor.shutdown();
  }

  @Test
  void testShutdownRejectsFurtherWrites() {
    executor.shutdown();
    assertThrows(IllegalStateException.class, () -> executor.enqueue(() -> {}));
  }

  @Test
  void testMetricsAccessors() {
    assertEquals(0, executor.getQueueDepth());
    assertEquals(0L, executor.getPeakQueueDepth());
    assertEquals(0L, executor.getTotalWrites());
    assertEquals(0L, executor.getTotalErrors());
  }

  @Test
  void testConcurrentPeakQueueDepthTracking() throws Exception {
    int threadCount = 8;
    int writesPerThread = 25;
    int totalWrites = threadCount * writesPerThread;
    SingleWriterPersistenceExecutor concurrentExecutor =
        new SingleWriterPersistenceExecutor(totalWrites + 50);

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(threadCount);
    AtomicInteger executedWrites = new AtomicInteger(0);

    List<Thread> threads = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      Thread thread =
          new Thread(
              () -> {
                try {
                  startLatch.await();
                  for (int i = 0; i < writesPerThread; i++) {
                    concurrentExecutor.enqueue(executedWrites::incrementAndGet);
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                } finally {
                  completionLatch.countDown();
                }
              });
      threads.add(thread);
      thread.start();
    }

    startLatch.countDown();
    completionLatch.await();

    // Start worker only after bursts are queued to guarantee high queue depth
    concurrentExecutor.start();

    for (Thread thread : threads) {
      thread.join();
    }

    concurrentExecutor.shutdown();

    assertEquals(totalWrites, executedWrites.get());
    assertEquals(totalWrites, concurrentExecutor.getTotalWrites());
    assertTrue(
        concurrentExecutor.getPeakQueueDepth() >= threadCount,
        "Peak queue depth should reflect concurrent bursts, was: "
            + concurrentExecutor.getPeakQueueDepth());
  }

  @Test
  void testReentrantExecutionOnWorkerThread() {
    AtomicInteger reentrantRuns = new AtomicInteger(0);

    executor.executeAndWait(
        () -> {
          // Inside worker thread, call executeAsync directly
          executor.executeAsync(reentrantRuns::incrementAndGet).join();

          // Inside worker thread, call supplyAsync directly
          String result = executor.supplyAsync(() -> "worker-result").join();
          assertEquals("worker-result", result);

          // Inside worker thread, failing executeAsync
          CompletionException ex1 =
              assertThrows(
                  CompletionException.class,
                  () ->
                      executor
                          .executeAsync(
                              () -> {
                                throw new IllegalStateException("reentrant-fail");
                              })
                          .join());
          assertTrue(ex1.getCause() instanceof IllegalStateException);

          // Inside worker thread, failing supplyAsync
          CompletionException ex2 =
              assertThrows(
                  CompletionException.class,
                  () ->
                      executor
                          .supplyAsync(
                              () -> {
                                throw new IllegalStateException("reentrant-supply-fail");
                              })
                          .join());
          assertTrue(ex2.getCause() instanceof IllegalStateException);
        });

    assertEquals(1, reentrantRuns.get());
  }

  @Test
  void testMultipleShutdownCallsAreIdempotent() {
    executor.shutdown();
    executor.shutdown();
    assertEquals(0, executor.getQueueDepth());
  }

  @Test
  void testExecuteAndWaitWithNonRuntimeExceptionThrowsIllegalStateException() {
    assertThrows(
        IllegalStateException.class,
        () ->
            executor.executeAndWait(
                () -> {
                  throw new CompletionException(new Exception("not a runtime exception"));
                }));
  }

  @Test
  void testRawRunnableExceptionInRunLoopIncrementsErrors() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    executor.enqueue(
        () -> {
          try {
            throw new RuntimeException("raw uncaught exception");
          } finally {
            latch.countDown();
          }
        });
    latch.await();
    // Worker loop caught it, logged it, and incremented error counter
    // Wait briefly or execute a write to verify the worker is still alive
    executor.executeAndWait(() -> {});
    assertTrue(executor.getTotalErrors() >= 1);
  }

  @Test
  void testWorkerSelfInterruptWhileRunningBreaksLoop() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    executor.enqueue(
        () -> {
          Thread.currentThread().interrupt();
          latch.countDown();
        });
    latch.await();
    // Worker breaks out of loop after encountering interrupted poll
    // Give it a moment to terminate
    Thread.sleep(150);
    // Enqueueing further writes will now either sit in queue or after shutdown drain
    executor.shutdown();
  }

  @Test
  void testShutdownThrowsWhenCallerThreadIsInterrupted() {
    Thread.currentThread().interrupt();
    try {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> executor.shutdown());
      assertTrue(ex.getMessage().contains("Interrupted while draining persistence writes"));
    } finally {
      Thread.interrupted(); // Clear interrupted status for JUnit thread
    }
  }
}
