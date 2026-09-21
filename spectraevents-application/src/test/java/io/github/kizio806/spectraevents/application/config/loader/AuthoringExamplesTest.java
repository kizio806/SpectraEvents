package io.github.kizio806.spectraevents.application.config.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthoringExamplesTest {
  private static final List<EventExample> COMPLETE_EXAMPLES =
      List.of(
          new EventExample("meteor", "meteor"),
          new EventExample("airdrop", "airdrop"),
          new EventExample("metin", "metin"),
          new EventExample("pinata", "pinata"),
          new EventExample("boss-portal", "boss_portal"));

  @Test
  void shippedExamplesLoadWithTheSchemaV1Contract() throws IOException {
    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    DefinitionLoader loader =
        new DefinitionLoader(new EventSpecYamlParser(), new EventDefinitionCompiler(), registry);

    Map<String, String> sources = new LinkedHashMap<>();
    for (EventExample example : COMPLETE_EXAMPLES) {
      sources.put(
          "events/" + example.fileName() + ".yml",
          readResource("/events/" + example.fileName() + ".yml"));
    }

    DefinitionLoadResult result = loader.load(sources);

    assertEquals(5, result.loaded().size(), result.failures()::toString);
    assertTrue(result.failures().isEmpty(), result.failures()::toString);
    for (EventExample example : COMPLETE_EXAMPLES) {
      var registered = registry.get(new EventDefinitionId(example.definitionId())).orElseThrow();
      assertTrue(
          allActions(registered.definition()).stream()
              .anyMatch(action -> "play_animation".equals(action.type())));
    }
  }

  private String readResource(String resource) throws IOException {
    try (InputStream input = getClass().getResourceAsStream(resource)) {
      if (input == null) {
        throw new IOException("Missing test resource " + resource);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private List<ActionDefinition> allActions(
      io.github.kizio806.spectraevents.core.event.definition.EventDefinition definition) {
    return definition.phaseIds().stream()
        .map(definition::phase)
        .flatMap(java.util.Optional::stream)
        .flatMap(phase -> phase.onEnterActions().stream())
        .toList();
  }

  private record EventExample(String fileName, String definitionId) {}
}
