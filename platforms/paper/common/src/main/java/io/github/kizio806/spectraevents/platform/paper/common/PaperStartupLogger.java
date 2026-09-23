package io.github.kizio806.spectraevents.platform.paper.common;

import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/** Formats administrator-facing Paper startup information through the plugin logger. */
public final class PaperStartupLogger {
  private final Logger logger;

  public PaperStartupLogger(Plugin plugin) {
    this.logger = plugin.getLogger();
  }

  public void printBanner(String version, String apiVersion) {
    line("");
    line("  ____                  _              _____                 _");
    line(" / ___| _ __   ___  ___| |_ _ __ __ _| ____|_   _____ _ __ | |_ ___");
    line(" \\___ \\| '_ \\ / _ \\/ __| __| '__/ _` |  _| \\ \\ / / _ \\ '_ \\| __/ __|");
    line("  ___) | |_) |  __/ (__| |_| | | (_| | |___ \\ V /  __/ | | | | |_\\__ \\");
    line(" |____/| .__/ \\___|\\___|\\__|_|  \\__,_|_____| \\_/ \\___|_| |_|\\__|___/");
    line("       |_|");
    line("");
    line("  Version:   " + version);
    line("  Platform:  Paper " + apiVersion);
    line("  Website:   https://github.com/kizio806/SpectraEvents");
    line("  Modrinth:  https://modrinth.com/plugin/spectraevents");
    line("");
  }

  public void storage(Path database, int loadedInstances) {
    section("Storage");
    line("  SQLite: " + database + " (" + loadedInstances + " persisted instances)");
    line("");
  }

  public void models(int loaded, int invalid) {
    section("Models");
    line("  " + loaded + " model definitions registered, " + invalid + " invalid");
    line("");
  }

  public void definitions(Collection<String> ids, int failures) {
    section("Definitions");
    String definitionList =
        ids.stream().sorted().reduce((left, right) -> left + ", " + right).orElse("none");
    line("  " + ids.size() + " event definitions registered (" + definitionList + ")");
    if (failures > 0) {
      line("  " + failures + " definition files rejected; see WARNING entries above");
    }
    line("");
  }

  public void integrations(IntegrationRegistry registry) {
    section("Integrations");
    registry.getAll().values().stream()
        .sorted(Comparator.comparing(IntegrationRegistry.IntegrationInfo::name))
        .forEach(info -> line("  " + pad(info.name(), 18) + integrationState(info)));
    line("");
  }

  public void checkingForUpdates() {
    section("Update");
    line("  Checking for updates...");
    line("");
  }

  public void enabled(long elapsedMillis) {
    line("SpectraEvents enabled successfully. (" + elapsedMillis + " ms)");
  }

  private void section(String name) {
    line("--- " + name + " ---");
  }

  private String integrationState(IntegrationRegistry.IntegrationInfo info) {
    return switch (info.state()) {
      case ENABLED -> "ACTIVE";
      case DISABLED -> "DISABLED";
      case MISSING -> "NOT INSTALLED";
    };
  }

  private String pad(String value, int length) {
    return String.format("%-" + length + "s", value);
  }

  private void line(String message) {
    logger.info(message);
  }
}
