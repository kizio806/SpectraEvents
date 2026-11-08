package io.github.kizio806.spectraevents.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompilerException;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterType;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import io.github.kizio806.spectraevents.application.config.spec.PhaseSpec;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class EventOrchestrationServiceTest {
  private final EventDefinitionRegistry definitions = new EventDefinitionRegistry();
  private final EventOrchestrationService service =
      new EventOrchestrationService(new InMemoryEventInstanceRepository(), definitions);

  EventOrchestrationServiceTest() {
    definitions.register(threePhaseDefinition(), "test-event.yml");
  }

  @Test
  void startsRegisteredDefinition() {
    EventInstance instance = service.startDefinition("test_event");
    assertNotNull(instance);
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals("phase_one", instance.currentPhase().orElseThrow().value());
  }

  @Test
  void transitionsPhase() {
    EventInstance instance = service.startDefinition("test_event");

    EventInstance next = service.transitionPhase(instance.id().toString());
    assertEquals("phase_two", next.currentPhase().orElseThrow().value());

    EventInstance finalPhase = service.transitionPhase(instance.id().toString());
    assertEquals("phase_three", finalPhase.currentPhase().orElseThrow().value());
  }

  @Test
  void queriesInfo() {
    EventInstance instance = service.startDefinition("test_event");
    EventInstance info = service.getEventInfo(instance.id().toString());
    assertEquals(instance.id(), info.id());
  }

  @Test
  void completesEvent() {
    EventInstance instance = service.startDefinition("test_event");
    EventInstance completed = service.completeEvent(instance.id().toString());
    assertEquals(EventLifecycleState.COMPLETED, completed.state());
  }

  @Test
  void cancelsEvent() {
    EventInstance instance = service.startDefinition("test_event");
    EventInstance cancelled = service.cancelEvent(instance.id().toString());
    assertEquals(EventLifecycleState.CANCELLED, cancelled.state());
  }

  @Test
  void startsDefinitionAsync() {
    EventInstance instance = service.startDefinitionAsync("test_event", null).join();
    assertNotNull(instance);
    assertEquals(EventLifecycleState.RUNNING, instance.state());
  }

  @Test
  void rejectsBlankDefinitionId() {
    assertThrows(IllegalArgumentException.class, () -> service.startDefinition(""));
    assertThrows(IllegalArgumentException.class, () -> service.startDefinition(null));
  }

  @Test
  void rejectsUnknownInstance() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.getEventInfo("00000000-0000-0000-0000-000000000000"));
  }

  @Test
  void rejectsInvalidUuid() {
    assertThrows(IllegalArgumentException.class, () -> service.getEventInfo("not-a-uuid"));
  }

  @Test
  void rejectsInvalidTransition() {
    EventInstance instance = service.startDefinition("test_event");
    service.transitionPhase(instance.id().toString()); // to two
    service.transitionPhase(instance.id().toString()); // to three

    // No more transitions
    assertThrows(
        IllegalStateException.class, () -> service.transitionPhase(instance.id().toString()));
  }

  @Test
  void testServiceWithExecutionEngine() {
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    EventDefinitionRegistry reg = new EventDefinitionRegistry();
    reg.register(threePhaseDefinition(), "test-event.yml");
    EventExecutionEngine engine =
        new EventExecutionEngine(
            repo, reg, new StubScheduler(), new StubActionPort(), new EventRuntimeStateStore());
    EventOrchestrationService engineService = new EventOrchestrationService(repo, reg, engine);

    assertEquals(engine, engineService.executionEngine());

    EventInstance inst = engineService.startDefinition("test_event");
    assertNotNull(inst);
    assertEquals(EventLifecycleState.RUNNING, inst.state());

    assertThrows(
        IllegalStateException.class, () -> engineService.transitionPhase(inst.id().toString()));

    EventInstance completed = engineService.completeEvent(inst.id().toString());
    assertEquals(EventLifecycleState.COMPLETED, completed.state());

    assertThrows(
        IllegalStateException.class, () -> engineService.completeEvent(inst.id().toString()));
    assertThrows(
        IllegalStateException.class, () -> engineService.cancelEvent(inst.id().toString()));

    EventInstance inst2 = engineService.startDefinition("test_event");
    EventInstance cancelled = engineService.cancelEvent(inst2.id().toString());
    assertEquals(EventLifecycleState.CANCELLED, cancelled.state());
  }

  @Test
  void testStartDefinitionAsyncWithEngine() {
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    EventDefinitionRegistry reg = new EventDefinitionRegistry();
    reg.register(threePhaseDefinition(), "test-event.yml");
    EventExecutionEngine engine =
        new EventExecutionEngine(
            repo, reg, new StubScheduler(), new StubActionPort(), new EventRuntimeStateStore());
    EventOrchestrationService engineService = new EventOrchestrationService(repo, reg, engine);

    EventInstance instance = engineService.startDefinitionAsync("test_event", null).join();
    assertNotNull(instance);
    assertEquals(EventLifecycleState.RUNNING, instance.state());
  }

  @Test
  void testStartDefinitionAsyncWithOverrides() {
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    EventDefinitionRegistry reg = new EventDefinitionRegistry();
    EventSpec spec =
        new EventSpec(
            "overridden_event",
            "1.0",
            "phase_one",
            Map.of("phase_one", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of(),
            Map.of(),
            Map.of(
                "speed",
                new EventParameterSpec(
                    "speed",
                    EventParameterType.INTEGER,
                    5,
                    BigDecimal.ONE,
                    BigDecimal.TEN,
                    BigDecimal.ONE,
                    true)));
    EventDefinition def = new EventDefinitionCompiler().compile(spec);
    reg.register(def, "overridden.yml", spec);

    EventExecutionEngine engine =
        new EventExecutionEngine(
            repo, reg, new StubScheduler(), new StubActionPort(), new EventRuntimeStateStore());
    EventOrchestrationService engineService = new EventOrchestrationService(repo, reg, engine);

    // Empty overrides delegates to normal startDefinitionAsync
    EventInstance instEmpty =
        engineService.startDefinitionAsync("overridden_event", null, Map.of()).join();
    assertNotNull(instEmpty);

    // Valid overrides with engine
    EventInstance instOverride =
        engineService.startDefinitionAsync("overridden_event", null, Map.of("speed", 8)).join();
    assertNotNull(instOverride);

    // Engine is null with overrides -> fails future
    CompletableFuture<EventInstance> noEngineFuture =
        service.startDefinitionAsync("overridden_event", null, Map.of("speed", 8));
    ExecutionException exNoEngine = assertThrows(ExecutionException.class, noEngineFuture::get);
    assertTrue(exNoEngine.getCause() instanceof IllegalStateException);

    // Definition not found in registry -> fails future
    CompletableFuture<EventInstance> notFoundFuture =
        engineService.startDefinitionAsync("unknown_event", null, Map.of("speed", 8));
    ExecutionException exNotFound = assertThrows(ExecutionException.class, notFoundFuture::get);
    assertTrue(exNotFound.getCause() instanceof IllegalArgumentException);

    // Invalid override value causes compilation exception -> fails future
    CompletableFuture<EventInstance> invalidOverrideFuture =
        engineService.startDefinitionAsync("overridden_event", null, Map.of("speed", 999));
    ExecutionException exInvalid =
        assertThrows(ExecutionException.class, invalidOverrideFuture::get);
    assertTrue(exInvalid.getCause() instanceof EventDefinitionCompilerException);
  }

  @Test
  void testResolveDefinitionMissingAfterStart() {
    EventInstance instance = service.startDefinition("test_event");
    definitions.clear();
    assertThrows(
        IllegalStateException.class, () -> service.transitionPhase(instance.id().toString()));
  }

  @Test
  void testResolveNextPhaseBranches() {
    PhaseId p1 = new PhaseId("p1");
    PhaseId p2 = new PhaseId("p2");
    PhaseId p3 = new PhaseId("p3");

    // Non-manual trigger
    TransitionRule ruleTick =
        new TransitionRule(
            new ConfiguredTriggerDefinition("tick"), List.of(), Optional.of(p2), List.of());
    // Manual trigger without target phase (targetPhase is empty)
    TransitionRule ruleManualNoTarget =
        new TransitionRule(
            new ConfiguredTriggerDefinition("manual"), List.of(), Optional.empty(), List.of());
    // Manual trigger with disallowed target phase (p3 not in allowedTransitions)
    TransitionRule ruleManualDisallowed =
        new TransitionRule(
            new ConfiguredTriggerDefinition("manual"), List.of(), Optional.of(p3), List.of());
    // Manual trigger with allowed target phase (p2)
    TransitionRule ruleManualAllowed =
        new TransitionRule(
            new ConfiguredTriggerDefinition("manual"), List.of(), Optional.of(p2), List.of());

    PhaseDefinition phase1 =
        new PhaseDefinition(
            p1,
            Set.of(p2),
            List.of(ruleTick, ruleManualNoTarget, ruleManualDisallowed, ruleManualAllowed),
            List.of());
    PhaseDefinition phase2 = new PhaseDefinition(p2, Set.of());

    EventDefinition customDef =
        new EventDefinition(
            new EventDefinitionId("custom_manual"), p1, Map.of(p1, phase1, p2, phase2));
    definitions.register(customDef, "custom.yml");

    EventInstance inst = service.startDefinition("custom_manual");
    EventInstance transitioned = service.transitionPhase(inst.id().toString());
    assertEquals("p2", transitioned.currentPhase().orElseThrow().value());

    // Phase 2 has no allowed transitions
    assertThrows(IllegalStateException.class, () -> service.transitionPhase(inst.id().toString()));
  }

  @Test
  void testTransitionPhaseWhenCurrentPhaseIsEmpty() {
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    EventOrchestrationService localService = new EventOrchestrationService(repo, definitions);
    EventInstanceId id = EventInstanceId.generate();
    EventInstance unstarted = EventInstance.create(id, new EventDefinitionId("test_event"));
    repo.save(unstarted);

    assertThrows(IllegalStateException.class, () -> localService.transitionPhase(id.toString()));
  }

  @Test
  void testEngineTransitionPhaseSuccess() {
    InMemoryEventInstanceRepository repo = new InMemoryEventInstanceRepository();
    EventDefinitionRegistry reg = new EventDefinitionRegistry();
    PhaseId pa = new PhaseId("phase_a");
    PhaseId pb = new PhaseId("phase_b");
    EventDefinition def =
        new EventDefinition(
            new EventDefinitionId("manual_event"),
            pa,
            Map.of(
                pa,
                new PhaseDefinition(
                    pa,
                    Set.of(pb),
                    List.of(
                        new TransitionRule(
                            new ConfiguredTriggerDefinition("manual"),
                            List.of(),
                            Optional.of(pb),
                            List.of())),
                    List.of()),
                pb,
                new PhaseDefinition(pb, Set.of())));
    reg.register(def, "manual_event.yml");

    EventExecutionEngine engine =
        new EventExecutionEngine(
            repo, reg, new StubScheduler(), new StubActionPort(), new EventRuntimeStateStore());
    EventOrchestrationService engineService = new EventOrchestrationService(repo, reg, engine);

    EventInstance instance = engineService.startDefinition("manual_event");
    assertEquals(pa, instance.currentPhase().orElseThrow());

    EventInstance transitioned = engineService.transitionPhase(instance.id().toString());
    assertEquals(pb, transitioned.currentPhase().orElseThrow());
  }

  @Test
  void testResolveNextPhaseUnknownCurrentPhase() {
    PhaseId p1 = new PhaseId("p1");
    PhaseId p2 = new PhaseId("p2");
    EventDefinition defInitial =
        new EventDefinition(
            new EventDefinitionId("evolving_event"),
            p1,
            Map.of(p1, new PhaseDefinition(p1, Set.of(p2)), p2, new PhaseDefinition(p2, Set.of())));
    definitions.register(defInitial, "evolving.yml");

    EventInstance inst = service.startDefinition("evolving_event");

    // Update definition to no longer contain p1
    EventDefinition defWithoutP1 =
        new EventDefinition(
            new EventDefinitionId("evolving_event"),
            p2,
            Map.of(p2, new PhaseDefinition(p2, Set.of())));
    definitions.registerOrUpdate(defWithoutP1, "evolving.yml");

    assertThrows(IllegalStateException.class, () -> service.transitionPhase(inst.id().toString()));
  }

  private static final class StubScheduler implements EventTaskScheduler {
    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
      task.run();
    }

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}
  }

  private static final class StubActionPort implements PlatformActionPort {
    @Override
    public CompletableFuture<Boolean> executeAction(
        EventInstance instance, EventRuntimeState state, ActionDefinition action) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public void cleanupEvent(EventInstanceId instanceId) {}
  }

  private EventDefinition threePhaseDefinition() {
    PhaseId first = new PhaseId("phase_one");
    PhaseId second = new PhaseId("phase_two");
    PhaseId third = new PhaseId("phase_three");
    return new EventDefinition(
        new EventDefinitionId("test_event"),
        first,
        Map.of(
            first, new PhaseDefinition(first, Set.of(second)),
            second, new PhaseDefinition(second, Set.of(third)),
            third, new PhaseDefinition(third, Set.of())));
  }
}
