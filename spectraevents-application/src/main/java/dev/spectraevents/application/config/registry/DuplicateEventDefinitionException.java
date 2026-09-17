package dev.spectraevents.application.config.registry;

import dev.spectraevents.application.config.validation.ValidationDiagnostic;
import dev.spectraevents.core.event.definition.EventDefinitionId;
import java.util.List;

/** Thrown when registering an event definition whose ID is already present. */
public final class DuplicateEventDefinitionException extends RuntimeException {
  private final EventDefinitionId definitionId;
  private final List<ValidationDiagnostic> diagnostics;

  public DuplicateEventDefinitionException(
      EventDefinitionId definitionId, String existingSource, String attemptedSource) {
    super(
        "Event definition '"
            + definitionId.value()
            + "' is already registered from "
            + existingSource);
    this.definitionId = definitionId;
    this.diagnostics =
        List.of(
            new ValidationDiagnostic(
                ValidationDiagnostic.Severity.ERROR,
                "SE-REG-001",
                "id",
                "Duplicate event definition '"
                    + definitionId.value()
                    + "': already loaded from "
                    + existingSource
                    + ", attempted again from "
                    + attemptedSource));
  }

  public EventDefinitionId definitionId() {
    return definitionId;
  }

  public List<ValidationDiagnostic> diagnostics() {
    return diagnostics;
  }
}
