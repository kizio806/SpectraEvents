package io.github.kizio806.spectraevents.platform.spigot.config;

import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.loader.FileSystemDefinitionLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/** Loads Spigot event definitions through the shared strict application pipeline. */
public final class SpigotDefinitionConfigBootstrap {
  private final JavaPlugin plugin;
  private final FileSystemDefinitionLoader fileSystemLoader;

  public SpigotDefinitionConfigBootstrap(JavaPlugin plugin, DefinitionLoader definitionLoader) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.fileSystemLoader =
        new FileSystemDefinitionLoader(plugin.getDataFolder().toPath(), definitionLoader);
  }

  public Path eventsDirectory() {
    return fileSystemLoader.eventsDirectory();
  }

  public DefinitionLoadResult loadFromDisk() throws IOException {
    return fileSystemLoader.loadFromDisk();
  }

  public DefinitionLoadResult reloadFromDisk() throws IOException {
    return fileSystemLoader.reloadFromDisk();
  }

  public DefinitionLoadResult validateFromDisk() throws IOException {
    return fileSystemLoader.validateFromDisk();
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

  public String formatDiagnostics(
      List<io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic>
          diagnostics) {
    return diagnostics.stream()
        .map(
            diagnostic ->
                diagnostic.code() + " @ " + diagnostic.path() + ": " + diagnostic.message())
        .reduce((left, right) -> left + "; " + right)
        .orElse("unknown error");
  }
}
