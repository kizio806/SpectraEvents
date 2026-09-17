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
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
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
    EventInstance instance = engine.startEvent("airdrop", "location_ref");

    // Fast-forward through falling -> locked -> open
    engine.evaluateTrigger(
        instance.id(), new ConfiguredTriggerDefinition("timer_elapsed")); // falling -> locked

    // Simulate lock expiration manually since FakeScheduler doesn't fire it automatically
    stateStore.get(instance.id()).ifPresent(state -> state.setLockedUntilMillis(0));
    engine.evaluateTrigger(
        instance.id(), new ConfiguredTriggerDefinition("timer_elapsed")); // locked -> open

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

    // Exactly 1 thread should succeed in claiming
    assertEquals(1, successCount, "Exactly 1 thread should successfully evaluate the trigger");

    // Exactly 1 give_item action should be executed
    assertEquals(1, giveItemCount.get(), "Exactly 1 give_item action should be executed");

    // Event should be completed
    EventInstance finalInstance = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.COMPLETED, finalInstance.state());
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
