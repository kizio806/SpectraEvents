package io.github.kizio806.spectraevents.application.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
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
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("StringConcatToTextBlock")
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
    EventDefinition meteorDefinition =
        compiler.compile(
            parser.parse(yamlContent, "meteor.yml"),
            Map.of("maximum-health", 10000, "maximum-hit-damage", 1000));
    registry.register(meteorDefinition, "meteor.yml");
  }

  @Test
  void testFullConfigDrivenMeteorLifecycle() {
    // 1. Start event
    EventInstance instance = engine.startEvent("meteor", location());
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals(new PhaseId("announced"), instance.currentPhase().orElseThrow());

    EventRuntimeState state = stateStore.get(instance.id()).orElseThrow();
    assertTrue(state.health().isPresent());
    assertEquals(10000, state.health().get().current());
    assertTrue(actionPort.containsAction("show_bossbar"));

    // Announced -> falling -> impact lock -> first vulnerable phase.
    boolean handled1 =
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .TimerElapsedTrigger(java.time.Duration.ofSeconds(1)));
    assertTrue(handled1);

    assertEquals(new PhaseId("falling"), phase(instance));
    assertTrue(actionPort.containsAction("spawn_model"));
    assertTrue(engine.evaluateTrigger(instance.id(), timer()));
    assertEquals(new PhaseId("impact_lock"), phase(instance));
    assertTrue(engine.evaluateTrigger(instance.id(), timer()));
    assertEquals(new PhaseId("assault_one"), phase(instance));

    damageUntilGate(instance, 3);
    assertEquals(new PhaseId("fracture_guard"), phase(instance));
    assertTrue(engine.evaluateTrigger(instance.id(), waveCleared("fracture_guard")));
    assertEquals(new PhaseId("assault_two"), phase(instance));

    damageUntilGate(instance, 3);
    assertEquals(new PhaseId("eruption_guard"), phase(instance));
    assertTrue(engine.evaluateTrigger(instance.id(), waveCleared("eruption_guard")));
    assertEquals(new PhaseId("assault_three"), phase(instance));

    damageUntilGate(instance, 3);
    assertEquals(new PhaseId("cataclysm"), phase(instance));
    assertTrue(engine.evaluateTrigger(instance.id(), waveCleared("cataclysm_guard")));
    assertEquals(new PhaseId("final_core"), phase(instance));

    damageUntilGate(instance, 3);
    EventInstance finalInstance = repository.findById(instance.id()).orElseThrow();
    assertEquals(EventLifecycleState.COMPLETED, finalInstance.state());
    assertTrue(actionPort.containsAction("remove_model"));
  }

  @Test
  void testMeteorCancellationCleansUpResources() {
    EventInstance instance = engine.startEvent("meteor", location());
    assertEquals(EventLifecycleState.RUNNING, instance.state());

    EventInstance cancelled = engine.cancelEvent(instance.id());
    assertEquals(EventLifecycleState.CANCELLED, cancelled.state());

    assertTrue(stateStore.get(instance.id()).isEmpty());
    assertTrue(scheduler.cancelled);
  }

  @Test
  void failsWithoutLootWhenAGatedWaveTimesOut() {
    EventInstance instance = engine.startEvent("meteor", location());
    assertTrue(engine.evaluateTrigger(instance.id(), timer()));
    assertTrue(engine.evaluateTrigger(instance.id(), timer()));
    assertTrue(engine.evaluateTrigger(instance.id(), timer()));
    damageUntilGate(instance, 3);
    assertEquals(new PhaseId("fracture_guard"), phase(instance));

    assertTrue(engine.evaluateTrigger(instance.id(), timer()));

    assertEquals(
        EventLifecycleState.FAILED, repository.findById(instance.id()).orElseThrow().state());
    assertFalse(actionPort.containsAction("release_ground_loot"));
  }

  private PhaseId phase(EventInstance instance) {
    return repository.findById(instance.id()).orElseThrow().currentPhase().orElseThrow();
  }

  private io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
          .TimerElapsedTrigger
      timer() {
    return new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
        .TimerElapsedTrigger(Duration.ofSeconds(1));
  }

  private io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
          .WaveClearedTrigger
      waveCleared(String waveId) {
    return new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
        .WaveClearedTrigger(waveId);
  }

  private void damageUntilGate(EventInstance instance, int hits) {
    for (int index = 0; index < hits; index++) {
      assertTrue(
          engine.evaluateTrigger(
              instance.id(),
              new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                  .CombatDamageTrigger(),
              ExecutionContext.withCombatDamage(new Object(), UUID.randomUUID(), 5000.0d)));
    }
  }

  private EventLocation location() {
    return new EventLocation("world", 30.0d, 70.0d, -20.0d, 0.0f, 0.0f);
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
