package io.github.kizio806.spectraevents.application.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetinConfigDrivenIntegrationTest {
  private InMemoryEventInstanceRepository repository;
  private EventDefinitionRegistry registry;
  private FakeEventTaskScheduler scheduler;
  private RecordingPlatformActionPort actionPort;
  private EventRuntimeStateStore stateStore;
  private EventExecutionEngine engine;

  @BeforeEach
  void setUp() throws Exception {
    repository = new InMemoryEventInstanceRepository();
    registry = new EventDefinitionRegistry();
    scheduler = new FakeEventTaskScheduler();
    actionPort = new RecordingPlatformActionPort();
    stateStore = new EventRuntimeStateStore();
    engine = new EventExecutionEngine(repository, registry, scheduler, actionPort, stateStore);

    // Read events/metin.yml
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("events/metin.yml");
    assertNotNull(inputStream, "metin.yml resource must exist");
    String yamlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    EventSpecYamlParser parser = new EventSpecYamlParser();
    EventDefinitionCompiler compiler = new EventDefinitionCompiler();
    EventDefinition metinDefinition = compiler.compile(parser.parse(yamlContent, "metin.yml"));
    registry.register(metinDefinition, "metin.yml");
  }

  @Test
  void testFullConfigDrivenMetinLifecycle() {
    // 1. Start event
    EventInstance instance =
        engine.startEvent("metin", new EventLocation("world", 100, 64, 100, 0, 0));
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals(new PhaseId("manifestation"), instance.currentPhase().orElseThrow());

    EventRuntimeState state = stateStore.get(instance.id()).orElseThrow();
    assertTrue(state.health().isPresent());
    assertEquals(1000, state.health().get().current());
    assertTrue(actionPort.containsAction("spawn_model"));
    assertTrue(actionPort.containsAction("play_sound"));

    // 2. Protected manifestation -> dominance.
    boolean handled1 =
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .TimerElapsedTrigger(java.time.Duration.ofSeconds(30)));
    assertTrue(handled1);

    EventInstance afterSpawning = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("dominance"), afterSpawning.currentPhase().orElseThrow());

    // 3. Dominance ends at 75%, then its bounded fracture wave gates the next phase.
    for (int i = 1; i <= 25; i++) {
      boolean hitHandled =
          engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction"));
      assertTrue(hitHandled, "Hit " + i + " was not handled");
    }
    EventInstance afterFracture = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("fracture"), afterFracture.currentPhase().orElseThrow());
    assertTrue(actionPort.containsAction("play_animation"));
    assertTrue(actionPort.containsAction("spawn_particles"));
    assertTrue(
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .WaveClearedTrigger("fracture_guard")));

    // 4. Desperation spawns its second bounded wave at 50%, then permits the push to 25%.
    for (int i = 1; i <= 25; i++) {
      engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction"));
    }
    assertTrue(actionPort.containsAction("spawn_mobs"));
    assertTrue(
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .WaveClearedTrigger("desperation_guard")));
    for (int i = 1; i <= 25; i++) {
      engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction"));
    }
    EventInstance finalAssault = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("final_assault"), finalAssault.currentPhase().orElseThrow());

    // 5. Defender and its final escort must all clear before victory.
    assertTrue(
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .WaveClearedTrigger("final_assault")));

    EventInstance afterVictory = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.COMPLETED, afterVictory.state());
    assertTrue(actionPort.containsAction("award_podium"));
    assertTrue(actionPort.containsAction("remove_model"));
  }

  private static class FakeEventTaskScheduler implements EventTaskScheduler {

    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}
  }

  private static class RecordingPlatformActionPort implements PlatformActionPort {
    List<ActionDefinition> actions = new ArrayList<>();

    @Override
    public java.util.concurrent.CompletableFuture<Boolean> executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      actions.add(action);
      return java.util.concurrent.CompletableFuture.completedFuture(true);
    }

    boolean containsAction(String type) {
      return actions.stream().anyMatch(a -> a.type().equalsIgnoreCase(type));
    }
  }
}
