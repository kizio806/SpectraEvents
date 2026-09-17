package io.github.kizio806.spectraevents.application.config.loader;

import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import java.util.List;
import java.util.Objects;

/** Summary of a definition load attempt. */
public record DefinitionLoadResult(
    List<RegisteredEventDefinition> loaded, List<SourceFileFailure> failures) {
  public DefinitionLoadResult {
    Objects.requireNonNull(loaded, "loaded");
    Objects.requireNonNull(failures, "failures");
    loaded = List.copyOf(loaded);
    failures = List.copyOf(failures);
  }

  /** A single source file that failed to parse, compile, or register. */
  public record SourceFileFailure(String sourceFile, List<ValidationDiagnostic> diagnostics) {
    public SourceFileFailure {
      Objects.requireNonNull(sourceFile, "sourceFile");
      Objects.requireNonNull(diagnostics, "diagnostics");
      if (sourceFile.isBlank()) {
        throw new IllegalArgumentException("sourceFile cannot be blank");
      }
      diagnostics = List.copyOf(diagnostics);
    }
  }
}
