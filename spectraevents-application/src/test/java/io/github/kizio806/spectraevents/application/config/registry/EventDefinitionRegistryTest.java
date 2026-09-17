package io.github.kizio806.spectraevents.application.config.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventDefinitionRegistryTest {
  private final EventDefinitionRegistry registry = new EventDefinitionRegistry();

  @Test
  void registersAndRetrievesDefinition() {
    EventDefinition definition = sampleDefinition("example");
    registry.register(definition, "events/example.yml");

    RegisteredEventDefinition registered =
        registry.get(new EventDefinitionId("example")).orElseThrow();
    assertEquals("example", registered.definition().id().value());
    assertEquals("events/example.yml", registered.sourceFile());
  }

  @Test
  void rejectsDuplicateIds() {
    registry.register(sampleDefinition("example"), "events/a.yml");

    DuplicateEventDefinitionException ex =
        assertThrows(
            DuplicateEventDefinitionException.class,
            () -> registry.register(sampleDefinition("example"), "events/b.yml"));

    assertEquals("example", ex.definitionId().value());
    assertTrue(ex.diagnostics().stream().anyMatch(d -> d.code().equals("SE-REG-001")));
  }

  @Test
  void clearRemovesAllDefinitions() {
    registry.register(sampleDefinition("one"), "events/one.yml");
    registry.register(sampleDefinition("two"), "events/two.yml");
    assertEquals(2, registry.getAll().size());

    registry.clear();
    assertTrue(registry.isEmpty());
  }

  private EventDefinition sampleDefinition(String id) {
    PhaseId phase = new PhaseId("waiting");
    return new EventDefinition(
        new EventDefinitionId(id), phase, Map.of(phase, new PhaseDefinition(phase, Set.of())));
  }
}
