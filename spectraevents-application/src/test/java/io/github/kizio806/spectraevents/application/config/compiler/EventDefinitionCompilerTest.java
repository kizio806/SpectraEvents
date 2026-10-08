package io.github.kizio806.spectraevents.application.config.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.spec.ActionSpec;
import io.github.kizio806.spectraevents.application.config.spec.ConditionSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterType;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import io.github.kizio806.spectraevents.application.config.spec.PhaseSpec;
import io.github.kizio806.spectraevents.application.config.spec.TransitionSpec;
import io.github.kizio806.spectraevents.application.config.spec.TriggerSpec;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventDefinitionCompilerTest {

  private final EventDefinitionCompiler compiler = new EventDefinitionCompiler();

  @Test
  void testCompileValidEventSpec() {
    EventSpec spec =
        new EventSpec(
            "airdrop",
            "1.0",
            "falling",
            Map.of(
                "falling",
                new PhaseSpec(
                    Set.of("locked"),
                    List.of(
                        new TransitionSpec(
                            new TriggerSpec("timer_elapsed", Map.of("duration", "10s")),
                            List.of(new ConditionSpec("altitude_below", Map.of("y", 64))),
                            "locked",
                            List.of(new ActionSpec("play_sound", Map.of("sound", "pling"))))),
                    List.of(
                        new ActionSpec(
                            "spawn_entity",
                            Map.of("entity_type", "minecraft:armor_stand", "name", "Guard")))),
                "locked",
                new PhaseSpec(Set.of(), List.of(), List.of())));

    EventDefinition def = compiler.compile(spec);

    assertEquals("airdrop", def.id().value());
    assertEquals(new PhaseId("falling"), def.initialPhase());
    assertTrue(def.phase(new PhaseId("falling")).isPresent());
    assertTrue(def.phase(new PhaseId("locked")).isPresent());

    var fallingPhase = def.phase(new PhaseId("falling")).get();
    assertEquals(1, fallingPhase.allowedTransitions().size());
    assertTrue(fallingPhase.allowsTransitionTo(new PhaseId("locked")));

    assertEquals(1, fallingPhase.rules().size());
    var rule = fallingPhase.rules().get(0);
    assertEquals("timer_elapsed", rule.trigger().type());
    assertEquals(1, rule.conditions().size());
    assertEquals("altitude_below", rule.conditions().get(0).type());
    assertTrue(rule.targetPhase().isPresent());
    assertEquals(new PhaseId("locked"), rule.targetPhase().get());
    assertEquals(1, rule.actions().size());
    assertEquals("play_sound", rule.actions().get(0).type());

    assertEquals(1, fallingPhase.onEnterActions().size());
    assertEquals("spawn_boss", fallingPhase.onEnterActions().get(0).type());
  }

  @Test
  void testCompileFailsOnMissingInitialPhase() {
    EventSpec spec =
        new EventSpec(
            "airdrop",
            "1.0",
            "unknown_phase",
            Map.of("falling", new PhaseSpec(Set.of(), List.of(), List.of())));

    EventDefinitionCompilerException ex =
        assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(spec));

    List<ValidationDiagnostic> diagnostics = ex.getDiagnostics();
    assertFalse(diagnostics.isEmpty());
    assertTrue(
        diagnostics.stream()
            .anyMatch(d -> d.code().equals("SE-DEF-003") && d.path().equals("initial-phase")));
  }

  @Test
  void testCompileFailsOnUnknownTargetPhase() {
    EventSpec spec =
        new EventSpec(
            "airdrop",
            "1.0",
            "falling",
            Map.of("falling", new PhaseSpec(Set.of("non_existent"), List.of(), List.of())));

    EventDefinitionCompilerException ex =
        assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(spec));

    List<ValidationDiagnostic> diagnostics = ex.getDiagnostics();
    assertFalse(diagnostics.isEmpty());
    assertTrue(
        diagnostics.stream()
            .anyMatch(
                d ->
                    d.code().equals("SE-DEF-005")
                        && d.path().equals("phases.falling.transitions")));
  }

  @Test
  void compilesTypedHitPercentageThresholdTrigger() {
    EventSpec spec =
        new EventSpec(
            "pinata",
            "1",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of("frenzy"),
                    List.of(
                        new TransitionSpec(
                            new TriggerSpec(
                                "hits_percent_threshold_crossed", Map.of("percent", 75)),
                            List.of(),
                            "frenzy",
                            List.of())),
                    List.of()),
                "frenzy",
                new PhaseSpec(Set.of(), List.of(), List.of())));

    EventDefinition definition = compiler.compile(spec);
    assertTrue(
        definition.phase(new PhaseId("active")).orElseThrow().rules().getFirst().trigger()
            instanceof CoreTriggers.HitsPercentThresholdCrossedTrigger);
  }

  @Test
  void rejectsUnknownActionsAndMissingRequiredActionParametersBeforeRuntime() {
    EventSpec unknownAction =
        new EventSpec(
            "invalid",
            "1",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of(), List.of(), List.of(new ActionSpec("teleport_everyone", Map.of())))));
    EventSpec missingModel =
        new EventSpec(
            "invalid-model",
            "1",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of(), List.of(), List.of(new ActionSpec("spawn_model", Map.of())))));

    assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(unknownAction));
    assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(missingModel));
  }

  @Test
  void resolvesParameterReferencesEmbeddedInPlayerVisibleTemplates() {
    EventSpec spec =
        new EventSpec(
            "airdrop",
            "1",
            "announced",
            Map.of(
                "announced",
                new PhaseSpec(
                    Set.of(),
                    List.of(),
                    List.of(
                        new ActionSpec(
                            "broadcast_message",
                            Map.of("message", "Landing in ${announcement-delay}"))))),
            Map.of(),
            Map.of(),
            Map.of(
                "announcement-delay",
                new EventParameterSpec(
                    "announcement-delay",
                    EventParameterType.DURATION,
                    "15m",
                    BigDecimal.valueOf(60),
                    BigDecimal.valueOf(3_600),
                    BigDecimal.valueOf(60),
                    true)));

    EventDefinition definition = compiler.compile(spec, Map.of("announcement-delay", "16m"));

    PlatformActions.BroadcastMessageAction action =
        (PlatformActions.BroadcastMessageAction)
            definition.phase(new PhaseId("announced")).orElseThrow().onEnterActions().getFirst();
    assertEquals("Landing in 16m", action.message());
  }

  @Test
  void rejectsMalformedDurationInsteadOfTreatingItAsZero() {
    EventSpec spec =
        new EventSpec(
            "timer",
            "1",
            "waiting",
            Map.of(
                "waiting",
                new PhaseSpec(
                    Set.of(),
                    List.of(
                        new TransitionSpec(
                            new TriggerSpec("timer_elapsed", Map.of("duration", "soon")),
                            List.of(),
                            null,
                            List.of())),
                    List.of())));

    EventDefinitionCompilerException exception =
        assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(spec));

    assertTrue(
        exception.getDiagnostics().stream()
            .anyMatch(diagnostic -> diagnostic.code().equals("SE-DEF-VALUE-001")));
  }

  @Test
  void acceptsMillisecondDurationSyntaxDocumentedForDefinitions() {
    EventSpec spec =
        new EventSpec(
            "timer",
            "1",
            "waiting",
            Map.of(
                "waiting",
                new PhaseSpec(
                    Set.of(),
                    List.of(
                        new TransitionSpec(
                            new TriggerSpec("timer_elapsed", Map.of("duration", "500ms")),
                            List.of(),
                            null,
                            List.of())),
                    List.of())));

    EventDefinition definition = compiler.compile(spec);
    CoreTriggers.TimerElapsedTrigger trigger =
        (CoreTriggers.TimerElapsedTrigger)
            definition.phase(new PhaseId("waiting")).orElseThrow().rules().getFirst().trigger();

    assertEquals(java.time.Duration.ofMillis(500), trigger.duration());
  }
}
