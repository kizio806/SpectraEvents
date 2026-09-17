package io.github.kizio806.spectraevents.application.config.compiled;

import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import java.util.Map;

public record ConfiguredActionDefinition(String type, Map<String, Object> parameters)
    implements ActionDefinition {
  public ConfiguredActionDefinition(String type) {
    this(type, Map.of());
  }

  public ConfiguredActionDefinition {
    parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
  }
}
