package dev.spectraevents.application.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import dev.spectraevents.application.config.compiler.EventDefinitionCompiler;
import dev.spectraevents.application.config.registry.EventDefinitionRegistry;
import dev.spectraevents.application.config.yaml.EventSpecYamlParser;
import dev.spectraevents.application.execution.EventExecutionEngine;
import dev.spectraevents.application.execution.EventRuntimeState;
import dev.spectraevents.application.execution.EventRuntimeStateStore;
import dev.spectraevents.application.port.EventTaskScheduler;
import dev.spectraevents.application.port.PlatformActionPort;
import dev.spectraevents.application.repository.InMemoryEventInstanceRepository;
import dev.spectraevents.core.event.definition.EventDefinition;
import dev.spectraevents.core.event.execution.action.ActionDefinition;
import dev.spectraevents.core.event.phase.PhaseId;
import dev.spectraevents.core.event.runtime.EventInstance;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.core.event.runtime.EventLifecycleState;
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
    EventInstance instance = engine.startEvent("metin", "location_ref");
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals(new PhaseId("spawning"), instance.currentPhase().orElseThrow());

    // Check health initialized
    EventRuntimeState state = stateStore.get(instance.id()).orElseThrow();
    assertTrue(state.health().isPresent());
    assertEquals(100, state.health().get().current());
    assertTrue(actionPort.containsAction("spawn_model"));

    // 2. Timer: spawning -> active
    boolean handled1 =
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("timer_elapsed"));
    assertTrue(handled1);

    EventInstance afterSpawning = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("active"), afterSpawning.currentPhase().orElseThrow());

    // 3. Active: hit until health < 60%
    // Currently health is 100. Each hit deals 5 damage. We need 8 hits to reach 60, 9 to cross it.
    // 100 - 8*5 = 60. 100 - 9*5 = 55 (crosses 60 threshold).
    for (int i = 1; i <= 9; i++) {
      engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction"));
    }

    EventInstance afterEnraged = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("enraged"), afterEnraged.currentPhase().orElseThrow());
    assertTrue(actionPort.containsAction("play_sound"));
    assertTrue(actionPort.containsAction("spawn_particles"));

    // 4. Enraged: hit until health < 25%
    // Current health is 55. We need to reach < 25.
    // 55 - 6*5 = 25. 55 - 7*5 = 20 (crosses 25 threshold).
    for (int i = 1; i <= 7; i++) {
      engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction"));
    }

    EventInstance afterBoss = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("boss"), afterBoss.currentPhase().orElseThrow());
    assertTrue(actionPort.containsAction("spawn_boss"));

    // 5. Boss: interactions are blocked (boss must be defeated)
    boolean damageHandled =
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction"));
    assertTrue(damageHandled);
    assertTrue(actionPort.containsAction("send_message"));

    // 6. Boss defeated -> defeated
    boolean deathHandled =
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("entity_death"));
    assertTrue(deathHandled);

    EventInstance afterDefeated = repository.findById(instance.id()).orElseThrow();
    // In defeated phase it has on-enter actions including complete_event
    // So it should transition to completed.
    assertEquals(EventLifecycleState.COMPLETED, afterDefeated.state());
    assertTrue(actionPort.containsAction("remove_model"));
  }

  private static class FakeEventTaskScheduler implements EventTaskScheduler {
    boolean cancelled = false;

    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

    @Override
    public void cancelAll(EventInstanceId eventId) {
      cancelled = true;
    }

    @Override
    public void cancelAll() {
      cancelled = true;
    }
  }

  private static class RecordingPlatformActionPort implements PlatformActionPort {
    List<ActionDefinition> actions = new ArrayList<>();

    @Override
    public void executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      actions.add(action);
    }

    boolean containsAction(String type) {
      return actions.stream().anyMatch(a -> a.type().equalsIgnoreCase(type));
    }
  }
}
