package io.github.kizio806.spectraevents.application.config.compiled;

import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;

public record ConfiguredActionDefinition(String type) implements ActionDefinition {
  public ConfiguredActionDefinition {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("type must not be blank");
    }
  }
}
