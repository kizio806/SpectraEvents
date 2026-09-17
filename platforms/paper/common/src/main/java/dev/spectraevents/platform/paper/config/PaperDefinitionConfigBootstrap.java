package dev.spectraevents.platform.paper.config;

import dev.spectraevents.application.config.loader.DefinitionLoadResult;
import dev.spectraevents.application.config.loader.FileSystemDefinitionLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/** Discovers YAML event definitions on disk and loads them through the application pipeline. */
public final class PaperDefinitionConfigBootstrap {
  private static final Logger LOGGER =
      Logger.getLogger(PaperDefinitionConfigBootstrap.class.getName());

  private final JavaPlugin plugin;
  private final FileSystemDefinitionLoader fileSystemLoader;

  public PaperDefinitionConfigBootstrap(
      JavaPlugin plugin,
      dev.spectraevents.application.config.loader.DefinitionLoader definitionLoader) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.fileSystemLoader =
        new FileSystemDefinitionLoader(plugin.getDataFolder().toPath(), definitionLoader);
  }

  public Path eventsDirectory() {
    return fileSystemLoader.eventsDirectory();
  }

  /** Ensures the events directory exists and contains default configurations. */
  public void ensureDefaultConfiguration() throws IOException {
    fileSystemLoader.ensureDefaultConfiguration();
  }

  /** Loads all YAML files from the events directory into the registry. */
  public DefinitionLoadResult loadFromDisk() throws IOException {
    return fileSystemLoader.loadFromDisk();
  }

  /** Clears the registry and reloads all YAML files from disk. */
  public DefinitionLoadResult reloadFromDisk() throws IOException {
    return fileSystemLoader.reloadFromDisk();
  }

  public void logLoadResult(DefinitionLoadResult result) {
    for (var loaded : result.loaded()) {
      LOGGER.info(
          "Loaded event definition '"
              + loaded.definition().id().value()
              + "' from "
              + loaded.sourceFile());
    }
    for (var failure : result.failures()) {
      LOGGER.log(
          Level.WARNING,
          () ->
              "Failed to load "
                  + failure.sourceFile()
                  + ": "
                  + formatDiagnostics(failure.diagnostics()));
    }
  }

  private String formatDiagnostics(
      List<dev.spectraevents.application.config.validation.ValidationDiagnostic> diagnostics) {
    return diagnostics.stream()
        .map(d -> d.code() + " @ " + d.path() + ": " + d.message())
        .reduce((a, b) -> a + "; " + b)
        .orElse("unknown error");
  }
}
