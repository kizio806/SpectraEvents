package io.github.kizio806.spectraevents.application.config.validation;

import java.util.Objects;

/**
 * Represents a structured diagnostic message emitted during configuration validation or
 * compilation.
 */
public record ValidationDiagnostic(Severity severity, String code, String path, String message) {
  public ValidationDiagnostic {
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(message, "message");
  }

  public enum Severity {
    INFO,
    WARNING,
    ERROR
  }
}
