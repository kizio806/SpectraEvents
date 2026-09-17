package io.github.kizio806.spectraevents.adapter.storage.sqlite;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Benchmark for SQLite persistence. Run this via gradle to measure before/after performance:
 * ./gradlew :adapters:storage-sqlite:test --tests
 * "io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteConcurrencyBenchmarkTest"
 * -Dbenchmark=true
 */
public class SQLiteConcurrencyBenchmarkTest {
  private Path dbPath;
  private SQLiteEventInstanceRepository repository;

  @BeforeEach
  void setUp() throws Exception {
    Class.forName("org.sqlite.JDBC");
    dbPath = Files.createTempFile("benchmark_test", ".db");
    repository = new SQLiteEventInstanceRepository(dbPath);
    repository.initialize();
  }

  @AfterEach
  void tearDown() throws Exception {
    Files.deleteIfExists(dbPath);
  }

  @Test
  // @EnabledIfSystemProperty
  void benchmarkCurrentImplementation() throws Exception {
    runBenchmarkScenario(1);
    runBenchmarkScenario(5);
    runBenchmarkScenario(20);
    runBenchmarkScenario(50);
  }

  private void runBenchmarkScenario(int threads) throws Exception {
    System.out.println("--- BENCHMARK SCENARIO: " + threads + " concurrent writers ---");

    ExecutorService executor = Executors.newFixedThreadPool(threads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger busyCount = new AtomicInteger(0);
    AtomicInteger otherErrorCount = new AtomicInteger(0);
    List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

    EventDefinitionId defId = new EventDefinitionId("test_def");
    PhaseId phaseId = new PhaseId("phase1");

    int totalOperations = 1000;

    long startTime = System.currentTimeMillis();

    List<Callable<Void>> tasks = new ArrayList<>();
    for (int i = 0; i < totalOperations; i++) {
      tasks.add(
          () -> {
            long startOp = System.nanoTime();
            try {
              EventInstance instance =
                  EventInstance.reconstitute(
                      EventInstanceId.generate(), defId, EventLifecycleState.RUNNING, phaseId);
              repository.save(instance);
              successCount.incrementAndGet();
              latencies.add((System.nanoTime() - startOp) / 1_000_000); // ms
            } catch (Exception e) {
              if (e.getMessage() != null && e.getMessage().contains("SQLITE_BUSY")) {
                busyCount.incrementAndGet();
              } else {
                otherErrorCount.incrementAndGet();
                e.printStackTrace();
              }
            }
            return null;
          });
    }

    List<Future<Void>> futures = executor.invokeAll(tasks);
    for (Future<Void> f : futures) {
      try {
        f.get();
      } catch (Exception ignored) {
      }
    }
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    long endTime = System.currentTimeMillis();
    long durationMs = endTime - startTime;
    double opsPerSec = (successCount.get() * 1000.0) / durationMs;

    List<Long> sortedLatencies = new ArrayList<>(latencies);
    Collections.sort(sortedLatencies);

    long p50 =
        sortedLatencies.isEmpty() ? 0 : sortedLatencies.get((int) (sortedLatencies.size() * 0.50));
    long p95 =
        sortedLatencies.isEmpty() ? 0 : sortedLatencies.get((int) (sortedLatencies.size() * 0.95));
    long p99 =
        sortedLatencies.isEmpty() ? 0 : sortedLatencies.get((int) (sortedLatencies.size() * 0.99));
    long max = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get(sortedLatencies.size() - 1);

    System.out.printf(
        "Threads: %d | Ops: %d | Duration: %d ms | Ops/s: %.2f%n",
        threads, successCount.get(), durationMs, opsPerSec);
    System.out.printf("Latencies (ms): p50=%d, p95=%d, p99=%d, max=%d%n", p50, p95, p99, max);
    System.out.printf(
        "Errors: SQLITE_BUSY=%d, Other=%d%n%n", busyCount.get(), otherErrorCount.get());
  }
}
