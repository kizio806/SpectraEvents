package io.github.kizio806.spectraevents.application.config.compiled;

import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;

public record ConfiguredConditionDefinition(String type) implements ConditionDefinition {
  public ConfiguredConditionDefinition {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("type must not be blank");
    }
  }
}
