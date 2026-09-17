package io.github.kizio806.spectraevents.application.model.compiler;

import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import java.util.List;
import java.util.Objects;

/** Exception thrown when 3D model compilation fails validation. */
public class ModelCompilerException extends RuntimeException {
  private final List<ValidationDiagnostic> diagnostics;

  public ModelCompilerException(String message, List<ValidationDiagnostic> diagnostics) {
    super(message + ": " + diagnostics);
    this.diagnostics =
        List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics cannot be null"));
  }

  public List<ValidationDiagnostic> diagnostics() {
    return diagnostics;
  }
}
