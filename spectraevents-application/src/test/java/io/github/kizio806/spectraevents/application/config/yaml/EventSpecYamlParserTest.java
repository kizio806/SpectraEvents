package io.github.kizio806.spectraevents.application.config.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompilerException;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventSpecYamlParserTest {
  private final EventSpecYamlParser parser = new EventSpecYamlParser();

  @Test
  void testParseValidMinimalEvent() {
    String yaml =
        """
        schema-version: 1
        id: example
        initial-phase: waiting
        phases:
          waiting:
            transitions:
              - trigger:
                  type: manual
                target: active
          active:
            transitions:
              - trigger:
                  type: manual
                target: completed
        """;

    EventSpec spec = parser.parse(yaml, "example.yml");
    assertEquals("example", spec.id());
    assertEquals("1", spec.schemaVersion());
    assertEquals("waiting", spec.initialPhase());
    assertNotNull(spec.phases().get("waiting"));
    assertNotNull(spec.phases().get("active"));
    assertEquals(1, spec.phases().get("waiting").transitions().size());
    assertEquals("manual", spec.phases().get("waiting").transitions().get(0).trigger().type());
  }

  @Test
  void testParseMissingSchemaVersion() {
    String yaml =
        """
        id: example
        """;
    var ex =
        assertThrows(EventDefinitionCompilerException.class, () -> parser.parse(yaml, "f.yml"));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-YAML-003")));
  }

  @Test
  void testParseUnsupportedSchemaVersion() {
    String yaml =
        """
        schema-version: 999
        """;
    var ex =
        assertThrows(EventDefinitionCompilerException.class, () -> parser.parse(yaml, "f.yml"));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-YAML-004")));
  }

  @Test
  void testParseInvalidYamlSyntax() {
    String yaml =
        """
        schema-version: 1
        id: [ broken
        """;
    var ex =
        assertThrows(EventDefinitionCompilerException.class, () -> parser.parse(yaml, "f.yml"));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-YAML-001")));
  }

  @Test
  void rejectsUnknownFieldsWithSourceAndPath() {
    String yaml =
        """
        schema-version: 1
        id: example
        initial-phase: waiting
        display: ignored-before-this-regression
        phases:
          waiting:
            components: {}
        """;

    var ex =
        assertThrows(
            EventDefinitionCompilerException.class, () -> parser.parse(yaml, "events/example.yml"));

    assertTrue(
        ex.getDiagnostics().stream()
            .anyMatch(
                d ->
                    d.code().equals("SE-YAML-010")
                        && d.path().equals("events/example.yml:root.display")));
    assertTrue(
        ex.getDiagnostics().stream()
            .anyMatch(
                d ->
                    d.code().equals("SE-YAML-010")
                        && d.path().equals("events/example.yml:phases.waiting.components")));
  }

  @Test
  void rejectsWrongCollectionTypesInsteadOfSilentlyDroppingThem() {
    String yaml =
        """
        schema-version: 1
        id: malformed
        initial-phase: active
        phases:
          active:
            transitions: not-a-list
            on-enter: not-a-list
        """;

    var ex =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yaml, "events/malformed.yml"));

    assertTrue(
        ex.getDiagnostics().stream().anyMatch(d -> d.path().endsWith("phases.active.transitions")));
    assertTrue(
        ex.getDiagnostics().stream().anyMatch(d -> d.path().endsWith("phases.active.onEnter")));
  }

  @Test
  void rejectsDuplicateYamlKeys() {
    String yaml =
        """
        schema-version: 1
        id: first
        id: second
        initial-phase: active
        phases:
          active: {}
        """;

    assertThrows(
        EventDefinitionCompilerException.class,
        () -> parser.parse(yaml, "events/duplicate-key.yml"));
  }

  @Test
  void rejectsStructuredValuesForScalarFieldsWithSourcePath() {
    String yaml =
        """
        schema-version: 1
        id:
          nested: value
        initial-phase: active
        phases:
          active: {}
        """;

    var ex =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yaml, "events/invalid-type.yml"));

    assertTrue(
        ex.getDiagnostics().stream()
            .anyMatch(
                d ->
                    d.code().equals("SE-YAML-011")
                        && d.path().equals("events/invalid-type.yml:id")));
  }

  @Test
  void rejectsNonMapParametersInsteadOfSilentlyDroppingThem() {
    String yaml =
        """
        schema-version: 1
        id: example
        initial-phase: active
        phases:
          active:
            transitions:
              - trigger:
                  type: manual
                  parameters: invalid
        """;

    var ex =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yaml, "events/invalid-parameters.yml"));

    assertTrue(
        ex.getDiagnostics().stream()
            .anyMatch(
                d ->
                    d.code().equals("SE-YAML-011")
                        && d.path()
                            .equals(
                                "events/invalid-parameters.yml:"
                                    + "phases.active.transitions[0].trigger.parameters")));
  }

  @Test
  void testRootNotAnObject() {
    String yamlScalar = "just_a_string";
    var ex1 =
        assertThrows(
            EventDefinitionCompilerException.class, () -> parser.parse(yamlScalar, "scalar.yml"));
    assertTrue(ex1.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-YAML-002")));

    String yamlList = "- item1\n- item2";
    var ex2 =
        assertThrows(
            EventDefinitionCompilerException.class, () -> parser.parse(yamlList, "list.yml"));
    assertTrue(ex2.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-YAML-002")));
  }

  @Test
  void testPhasesValidationErrors() {
    // 1. Phase entry is not an object
    String yamlPhaseNotObj =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1: "not_a_map"
        """;
    var ex1 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlPhaseNotObj, "phase_not_obj.yml"));
    assertTrue(ex1.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-YAML-005")));

    // 2. Phases field is not an object
    String yamlPhasesNotMap =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases: "not_a_map"
        """;
    var ex2 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlPhasesNotMap, "phases_not_map.yml"));
    assertTrue(
        ex2.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("phases must be an object/map")));
  }

  @Test
  void testEncounterAndLimitsNotAMap() {
    String yaml =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1: {}
        encounter: "not_a_map"
        limits: 123
        """;
    var ex =
        assertThrows(
            EventDefinitionCompilerException.class, () -> parser.parse(yaml, "bad_roots.yml"));
    assertTrue(
        ex.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("encounter must be an object/map")));
    assertTrue(
        ex.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("limits must be an object/map")));
  }

  @Test
  void testParametersComprehensiveValidation() {
    // 1. Valid parameters with min, max, step, gui-editable
    String validYaml =
        """
        schema-version: 1
        id: params_test
        initial-phase: p1
        phases:
          p1: {}
        parameters:
          boss-health:
            type: integer
            default: 100
            min: 10
            max: 500
            step: 5
            gui-editable: true
        """;
    EventSpec spec = parser.parse(validYaml, "params_valid.yml");
    assertNotNull(spec.parameters().get("boss-health"));
    assertEquals(new java.math.BigDecimal("10"), spec.parameters().get("boss-health").minimum());
    assertEquals(new java.math.BigDecimal("500"), spec.parameters().get("boss-health").maximum());
    assertEquals(new java.math.BigDecimal("5"), spec.parameters().get("boss-health").step());
    assertTrue(spec.parameters().get("boss-health").guiEditable());

    // 2. Parameter entry not a map
    String yamlNotMap =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1: {}
        parameters:
          bad_param: "string_value"
        """;
    var ex1 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlNotMap, "bad_param.yml"));
    assertTrue(
        ex1.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("parameter declaration must be an object/map")));

    // 3. Parameter missing required type or default
    String yamlMissingType =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1: {}
        parameters:
          p1:
            default: 10
        """;
    var ex2 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlMissingType, "missing_type.yml"));
    assertTrue(
        ex2.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("type and default are required")));

    // 4. Parameter non-numeric min
    String yamlNonNumeric =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1: {}
        parameters:
          p1:
            type: integer
            default: 10
            min: "not_a_number"
        """;
    var ex3 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlNonNumeric, "non_numeric.yml"));
    assertTrue(ex3.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-YAML-014")));

    // 5. Parameters field not a map
    String yamlParamsNotMap =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1: {}
        parameters: "not_a_map"
        """;
    var ex4 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlParamsNotMap, "params_not_map.yml"));
    assertTrue(
        ex4.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("parameters must be an object/map")));
  }

  @Test
  void testTransitionsAndActionsDeepValidation() {
    // 1. Transition element not an object
    String yamlTransitionNotObj =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1:
            transitions:
              - "not_an_object"
        """;
    var ex1 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlTransitionNotObj, "t1.yml"));
    assertTrue(
        ex1.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("Transition must be an object")));

    // 2. Transition missing trigger
    String yamlNoTrigger =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1:
            transitions:
              - target: p2
        """;
    var ex2 =
        assertThrows(
            EventDefinitionCompilerException.class, () -> parser.parse(yamlNoTrigger, "t2.yml"));
    assertTrue(ex2.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-YAML-006")));

    // 3. Condition element not an object, conditions not a list
    String yamlCondNotObj =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1:
            transitions:
              - trigger:
                  type: manual
                target: p2
                conditions:
                  - "not_an_object"
        """;
    var ex3 =
        assertThrows(
            EventDefinitionCompilerException.class, () -> parser.parse(yamlCondNotObj, "c1.yml"));
    assertTrue(
        ex3.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("Condition must be an object")));

    String yamlCondNotList =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1:
            transitions:
              - trigger:
                  type: manual
                target: p2
                conditions: "not_a_list"
        """;
    var ex4 =
        assertThrows(
            EventDefinitionCompilerException.class, () -> parser.parse(yamlCondNotList, "c2.yml"));
    assertTrue(
        ex4.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("conditions must be a list")));

    // 4. Action element not an object, actions not a list
    String yamlActionNotObj =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1:
            transitions:
              - trigger:
                  type: manual
                target: p2
                actions:
                  - "not_an_object"
        """;
    var ex5 =
        assertThrows(
            EventDefinitionCompilerException.class, () -> parser.parse(yamlActionNotObj, "a1.yml"));
    assertTrue(
        ex5.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("Action must be an object")));

    String yamlActionNotList =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1:
            transitions:
              - trigger:
                  type: manual
                target: p2
                actions: "not_a_list"
        """;
    var ex6 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlActionNotList, "a2.yml"));
    assertTrue(
        ex6.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("actions must be a list")));

    // 5. On-enter action not an object
    String yamlOnEnterNotObj =
        """
        schema-version: 1
        id: test
        initial-phase: p1
        phases:
          p1:
            on-enter:
              - "not_an_object"
        """;
    var ex7 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlOnEnterNotObj, "oe1.yml"));
    assertTrue(ex7.getDiagnostics().stream().anyMatch(d -> d.code().equals("SE-YAML-012")));
  }

  @Test
  void testTargetPhaseAliasesAndOnEnterAliases() {
    // Test target-phase, targetPhase, target_phase aliases
    for (String targetKey : List.of("target-phase", "targetPhase", "target_phase")) {
      String template =
          """
          schema-version: 1
          id: alias_test
          initial-phase: p1
          phases:
            p1:
              transitions:
                - trigger:
                    type: manual
                  TARGET_KEY: p2
            p2: {}
          """;
      String yaml = template.replace("TARGET_KEY: p2", targetKey + ": p2");
      EventSpec spec = parser.parse(yaml, "alias.yml");
      assertEquals("p2", spec.phases().get("p1").transitions().get(0).targetPhase());
    }

    // Test on_enter alias
    String yamlOnEnter =
        """
        schema-version: 1
        id: alias_on_enter
        initial-phase: p1
        phases:
          p1:
            on_enter:
              - type: broadcast
                message: "Welcome"
        """;
    EventSpec spec = parser.parse(yamlOnEnter, "on_enter.yml");
    assertEquals(1, spec.phases().get("p1").onEnter().size());
  }

  @Test
  void testMissingTypeInTriggerConditionActionAndExplicitNulls() {
    String yaml =
        """
        schema-version: 1
        id: missing_types
        initial-phase: p1
        phases:
          p1:
            transitions:
              - trigger:
                  foo: bar
                conditions:
                  - foo: bar
                actions:
                  - foo: bar
                target: p1
        """;
    var ex =
        assertThrows(
            EventDefinitionCompilerException.class, () -> parser.parse(yaml, "missing_types.yml"));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> "SE-YAML-007".equals(d.code())));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> "SE-YAML-008".equals(d.code())));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> "SE-YAML-009".equals(d.code())));

    // Explicit null type
    String yamlNullType =
        """
        schema-version: 1
        id: null_type
        initial-phase: p1
        phases:
          p1:
            transitions:
              - trigger:
                  type: null
                target: p1
        """;
    var ex2 =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlNullType, "null_type.yml"));
    assertTrue(ex2.getDiagnostics().stream().anyMatch(d -> "SE-YAML-007".equals(d.code())));
  }

  @Test
  void testOversizedConditionsTriggersLimitDiagnostics() {
    StringBuilder sb = new StringBuilder();
    sb.append(
        """
        schema-version: 1
        id: oversized
        initial-phase: p1
        phases:
          p1:
            transitions:
              - trigger:
                  type: manual
                target: p1
                conditions:
        """);
    for (int i = 0; i < 65; i++) {
      sb.append("          - type: dummy_cond_").append(i).append("\n");
    }
    var ex =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(sb.toString(), "oversized.yml"));
    assertTrue(ex.getDiagnostics().stream().anyMatch(d -> "SE-YAML-013".equals(d.code())));
  }

  @Test
  void testNestedParametersMapAndNonMapParameters() {
    String yaml =
        """
        schema-version: 1
        id: nested_params
        initial-phase: p1
        parameters:
          unrestricted-param:
            type: boolean
            default: false
        phases:
          p1:
            transitions:
              - trigger:
                  type: tick
                  parameters:
                    interval: 20
                  extra: value
                conditions:
                  - type: player_count
                    parameters:
                      min: 1
                actions:
                  - type: broadcast
                    parameters:
                      msg: "hi"
                target: p1
        """;
    EventSpec spec = parser.parse(yaml, "nested_params.yml");
    assertNotNull(spec);
    assertEquals(false, spec.parameters().get("unrestricted-param").defaultValue());
    assertEquals(false, spec.parameters().get("unrestricted-param").guiEditable());

    // Non-map parameters object in trigger
    String yamlBadParams =
        """
        schema-version: 1
        id: bad_params
        initial-phase: p1
        phases:
          p1:
            transitions:
              - trigger:
                  type: tick
                  parameters: "not_a_map"
                target: p1
        """;
    var ex =
        assertThrows(
            EventDefinitionCompilerException.class,
            () -> parser.parse(yamlBadParams, "bad_params.yml"));
    assertTrue(
        ex.getDiagnostics().stream()
            .anyMatch(d -> d.message().contains("parameters must be an object/map")));
  }
}
