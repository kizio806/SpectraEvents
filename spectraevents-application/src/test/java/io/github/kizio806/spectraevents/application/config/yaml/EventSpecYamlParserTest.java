package io.github.kizio806.spectraevents.application.config.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompilerException;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
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
}
