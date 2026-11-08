package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.template.SpectraBundleInstaller;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.bukkit.util.BukkitPluginVersions;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/** Handles `/spectraevents template` subcommands on Spigot. */
public final class SpigotTemplateCommandHandler implements SpigotSubcommandHandler {

  private final Plugin plugin;
  private final SpectraEventsApplication application;
  private final BukkitDefinitionConfigBootstrap definitions;
  private final SpigotCommandHelper helper;

  public SpigotTemplateCommandHandler(
      Plugin plugin,
      SpectraEventsApplication application,
      BukkitDefinitionConfigBootstrap definitions,
      SpigotCommandHelper helper) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.application = Objects.requireNonNull(application, "application");
    this.definitions = Objects.requireNonNull(definitions, "definitions");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public void execute(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.template");
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      sender.sendMessage("Bundled templates: metin");
      return;
    }
    if (arguments.length != 2 || !"install".equalsIgnoreCase(arguments[0])) {
      throw new IllegalArgumentException("Usage: /spectraevents template <list|install metin>");
    }
    if (!"metin".equalsIgnoreCase(arguments[1])) {
      throw new IllegalArgumentException("Unknown bundled template: " + arguments[1]);
    }
    try {
      Path archive =
          plugin.getDataFolder().toPath().resolve("templates").resolve("metin.spectra.zip");
      String version = BukkitPluginVersions.resolveVersion(plugin);
      new SpectraBundleInstaller(plugin.getDataFolder().toPath(), version).install(archive);
      application.assetPipelineService().buildAssets();
      DefinitionLoadResult result = definitions.reloadFromDisk();
      definitions.logLoadResult(result);
      if (!result.failures().isEmpty()) {
        throw new IllegalStateException(
            "Installed template did not validate: " + result.failures());
      }
      sender.sendMessage("Template metin is installed and validated.");
    } catch (IOException exception) {
      throw new IllegalStateException("Could not install template metin", exception);
    }
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] arguments) {
    if (sender == null || !sender.hasPermission("spectraevents.template")) {
      return List.of();
    }
    if (arguments.length == 1) {
      return helper.matches(arguments[0], "list", "install");
    }
    if (arguments.length == 2 && "install".equalsIgnoreCase(arguments[0])) {
      return helper.matches(arguments[1], "metin");
    }
    return List.of();
  }
}
