package dev.spectraevents.application.config.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.spectraevents.application.config.compiler.EventDefinitionCompiler;
import dev.spectraevents.application.config.registry.EventDefinitionRegistry;
import dev.spectraevents.application.config.yaml.EventSpecYamlParser;
import dev.spectraevents.core.event.definition.EventDefinitionId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefinitionLoaderTest {
  private EventDefinitionRegistry registry;
  private DefinitionLoader loader;

  private static final String VALID_YAML =
      """
      schema-version: 1
      id: valid
      initial-phase: waiting
      phases:
        waiting:
          transitions:
            - trigger:
                type: manual
              target: active
        active:
      """;

  @BeforeEach
  void setUp() {
    registry = new EventDefinitionRegistry();
    loader =
        new DefinitionLoader(new EventSpecYamlParser(), new EventDefinitionCompiler(), registry);
  }

  @Test
  void loadsValidFiles() {
    DefinitionLoadResult result = loader.load(Map.of("events/valid.yml", VALID_YAML));

    assertEquals(1, result.loaded().size());
    assertTrue(result.failures().isEmpty());
    assertTrue(registry.get(new EventDefinitionId("valid")).isPresent());
  }

  @Test
  void isolatesBrokenFiles() {
    LinkedHashMap<String, String> sources = new LinkedHashMap<>();
    sources.put("events/broken.yml", "schema-version: 1\nid: [broken");
    sources.put("events/valid.yml", VALID_YAML);

    DefinitionLoadResult result = loader.load(sources);

    assertEquals(1, result.loaded().size());
    assertEquals(1, result.failures().size());
    assertEquals("events/broken.yml", result.failures().get(0).sourceFile());
    assertTrue(
        result.failures().get(0).diagnostics().stream()
            .anyMatch(d -> d.code().equals("SE-YAML-001")));
    assertTrue(registry.get(new EventDefinitionId("valid")).isPresent());
  }

  @Test
  void reportsDuplicateIdsWithoutBlockingOtherFiles() {
    String duplicateYaml =
        """
        schema-version: 1
        id: valid
        initial-phase: waiting
        phases:
          waiting:
        """;

    LinkedHashMap<String, String> sources = new LinkedHashMap<>();
    sources.put("events/first.yml", VALID_YAML);
    sources.put("events/duplicate.yml", duplicateYaml);

    DefinitionLoadResult result = loader.load(sources);

    assertEquals(1, result.loaded().size());
    assertEquals(1, result.failures().size());
    assertTrue(
        result.failures().get(0).diagnostics().stream()
            .anyMatch(d -> d.code().equals("SE-REG-001")));
  }

  @Test
  void reloadPreservesExistingValidDefinitionWhenReloadFails() {
    loader.load(Map.of("events/valid.yml", VALID_YAML));
    assertEquals(1, registry.getAll().size());

    // User edits valid.yml and introduces invalid syntax
    DefinitionLoadResult result =
        loader.reload(Map.of("events/valid.yml", "schema-version: 1\nid: [invalid syntax"));

    assertEquals(1, result.failures().size());
    assertTrue(
        registry.get(new EventDefinitionId("valid")).isPresent(),
        "Old valid definition must remain active when reload fails");
  }
}
