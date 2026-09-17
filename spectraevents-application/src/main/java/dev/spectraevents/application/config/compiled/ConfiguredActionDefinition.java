package dev.spectraevents.application.config.compiled;

import dev.spectraevents.core.event.execution.action.ActionDefinition;
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
