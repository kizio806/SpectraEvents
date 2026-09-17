package io.github.kizio806.spectraevents.application.config.compiled;

import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;
import java.util.Map;

public record ConfiguredTriggerDefinition(String type, Map<String, Object> parameters)
    implements TriggerDefinition {
  public ConfiguredTriggerDefinition(String type) {
    this(type, Map.of());
  }

  public ConfiguredTriggerDefinition {
    parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
  }
}
