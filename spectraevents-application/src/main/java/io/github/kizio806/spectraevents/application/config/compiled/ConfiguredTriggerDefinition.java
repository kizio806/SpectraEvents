package io.github.kizio806.spectraevents.application.config.compiled;

import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;

public record ConfiguredTriggerDefinition(String type) implements TriggerDefinition {
  public ConfiguredTriggerDefinition {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("type must not be blank");
    }
  }
}
