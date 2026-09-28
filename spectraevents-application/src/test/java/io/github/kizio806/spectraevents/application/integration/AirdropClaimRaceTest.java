package io.github.kizio806.spectraevents.application.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AirdropClaimRaceTest {
  private InMemoryEventInstanceRepository repository;
  private EventDefinitionRegistry registry;
  private FakeEventTaskScheduler scheduler;
  private EventRuntimeStateStore stateStore;
  private EventExecutionEngine engine;
  private AtomicInteger giveItemCount;

  @BeforeEach
  void setUp() throws Exception {
    repository = new InMemoryEventInstanceRepository();
    registry = new EventDefinitionRegistry();
    scheduler = new FakeEventTaskScheduler();
    stateStore = new EventRuntimeStateStore();
    giveItemCount = new AtomicInteger(0);

    PlatformActionPort actionPort =
        (instance, state, action) -> {
          if (action.type().equalsIgnoreCase("give_item")) {
            giveItemCount.incrementAndGet();
          }
          return java.util.concurrent.CompletableFuture.completedFuture(true);
        };

    engine = new EventExecutionEngine(repository, registry, scheduler, actionPort, stateStore);

    // Read events/airdrop.yml
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("events/airdrop.yml");
    String yamlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    EventSpecYamlParser parser = new EventSpecYamlParser();
    EventDefinitionCompiler compiler = new EventDefinitionCompiler();
    EventDefinition airdropDefinition = compiler.compile(parser.parse(yamlContent, "airdrop.yml"));
    registry.register(airdropDefinition, "airdrop.yml");
  }

  @Test
  void testConcurrentClaimRace() throws Exception {
    EventInstance instance =
        engine.startEvent(
            "airdrop",
            new io.github.kizio806.spectraevents.application.execution.EventLocation(
                "world", 0, 64, 0, 0, 0));

    // Fast-forward through falling -> locked -> open
    engine.evaluateTrigger(
        instance.id(),
        new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
            .TimerElapsedTrigger(java.time.Duration.ofMinutes(15))); // announced -> falling
    engine.evaluateTrigger(
        instance.id(),
        new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
            .TimerElapsedTrigger(java.time.Duration.ofSeconds(3))); // falling -> locked

    // Simulate lock expiration manually since FakeScheduler doesn't fire it automatically
    stateStore.get(instance.id()).ifPresent(state -> state.setLockedUntilMillis(0));
    engine.evaluateTrigger(
        instance.id(),
        new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
            .TimerElapsedTrigger(java.time.Duration.ofMinutes(5))); // locked -> open

    assertEquals("open", repository.findById(instance.id()).get().currentPhase().get().value());

    int threadCount = 100;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Callable<Boolean>> tasks = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      UUID playerUuid = UUID.randomUUID();
      tasks.add(
          () -> {
            ExecutionContext context = ExecutionContext.withActor(playerUuid);
            return engine.evaluateTrigger(
                instance.id(), new ConfiguredTriggerDefinition("interaction"), context);
          });
    }

    List<Future<Boolean>> futures = executor.invokeAll(tasks);
    int successCount = 0;
    for (Future<Boolean> future : futures) {
      if (future.get()) {
        successCount++;
      }
    }

    executor.shutdown();

    // Opening is public: no player reserves the crate and all concurrent opens are accepted.
    assertEquals(threadCount, successCount);
    assertEquals(
        0, giveItemCount.get(), "Loot is delivered only by an atomic inventory slot click");
    assertEquals(
        4,
        stateStore.get(instance.id()).orElseThrow().sharedLootSnapshot().size(),
        "The configured public pool is generated once before any inventory is opened");
  }

  private static class FakeEventTaskScheduler implements EventTaskScheduler {
    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}
  }
}
