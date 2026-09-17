package dev.spectraevents.application.config.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.spectraevents.application.config.compiler.EventDefinitionCompilerException;
import dev.spectraevents.application.config.spec.EventSpec;
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
}
