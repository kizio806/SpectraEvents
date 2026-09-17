package io.github.kizio806.spectraevents.application.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
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

class MeteorConfigDrivenIntegrationTest {
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

    // Read events/meteor.yml
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("events/meteor.yml");
    assertNotNull(inputStream, "meteor.yml resource must exist");
    String yamlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    EventSpecYamlParser parser = new EventSpecYamlParser();
    EventDefinitionCompiler compiler = new EventDefinitionCompiler();
    EventDefinition meteorDefinition = compiler.compile(parser.parse(yamlContent, "meteor.yml"));
    registry.register(meteorDefinition, "meteor.yml");
  }

  @Test
  void testFullConfigDrivenMeteorLifecycle() {
    // 1. Start event
    EventInstance instance = engine.startEvent("meteor", "location_ref");
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals(new PhaseId("falling"), instance.currentPhase().orElseThrow());

    // Check health initialized
    EventRuntimeState state = stateStore.get(instance.id()).orElseThrow();
    assertTrue(state.health().isPresent());
    assertEquals(20, state.health().get().current());
    assertTrue(actionPort.containsAction("spawn_model"));

    // 2. Timer: falling -> impact
    boolean handled1 =
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("timer_elapsed"));
    assertTrue(handled1);

    EventInstance afterFalling = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("impact"), afterFalling.currentPhase().orElseThrow());
    assertTrue(actionPort.containsAction("move_model"));
    assertTrue(actionPort.containsAction("play_sound"));
    assertTrue(actionPort.containsAction("spawn_particles"));

    // 3. Timer: impact -> locked
    boolean handled2 =
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("timer_elapsed"));
    assertTrue(handled2);

    EventInstance afterImpact = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("locked"), afterImpact.currentPhase().orElseThrow());
    assertTrue(state.isLocked());

    // 4. Timer: locked -> active
    boolean handled3 =
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("timer_elapsed"));
    assertTrue(handled3);

    EventInstance afterLocked = repository.findById(instance.id()).orElseThrow();
    assertEquals(new PhaseId("active"), afterLocked.currentPhase().orElseThrow());

    // 5. Interactions damage health down to 0
    for (int i = 1; i <= 20; i++) {
      boolean damageHandled =
          engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction"));
      assertTrue(damageHandled);
    }

    // 6. After 20 hits, health is 0, health_depleted fired -> destroyed -> complete_event
    EventInstance finalInstance = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.COMPLETED, finalInstance.state());
    assertTrue(actionPort.containsAction("remove_model"));
  }

  @Test
  void testMeteorCancellationCleansUpResources() {
    EventInstance instance = engine.startEvent("meteor", "location_ref");
    assertEquals(EventLifecycleState.RUNNING, instance.state());

    EventInstance cancelled = engine.cancelEvent(instance.id());
    assertEquals(EventLifecycleState.CANCELLED, cancelled.state());

    assertTrue(stateStore.get(instance.id()).isEmpty());
    assertTrue(scheduler.cancelled);
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
