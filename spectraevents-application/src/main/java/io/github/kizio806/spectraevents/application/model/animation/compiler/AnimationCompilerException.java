package io.github.kizio806.spectraevents.application.model.animation.compiler;

import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import java.util.List;

/** Exception thrown when animation compilation fails diagnostic checks. */
public class AnimationCompilerException extends RuntimeException {
  private final List<ValidationDiagnostic> diagnostics;

  public AnimationCompilerException(String message, List<ValidationDiagnostic> diagnostics) {
    super(message + " (" + (diagnostics != null ? diagnostics.size() : 0) + " diagnostics)");
    this.diagnostics = diagnostics != null ? List.copyOf(diagnostics) : List.of();
  }

  public List<ValidationDiagnostic> diagnostics() {
    return diagnostics;
  }
}
