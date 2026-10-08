package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HitPercentThresholdIntegrationTest {

  @Test
  void transitionsOnceWhenTheCounterCrossesTheConfiguredPercentage() {
    EventDefinition definition =
        new EventDefinition(
            new EventDefinitionId("pinata"),
            new PhaseId("active"),
            Map.of(
                new PhaseId("active"),
                new PhaseDefinition(
                    new PhaseId("active"),
                    Set.of(new PhaseId("frenzy")),
                    List.of(
                        new TransitionRule(
                            new CoreTriggers.HitsPercentThresholdCrossedTrigger(75),
                            List.of(),
                            java.util.Optional.of(new PhaseId("frenzy")),
                            List.of()),
                        new TransitionRule(
                            new ConfiguredTriggerDefinition("interaction"),
                            List.of(),
                            java.util.Optional.empty(),
                            List.of(new CoreActions.IncrementHitsAction(1)))),
                    List.of(new CoreActions.InitializeHitCounterAction(4))),
                new PhaseId("frenzy"),
                new PhaseDefinition(new PhaseId("frenzy"), Set.of(), List.of(), List.of())));
    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    registry.register(definition, "pinata.yml");
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    EventExecutionEngine engine =
        new EventExecutionEngine(
            repository,
            registry,
            new NoopScheduler(),
            (instance, state, action) ->
                java.util.concurrent.CompletableFuture.completedFuture(true),
            new EventRuntimeStateStore());

    var instance = engine.startEvent("pinata", "location");
    assertTrue(
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction")));
    assertTrue(
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction")));
    assertEquals(
        new PhaseId("active"),
        repository.findById(instance.id()).orElseThrow().currentPhase().orElseThrow());

    assertTrue(
        engine.evaluateTrigger(instance.id(), new ConfiguredTriggerDefinition("interaction")));
    assertEquals(
        new PhaseId("frenzy"),
        repository.findById(instance.id()).orElseThrow().currentPhase().orElseThrow());
  }

  private static final class NoopScheduler implements EventTaskScheduler {
    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}
  }
}
