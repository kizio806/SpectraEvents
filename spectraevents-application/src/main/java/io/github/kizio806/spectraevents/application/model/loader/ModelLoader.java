package io.github.kizio806.spectraevents.application.model.loader;

import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic.Severity;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompilerException;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.spec.ModelSpec;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/** Loads, parses, compiles, and registers 3D model YAML definition files from disk. */
public class ModelLoader {
  private final ModelCompiler compiler;
  private final ModelDefinitionRegistry registry;

  public ModelLoader(ModelCompiler compiler, ModelDefinitionRegistry registry) {
    this.compiler = Objects.requireNonNull(compiler, "compiler cannot be null");
    this.registry = Objects.requireNonNull(registry, "registry cannot be null");
  }

  /** Loads all *.yml model files from the specified directory. */
  public ModelLoaderResult loadDirectory(Path directoryPath) {
    Objects.requireNonNull(directoryPath, "directoryPath cannot be null");

    List<ValidationDiagnostic> diagnostics = new ArrayList<>();
    int loaded = 0;
    int invalid = 0;

    if (!Files.exists(directoryPath)) {
      try {
        Files.createDirectories(directoryPath);
      } catch (Exception e) {
        diagnostics.add(
            new ValidationDiagnostic(
                Severity.ERROR,
                "MODEL_DIR_CREATE_FAILED",
                directoryPath.toString(),
                "Failed to create models directory: " + e.getMessage()));
        return new ModelLoaderResult(0, 1, diagnostics);
      }
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.yml")) {
      for (Path file : stream) {
        try {
          ModelDefinition compiled = parseAndCompile(file);
          if (registry.contains(compiled.id())) {
            diagnostics.add(
                new ValidationDiagnostic(
                    Severity.ERROR,
                    "DUPLICATE_MODEL_ID",
                    file.getFileName().toString(),
                    "Duplicate model ID '"
                        + compiled.id().value()
                        + "' found in file "
                        + file.getFileName()));
            invalid++;
          } else {
            registry.register(compiled);
            loaded++;
          }
        } catch (Exception e) {
          invalid++;
          if (e instanceof ModelCompilerException mce) {
            diagnostics.addAll(mce.diagnostics());
          } else {
            diagnostics.add(
                new ValidationDiagnostic(
                    Severity.ERROR,
                    "MODEL_PARSE_FAILED",
                    file.getFileName().toString(),
                    "Failed to parse model file: " + e.getMessage()));
          }
        }
      }
    } catch (Exception e) {
      diagnostics.add(
          new ValidationDiagnostic(
              Severity.ERROR,
              "MODEL_DIR_READ_FAILED",
              directoryPath.toString(),
              "Failed to read models directory: " + e.getMessage()));
    }

    return new ModelLoaderResult(loaded, invalid, diagnostics);
  }

  /** Parses and compiles a single YAML model stream. */
  public ModelDefinition parseAndCompile(InputStream inputStream, String fileName) {
    Objects.requireNonNull(inputStream, "inputStream cannot be null");
    try {
      LoaderOptions options = new LoaderOptions();
      Yaml yaml = new Yaml(new Constructor(ModelSpec.class, options));
      ModelSpec spec = yaml.load(inputStream);
      if (spec == null) {
        throw new IllegalArgumentException("Model YAML stream is empty");
      }
      return compiler.compile(spec);
    } catch (Exception e) {
      if (e instanceof ModelCompilerException) {
        throw (ModelCompilerException) e;
      }
      List<ValidationDiagnostic> diagnostics =
          List.of(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "YAML_SYNTAX_ERROR",
                  fileName,
                  "YAML syntax error: " + e.getMessage()));
      throw new ModelCompilerException("YAML parsing failed for " + fileName, diagnostics);
    }
  }

  public ModelDefinition parseAndCompile(Path file) {
    try (InputStream is = Files.newInputStream(file)) {
      return parseAndCompile(is, file.getFileName().toString());
    } catch (Exception e) {
      if (e instanceof ModelCompilerException) {
        throw (ModelCompilerException) e;
      }
      List<ValidationDiagnostic> diagnostics =
          List.of(
              new ValidationDiagnostic(
                  Severity.ERROR,
                  "FILE_READ_ERROR",
                  file.getFileName().toString(),
                  "Failed to read file: " + e.getMessage()));
      throw new ModelCompilerException("File read failed for " + file, diagnostics);
    }
  }

  public record ModelLoaderResult(
      int loadedCount, int invalidCount, List<ValidationDiagnostic> diagnostics) {}
}
