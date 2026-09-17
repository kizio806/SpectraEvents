package io.github.kizio806.spectraevents.application.config.registry;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import java.util.Objects;

/** A compiled event definition together with its authoring source metadata. */
public record RegisteredEventDefinition(EventDefinition definition, String sourceFile) {
  public RegisteredEventDefinition {
    Objects.requireNonNull(definition, "definition");
    Objects.requireNonNull(sourceFile, "sourceFile");
    if (sourceFile.isBlank()) {
      throw new IllegalArgumentException("sourceFile cannot be blank");
    }
  }
}
