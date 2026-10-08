package io.github.kizio806.spectraevents.application.config.loader;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompilerException;
import io.github.kizio806.spectraevents.application.config.registry.DuplicateEventDefinitionException;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Parses, compiles, and registers event definitions from YAML source files. */
public final class DefinitionLoader {
  private final EventSpecYamlParser parser;
  private final EventDefinitionCompiler compiler;
  private final EventDefinitionRegistry registry;

  public DefinitionLoader(
      EventSpecYamlParser parser,
      EventDefinitionCompiler compiler,
      EventDefinitionRegistry registry) {
    this.parser = Objects.requireNonNull(parser, "parser");
    this.compiler = Objects.requireNonNull(compiler, "compiler");
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  /**
   * Loads definitions from the provided sources into the registry. Failures are isolated per file.
   *
   * @param sources map of source file path/name to YAML content
   * @return summary of loaded definitions and per-file failures
   */
  public DefinitionLoadResult load(Map<String, String> sources) {
    Objects.requireNonNull(sources, "sources");

    List<RegisteredEventDefinition> loaded = new ArrayList<>();
    List<DefinitionLoadResult.SourceFileFailure> failures = new ArrayList<>();

    for (Map.Entry<String, String> entry : sources.entrySet()) {
      String sourceFile = entry.getKey();
      String content = entry.getValue();

      try {
        EventSpec spec = parser.parse(content, sourceFile);
        EventDefinition definition = compiler.compile(spec);
        registry.register(definition, sourceFile, spec);
        loaded.add(new RegisteredEventDefinition(definition, sourceFile, spec));
      } catch (EventDefinitionCompilerException ex) {
        failures.add(
            new DefinitionLoadResult.SourceFileFailure(
                sourceFile, qualifyDiagnostics(sourceFile, ex.getDiagnostics())));
      } catch (DuplicateEventDefinitionException ex) {
        failures.add(
            new DefinitionLoadResult.SourceFileFailure(
                sourceFile, qualifyDiagnostics(sourceFile, ex.diagnostics())));
      }
    }

    return new DefinitionLoadResult(loaded, failures);
  }

  /**
   * Reloads provided sources transactionally. If a source fails to compile, its previous valid
   * definition in the registry remains intact.
   *
   * @param sources map of source file path/name to YAML content
   * @return summary of reloaded definitions and per-file failures
   */
  public DefinitionLoadResult reload(Map<String, String> sources) {
    Objects.requireNonNull(sources, "sources");

    List<RegisteredEventDefinition> loaded = new ArrayList<>();
    List<DefinitionLoadResult.SourceFileFailure> failures = new ArrayList<>();

    for (Map.Entry<String, String> entry : sources.entrySet()) {
      String sourceFile = entry.getKey();
      String content = entry.getValue();

      try {
        EventSpec spec = parser.parse(content, sourceFile);
        EventDefinition definition = compiler.compile(spec);
        registry.registerOrUpdate(definition, sourceFile, spec);
        loaded.add(new RegisteredEventDefinition(definition, sourceFile, spec));
      } catch (EventDefinitionCompilerException ex) {
        failures.add(
            new DefinitionLoadResult.SourceFileFailure(
                sourceFile, qualifyDiagnostics(sourceFile, ex.getDiagnostics())));
      } catch (DuplicateEventDefinitionException ex) {
        failures.add(
            new DefinitionLoadResult.SourceFileFailure(
                sourceFile, qualifyDiagnostics(sourceFile, ex.diagnostics())));
      }
    }

    registry.retainSources(sources.keySet());

    return new DefinitionLoadResult(loaded, failures);
  }

  /** Parses and compiles sources against an isolated registry without mutating live definitions. */
  public DefinitionLoadResult validate(Map<String, String> sources) {
    Objects.requireNonNull(sources, "sources");
    DefinitionLoader isolated =
        new DefinitionLoader(parser, compiler, new EventDefinitionRegistry());
    return isolated.load(sources);
  }

  /**
   * Reads source files from a map while preserving declaration order.
   *
   * @param orderedSources insertion-ordered map of source file to YAML content
   * @return load result
   */
  public DefinitionLoadResult loadOrdered(java.util.SequencedMap<String, String> orderedSources) {
    return load(orderedSources);
  }

  public EventDefinitionRegistry registry() {
    return registry;
  }

  private List<ValidationDiagnostic> qualifyDiagnostics(
      String sourceFile, List<ValidationDiagnostic> diagnostics) {
    String prefix = sourceFile + ":";
    return diagnostics.stream()
        .map(
            diagnostic -> {
              String path = diagnostic.path();
              if (path.startsWith(prefix)) {
                return diagnostic;
              }
              return new ValidationDiagnostic(
                  diagnostic.severity(), diagnostic.code(), prefix + path, diagnostic.message());
            })
        .toList();
  }
}
