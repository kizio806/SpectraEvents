package io.github.kizio806.spectraevents.application.config.compiler;

import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import java.util.List;

/** Thrown when an event definition fails to compile due to validation errors. */
public class EventDefinitionCompilerException extends RuntimeException {
  private final List<ValidationDiagnostic> diagnostics;

  public EventDefinitionCompilerException(String message, List<ValidationDiagnostic> diagnostics) {
    super(message);
    this.diagnostics = List.copyOf(diagnostics);
  }

  public List<ValidationDiagnostic> getDiagnostics() {
    return diagnostics;
  }
}
