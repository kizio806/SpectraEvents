package dev.spectraevents.application.config.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.spectraevents.application.config.spec.ActionSpec;
import dev.spectraevents.application.config.spec.ConditionSpec;
import dev.spectraevents.application.config.spec.EventSpec;
import dev.spectraevents.application.config.spec.PhaseSpec;
import dev.spectraevents.application.config.spec.TransitionSpec;
import dev.spectraevents.application.config.spec.TriggerSpec;
import dev.spectraevents.application.config.validation.ValidationDiagnostic;
import dev.spectraevents.core.event.definition.EventDefinition;
import dev.spectraevents.core.event.phase.PhaseId;
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
            "dev_airdrop",
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
                    List.of(new ActionSpec("spawn_entity", Map.of("type", "armor_stand")))),
                "locked",
                new PhaseSpec(Set.of(), List.of(), List.of())));

    EventDefinition def = compiler.compile(spec);

    assertEquals("dev_airdrop", def.id().value());
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
    assertEquals("spawn_entity", fallingPhase.onEnterActions().get(0).type());
  }

  @Test
  void testCompileFailsOnMissingInitialPhase() {
    EventSpec spec =
        new EventSpec(
            "dev_airdrop",
            "1.0",
            "unknown_phase",
            Map.of("falling", new PhaseSpec(Set.of(), List.of(), List.of())));

    EventDefinitionCompilerException ex =
        assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(spec));

    List<ValidationDiagnostic> diagnostics = ex.getDiagnostics();
    assertFalse(diagnostics.isEmpty());
    assertTrue(
        diagnostics.stream()
            .anyMatch(d -> d.code().equals("SE-DEF-003") && d.path().equals("initialPhase")));
  }

  @Test
  void testCompileFailsOnUnknownTargetPhase() {
    EventSpec spec =
        new EventSpec(
            "dev_airdrop",
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
                        && d.path().equals("phases.falling.allowedTransitions")));
  }
}
