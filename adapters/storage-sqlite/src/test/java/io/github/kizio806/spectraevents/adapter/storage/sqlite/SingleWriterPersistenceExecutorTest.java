package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletionException;
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
    assertNotNull(executor.getTotalWrites());
    assertNotNull(executor.getTotalErrors());
  }
}
