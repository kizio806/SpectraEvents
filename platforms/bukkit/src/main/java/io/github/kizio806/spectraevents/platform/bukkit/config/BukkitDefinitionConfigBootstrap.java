package io.github.kizio806.spectraevents.platform.bukkit.config;

import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.loader.FileSystemDefinitionLoader;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

/**
 * Discovers YAML event definitions on disk and loads them through the shared application pipeline
 * for Bukkit-based platforms (Paper, Folia, Spigot).
 */
public class BukkitDefinitionConfigBootstrap {

  private final Plugin plugin;
  private final FileSystemDefinitionLoader fileSystemLoader;

  public BukkitDefinitionConfigBootstrap(Plugin plugin, DefinitionLoader definitionLoader) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(definitionLoader, "definitionLoader");
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

  /** Validates all YAML files on disk without committing changes to the registry. */
  public DefinitionLoadResult validateFromDisk() throws IOException {
    return fileSystemLoader.validateFromDisk();
  }

  public FileSystemDefinitionLoader fileSystemLoader() {
    return fileSystemLoader;
  }

  public void logLoadResult(DefinitionLoadResult result) {
    for (var loaded : result.loaded()) {
      plugin
          .getLogger()
          .info(
              "Loaded event definition '"
                  + loaded.definition().id().value()
                  + "' from "
                  + loaded.sourceFile());
    }
    for (var failure : result.failures()) {
      plugin
          .getLogger()
          .log(
              Level.WARNING,
              "Failed to load "
                  + failure.sourceFile()
                  + ": "
                  + formatDiagnostics(failure.diagnostics()));
    }
  }

  public String formatDiagnostics(List<ValidationDiagnostic> diagnostics) {
    return diagnostics.stream()
        .map(
            diagnostic ->
                diagnostic.code() + " @ " + diagnostic.path() + ": " + diagnostic.message())
        .reduce((left, right) -> left + "; " + right)
        .orElse("unknown error");
  }
}
