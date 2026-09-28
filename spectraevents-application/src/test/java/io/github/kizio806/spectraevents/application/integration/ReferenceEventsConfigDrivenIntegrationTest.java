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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReferenceEventsConfigDrivenIntegrationTest {
  private InMemoryEventInstanceRepository repository;
  private RecordingPlatformActionPort actionPort;
  private EventRuntimeStateStore stateStore;
  private EventExecutionEngine engine;

  @BeforeEach
  void setUp() throws IOException {
    repository = new InMemoryEventInstanceRepository();
    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    actionPort = new RecordingPlatformActionPort();
    stateStore = new EventRuntimeStateStore();
    engine =
        new EventExecutionEngine(
            repository, registry, new FakeEventTaskScheduler(), actionPort, stateStore);

    EventSpecYamlParser parser = new EventSpecYamlParser();
    EventDefinitionCompiler compiler = new EventDefinitionCompiler();
    for (String event : List.of("pinata", "boss-portal")) {
      try {
        EventDefinition definition =
            compiler.compile(parser.parse(readResource(event), event + ".yml"));
        registry.register(definition, event + ".yml");
      } catch (
          io.github.kizio806.spectraevents.application.config.compiler
                  .EventDefinitionCompilerException
              e) {
        throw new AssertionError(
            "Could not compile bundled event " + event + ": " + e.getDiagnostics(), e);
      }
    }
  }

  @Test
  void pinataCountsInteractionsIndependentlyOfDamageAndCleansUpAtTheTarget() {
    EventInstance instance =
        engine.startEvent(
            "pinata",
            new io.github.kizio806.spectraevents.application.execution.EventLocation(
                "world", 0, 64, 0, 0, 0));
    assertEquals(new PhaseId("announced"), instance.currentPhase().orElseThrow());
    assertTrue(
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .TimerElapsedTrigger(java.time.Duration.ofSeconds(60))));

    assertTrue(
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .TimerElapsedTrigger(java.time.Duration.ofSeconds(1))));

    assertEquals(
        new PhaseId("active"),
        repository.findById(instance.id()).orElseThrow().currentPhase().orElseThrow());

    EventRuntimeState state = stateStore.get(instance.id()).orElseThrow();
    assertNotNull(state.hitCounter());
    assertEquals(0, state.hitCounter().current());

    for (int index = 0; index < 75; index++) {
      assertTrue(
          engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction")));
    }

    assertNotNull(state.hitCounter());
    assertEquals(75, state.hitCounter().current());
    assertEquals(
        new PhaseId("frenzy"),
        repository.findById(instance.id()).orElseThrow().currentPhase().orElseThrow());
    assertEquals(
        EventLifecycleState.RUNNING, repository.findById(instance.id()).orElseThrow().state());

    for (int index = 0; index < 25; index++) {
      assertTrue(
          engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction")));
    }
    assertEquals(
        EventLifecycleState.COMPLETED, repository.findById(instance.id()).orElseThrow().state());
    assertTrue(actionPort.containsAction("remove_model"));
    assertTrue(stateStore.get(instance.id()).isEmpty());
  }

  @Test
  void bossPortalUsesTheSharedTimerBossAndEntityDeathWorkflow() {
    EventInstance instance =
        engine.startEvent(
            "boss_portal",
            new io.github.kizio806.spectraevents.application.execution.EventLocation(
                "world", 0, 64, 0, 0, 0));
    assertEquals(new PhaseId("gathering"), instance.currentPhase().orElseThrow());
    assertTrue(actionPort.containsAction("spawn_model"));

    assertTrue(
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .TimerElapsedTrigger(java.time.Duration.ofMinutes(5))));
    assertEquals(
        new PhaseId("first-wave"),
        repository.findById(instance.id()).orElseThrow().currentPhase().orElseThrow());
    assertTrue(actionPort.containsAction("spawn_mobs"));

    assertTrue(
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .WaveClearedTrigger("portal-vanguard")));
    assertEquals(
        new PhaseId("second-wave"),
        repository.findById(instance.id()).orElseThrow().currentPhase().orElseThrow());

    assertTrue(
        engine.evaluateTrigger(
            instance.id(),
            new io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers
                .WaveClearedTrigger("portal-wardens")));
    assertEquals(
        new PhaseId("boss"),
        repository.findById(instance.id()).orElseThrow().currentPhase().orElseThrow());
    assertTrue(actionPort.containsAction("spawn_boss"));

    assertTrue(
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("entity_death")));
    assertEquals(
        EventLifecycleState.COMPLETED, repository.findById(instance.id()).orElseThrow().state());
    assertTrue(actionPort.containsAction("remove_bossbar"));
    assertTrue(actionPort.containsAction("remove_model"));
  }

  private String readResource(String event) throws IOException {
    try (InputStream input =
        getClass().getClassLoader().getResourceAsStream("events/" + event + ".yml")) {
      assertNotNull(input, "Missing event resource " + event);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static final class FakeEventTaskScheduler implements EventTaskScheduler {
    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}
  }

  private static final class RecordingPlatformActionPort implements PlatformActionPort {
    private final List<ActionDefinition> actions = new ArrayList<>();

    @Override
    public java.util.concurrent.CompletableFuture<Boolean> executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      actions.add(action);
      return java.util.concurrent.CompletableFuture.completedFuture(true);
    }

    boolean containsAction(String type) {
      return actions.stream().anyMatch(action -> type.equalsIgnoreCase(action.type()));
    }
  }
}
