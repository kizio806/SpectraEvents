package io.github.kizio806.spectraevents.application.config.compiled;

import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;
import java.util.Map;

public record ConfiguredConditionDefinition(String type, Map<String, Object> parameters)
    implements ConditionDefinition {
  public ConfiguredConditionDefinition(String type) {
    this(type, Map.of());
  }

  public ConfiguredConditionDefinition {
    parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
  }
}
