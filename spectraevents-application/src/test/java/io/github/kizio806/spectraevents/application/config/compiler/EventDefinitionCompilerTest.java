package io.github.kizio806.spectraevents.application.config.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import io.github.kizio806.spectraevents.core.event.definition.EventEncounterSettings;
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

  @Test
  void testCompileEncounterWithoutLimits() {
    EventSpec spec =
        new EventSpec(
            "encounter_only",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("zone-radius", 30.0, "deadline", "5m"),
            null,
            Map.of());

    EventDefinition def = compiler.compile(spec);
    assertEquals("encounter_only", def.id().value());
    assertEquals(30.0, def.encounterSettings().zoneRadius());
    assertEquals(java.time.Duration.ofMinutes(5), def.encounterSettings().deadline());
    assertEquals(1, def.encounterSettings().maxActivePerWorld());
  }

  @Test
  void testCompileLimitsWithoutEncounter() {
    EventSpec spec =
        new EventSpec(
            "limits_only",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            null,
            Map.of("max-active-per-world", 3),
            Map.of());

    EventDefinition def = compiler.compile(spec);
    assertEquals("limits_only", def.id().value());
    assertEquals(0.0, def.encounterSettings().zoneRadius());
    assertEquals(3, def.encounterSettings().maxActivePerWorld());
  }

  @Test
  void testCompileEncounterAndLimitsValidationErrors() {
    EventSpec invalidEnc =
        new EventSpec(
            "invalid_enc",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("unknown-encounter-key", "val"),
            Map.of("unknown-limit-key", 1),
            Map.of());

    EventDefinitionCompilerException ex =
        assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(invalidEnc));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-DEF-ENC-001")));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-DEF-LIMIT-001")));
  }

  @Test
  void testCompileActionsWithInvalidNumberStringReportsDiagnostics() {
    EventSpec spec =
        new EventSpec(
            "action_invalid_numbers",
            "1.0",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of(),
                    List.of(),
                    List.of(
                        new ActionSpec(
                            "play_sound",
                            Map.of("sound", "bell", "volume", "not_a_float", "pitch", "1.5")),
                        new ActionSpec(
                            "spawn_particles",
                            Map.of("particle", "flame", "count", "invalid_int"))))),
            null,
            null,
            Map.of());

    EventDefinitionCompilerException ex =
        assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(spec));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-DEF-VALUE-001")));
  }

  @Test
  void testCompileEncounterWithoutDeadline() {
    EventSpec spec =
        new EventSpec(
            "encounter_no_deadline",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("zone-radius", 25.0, "minimum-contribution", 10),
            Map.of("max-active-per-world", 2),
            Map.of());

    EventDefinition def = compiler.compile(spec);
    assertEquals("encounter_no_deadline", def.id().value());
    assertEquals(25.0, def.encounterSettings().zoneRadius());
    assertEquals(java.time.Duration.ZERO, def.encounterSettings().deadline());
    assertEquals(10, def.encounterSettings().minimumContribution());
    assertEquals(2, def.encounterSettings().maxActivePerWorld());
  }

  @Test
  void testCompileEncounterEmptyMapsReturnsNone() {
    EventSpec spec =
        new EventSpec(
            "encounter_empty",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of(),
            Map.of(),
            Map.of());

    EventDefinition def = compiler.compile(spec);
    assertEquals(EventEncounterSettings.NONE, def.encounterSettings());
  }

  @Test
  void testCompileEncounterWithInvalidSettingsProducesDiagnostic() {
    EventSpec negativeRadius =
        new EventSpec(
            "negative_radius",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("zone-radius", -10.0),
            null,
            Map.of());

    EventDefinitionCompilerException ex1 =
        assertThrows(
            EventDefinitionCompilerException.class, () -> compiler.compile(negativeRadius));
    assertTrue(ex1.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-DEF-ENC-002")));

    EventSpec negativeContribution =
        new EventSpec(
            "negative_contrib",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("minimum-contribution", -1),
            null,
            Map.of());

    EventDefinitionCompilerException ex2 =
        assertThrows(
            EventDefinitionCompilerException.class, () -> compiler.compile(negativeContribution));
    assertTrue(ex2.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-DEF-ENC-002")));

    EventSpec zeroActiveWithZone =
        new EventSpec(
            "zero_active_with_zone",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("zone-radius", 15.0),
            Map.of("max-active-per-world", 0),
            Map.of());

    EventDefinitionCompilerException ex3 =
        assertThrows(
            EventDefinitionCompilerException.class, () -> compiler.compile(zeroActiveWithZone));
    assertTrue(ex3.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-DEF-ENC-002")));
  }

  @Test
  void testCompilerNumericParsingBranches() {
    // 1. getInt with decimal Number throws SE-DEF-VALUE-001
    EventSpec decimalIntSpec =
        new EventSpec(
            "decimal_int",
            "1.0",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of(),
                    List.of(),
                    List.of(new ActionSpec("initialize_health", Map.of("max", 20.5))))));
    assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(decimalIntSpec));

    // 2. getInt with string number parses successfully
    EventSpec stringIntSpec =
        new EventSpec(
            "string_int",
            "1.0",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of(),
                    List.of(),
                    List.of(new ActionSpec("initialize_health", Map.of("max", "25"))))));
    assertNotNull(compiler.compile(stringIntSpec));

    // 2b. getInt with non-integer string throws SE-DEF-VALUE-001
    EventSpec invalidStringIntSpec =
        new EventSpec(
            "invalid_str_int",
            "1.0",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of(),
                    List.of(),
                    List.of(new ActionSpec("initialize_health", Map.of("max", "not_an_int"))))));
    assertThrows(
        EventDefinitionCompilerException.class, () -> compiler.compile(invalidStringIntSpec));

    // 3. getFloat with string float parses successfully
    EventSpec stringFloatSpec =
        new EventSpec(
            "string_float",
            "1.0",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of(),
                    List.of(),
                    List.of(
                        new ActionSpec(
                            "play_sound",
                            Map.of("sound", "ambient.cave", "volume", "1.5", "pitch", "0.8"))))));
    assertNotNull(compiler.compile(stringFloatSpec));

    // 4. getFloat with NaN Number throws SE-DEF-VALUE-001
    EventSpec nanFloatSpec =
        new EventSpec(
            "nan_float",
            "1.0",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of(),
                    List.of(),
                    List.of(
                        new ActionSpec(
                            "play_sound", Map.of("sound", "ambient.cave", "volume", Float.NaN))))));
    assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(nanFloatSpec));

    // 5. getFloat with string NaN throws SE-DEF-VALUE-001
    EventSpec stringNanFloatSpec =
        new EventSpec(
            "string_nan_float",
            "1.0",
            "active",
            Map.of(
                "active",
                new PhaseSpec(
                    Set.of(),
                    List.of(),
                    List.of(
                        new ActionSpec(
                            "play_sound", Map.of("sound", "ambient.cave", "volume", "NaN"))))));
    assertThrows(
        EventDefinitionCompilerException.class, () -> compiler.compile(stringNanFloatSpec));

    // 6. getDouble with string double parses successfully
    EventSpec stringDoubleSpec =
        new EventSpec(
            "string_double",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("zone-radius", "45.5"),
            null,
            Map.of());
    EventDefinition stringDoubleDef = compiler.compile(stringDoubleSpec);
    assertEquals(45.5, stringDoubleDef.encounterSettings().zoneRadius());

    // 7. getDouble with non-finite Number throws SE-DEF-VALUE-001
    EventSpec nanDoubleSpec =
        new EventSpec(
            "nan_double",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("zone-radius", Double.NaN),
            null,
            Map.of());
    assertThrows(EventDefinitionCompilerException.class, () -> compiler.compile(nanDoubleSpec));

    // 8. getDouble with string NaN throws SE-DEF-VALUE-001
    EventSpec stringNanDoubleSpec =
        new EventSpec(
            "string_nan_double",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("zone-radius", "NaN"),
            null,
            Map.of());
    assertThrows(
        EventDefinitionCompilerException.class, () -> compiler.compile(stringNanDoubleSpec));

    // 9. getDouble with non-number string throws SE-DEF-VALUE-001
    EventSpec invalidDoubleStringSpec =
        new EventSpec(
            "invalid_double_str",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), List.of())),
            Map.of("zone-radius", "not_a_double"),
            null,
            Map.of());
    assertThrows(
        EventDefinitionCompilerException.class, () -> compiler.compile(invalidDoubleStringSpec));
  }

  @Test
  void testCompileAllActionAndConditionTypes() {
    List<ActionSpec> actions =
        List.of(
            new ActionSpec("initialize_health", Map.of("max", 100)),
            new ActionSpec("initialize_hit_counter", Map.of("max", 50)),
            new ActionSpec("set_locked", Map.of("duration", "5s")),
            new ActionSpec("try_claim", Map.of()),
            new ActionSpec(
                "initialize_shared_loot",
                Map.of("items", List.of(Map.of("material", "minecraft:diamond", "amount", 3)))),
            new ActionSpec(
                "initialize_ground_loot",
                Map.of(
                    "items",
                    List.of(
                        Map.of("material", "minecraft:gold_ingot", "amount", 5, "chance", 80)))),
            new ActionSpec(
                "create_participant_reward_claims",
                Map.of(
                    "items",
                    List.of(Map.of("material", "minecraft:emerald", "amount", 2, "chance", 100)))),
            new ActionSpec("apply_damage", Map.of("amount", 10)),
            new ActionSpec("increment_hits", Map.of("amount", 2)),
            new ActionSpec("fail_event", Map.of()),
            new ActionSpec("cancel_event", Map.of()),
            new ActionSpec("give_item", Map.of("material", "minecraft:apple", "amount", 1)),
            new ActionSpec("open_shared_loot", Map.of("title", "Airdrop Chest")),
            new ActionSpec(
                "drop_loot",
                Map.of(
                    "radius",
                    3.0f,
                    "items",
                    List.of(
                        Map.of("material", "minecraft:iron_ingot", "amount", 4, "chance", 90)))),
            new ActionSpec("release_ground_loot", Map.of("radius", 4.0f)),
            new ActionSpec(
                "award_podium",
                Map.of(
                    "pools",
                    List.of(
                        Map.of(
                            "items",
                            List.of(
                                Map.of(
                                    "material", "minecraft:diamond", "amount", 3, "chance", 100))),
                        Map.of(
                            "items",
                            List.of(
                                Map.of(
                                    "material",
                                    "minecraft:gold_ingot",
                                    "amount",
                                    2,
                                    "chance",
                                    100))),
                        Map.of(
                            "items",
                            List.of(
                                Map.of(
                                    "material",
                                    "minecraft:iron_ingot",
                                    "amount",
                                    1,
                                    "chance",
                                    100)))))),
            new ActionSpec("broadcast", Map.of("message", "Attention!")),
            new ActionSpec(
                "show_title",
                Map.of(
                    "title",
                    "VICTORY",
                    "subtitle",
                    "Event completed",
                    "fade-in",
                    10,
                    "stay",
                    40,
                    "fade-out",
                    10)),
            new ActionSpec(
                "spawn_wave",
                Map.of(
                    "wave-id",
                    "wave_1",
                    "mobs",
                    List.of(
                        Map.of(
                            "entity_type",
                            "minecraft:zombie",
                            "name",
                            "Undead",
                            "amount",
                            3,
                            "radius",
                            2.5f)))),
            new ActionSpec(
                "show_bossbar",
                Map.of(
                    "color",
                    "PURPLE",
                    "style",
                    "NOTCHED_10",
                    "title",
                    "Boss Health",
                    "progress",
                    "0.8")),
            new ActionSpec("update_bossbar", Map.of("title", "Enraged Boss", "progress", "0.5")),
            new ActionSpec("remove_bossbar", Map.of()),
            new ActionSpec(
                "show_scoreboard",
                Map.of("title", "Event Stats", "lines", List.of("Line 1", "Line 2"))),
            new ActionSpec("update_scoreboard", Map.of("lines", List.of("Line A", "Line B"))),
            new ActionSpec("remove_scoreboard", Map.of()));

    List<TransitionSpec> transitions =
        List.of(
            new TransitionSpec(
                new TriggerSpec("timer_elapsed", Map.of("duration", "5s")),
                List.of(
                    new ConditionSpec("health_below", Map.of("value", 50)),
                    new ConditionSpec("health_at_least", Map.of("value", 20)),
                    new ConditionSpec("hits_below", Map.of("value", 30)),
                    new ConditionSpec("hits_at_least", Map.of("value", 10))),
                "finish",
                List.of()));

    EventSpec spec =
        new EventSpec(
            "comprehensive_actions",
            "1.0",
            "active",
            Map.of(
                "active", new PhaseSpec(Set.of("finish"), transitions, actions),
                "finish", new PhaseSpec(Set.of(), List.of(), List.of())));

    EventDefinition def = compiler.compile(spec);
    assertEquals("comprehensive_actions", def.id().value());
    var phase = def.phase(new PhaseId("active")).orElseThrow();
    assertEquals(actions.size(), phase.onEnterActions().size());
  }

  @Test
  void testCompilesActionsWithDefaultFallbackValues() {
    List<ActionSpec> actions =
        List.of(
            new ActionSpec("show_title", Map.of()),
            new ActionSpec(
                "spawn_mobs",
                Map.of(
                    "mobs",
                    List.of(
                        Map.of()))), // empty mob map -> defaults to minecraft:zombie and <red>Mob
            new ActionSpec(
                "create_participant_reward_claims",
                Map.of(
                    "items",
                    List.of(
                        Map.of("amount", 2)))), // missing material -> defaults to minecraft:diamond
            new ActionSpec(
                "award_podium",
                Map.of(
                    "pools",
                    List.of(
                        Map.of("items", List.of(Map.of("amount", 3))),
                        Map.of("items", List.of(Map.of("amount", 2))),
                        Map.of(
                            "items",
                            List.of(
                                Map.of(
                                    "amount",
                                    1))))))); // missing material -> defaults to minecraft:diamond

    EventSpec spec =
        new EventSpec(
            "default_action_values",
            "1.0",
            "active",
            Map.of("active", new PhaseSpec(Set.of(), List.of(), actions)));

    try {
      EventDefinition def = compiler.compile(spec);
      assertNotNull(def);
      var phase = def.phase(new PhaseId("active")).orElseThrow();
      assertEquals(4, phase.onEnterActions().size());
    } catch (EventDefinitionCompilerException e) {
      org.junit.jupiter.api.Assertions.fail(
          "Diagnostics: "
              + e.getDiagnostics().stream().map(d -> d.code() + ": " + d.message()).toList());
    }
  }
}
