package dev.spectraevents.application.config.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.spectraevents.application.config.compiler.EventDefinitionCompiler;
import dev.spectraevents.application.config.registry.EventDefinitionRegistry;
import dev.spectraevents.application.config.yaml.EventSpecYamlParser;
import dev.spectraevents.application.repository.InMemoryEventInstanceRepository;
import dev.spectraevents.application.service.EventOrchestrationService;
import dev.spectraevents.core.event.runtime.EventInstance;
import dev.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefinitionLoadPipelineE2ETest {
  private static final String EXAMPLE_YAML =
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
        completed:
      """;

  @Test
  void loadCompileRegisterAndStartWithManualTransitions() {
    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    DefinitionLoader loader =
        new DefinitionLoader(new EventSpecYamlParser(), new EventDefinitionCompiler(), registry);

    DefinitionLoadResult result = loader.load(Map.of("events/example.yml", EXAMPLE_YAML));
    assertEquals(1, result.loaded().size());

    EventOrchestrationService orchestrationService =
        new EventOrchestrationService(new InMemoryEventInstanceRepository(), registry);

    EventInstance instance = orchestrationService.startDefinition("example");
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals("waiting", instance.currentPhase().orElseThrow().value());

    EventInstance active = orchestrationService.transitionPhase(instance.id().toString());
    assertEquals("active", active.currentPhase().orElseThrow().value());

    EventInstance completed = orchestrationService.transitionPhase(instance.id().toString());
    assertEquals("completed", completed.currentPhase().orElseThrow().value());
  }
}
