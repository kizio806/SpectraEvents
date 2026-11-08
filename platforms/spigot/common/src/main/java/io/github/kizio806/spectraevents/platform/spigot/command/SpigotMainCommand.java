package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import io.github.kizio806.spectraevents.platform.bukkit.gui.AdminGuiController;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Classic Bukkit command adapter dispatching to modular sub-command handlers. */
public final class SpigotMainCommand implements CommandExecutor, TabCompleter {

  private final Plugin plugin;
  private final SpigotCommandHelper helper;
  private final Map<String, SpigotSubcommandHandler> subcommands = new HashMap<>();
  private final SpigotDiagnosticsCommandHandler diagnosticsHandler;
  private final SpigotDefinitionCommandHandler definitionHandler;
  private final AdminGuiController guiController;

  public SpigotMainCommand(
      Plugin plugin,
      SpectraEventsApplication application,
      EventInstanceRepository repository,
      BukkitDefinitionConfigBootstrap definitions,
      SpigotActionAdapter actionAdapter,
      BukkitEventSettingsStore settingsStore,
      AdminGuiController guiController,
      UpdateService updateService,
      LocaleCatalog locales) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.helper = new SpigotCommandHelper(locales);
    this.guiController = guiController;

    Path schedulesFile = plugin.getDataFolder().toPath().resolve("schedules.yml");

    this.diagnosticsHandler =
        new SpigotDiagnosticsCommandHandler(plugin, application, repository, helper);
    this.definitionHandler =
        new SpigotDefinitionCommandHandler(plugin, application, definitions, helper);

    subcommands.put(
        "event", new SpigotEventCommandHandler(plugin, application, repository, helper));
    subcommands.put("definition", definitionHandler);
    subcommands.put(
        "template", new SpigotTemplateCommandHandler(plugin, application, definitions, helper));
    subcommands.put(
        "schedule", new SpigotScheduleCommandHandler(application, schedulesFile, helper));
    subcommands.put(
        "rewards", new SpigotRewardsCommandHandler(plugin, application, actionAdapter, helper));

    if (settingsStore != null) {
      subcommands.put("location", new SpigotLocationCommandHandler(settingsStore, helper));
    }
    if (application.modelDefinitionRegistry() != null) {
      subcommands.put(
          "model", new SpigotModelCommandHandler(application.modelDefinitionRegistry(), helper));
    }
    if (application.assetPipelineService() != null) {
      SpigotAssetCommandHandler assetHandler =
          new SpigotAssetCommandHandler(application.assetPipelineService(), helper);
      subcommands.put("asset", assetHandler);
      subcommands.put("assets", assetHandler);
    }
    if (application.integrationRegistry() != null) {
      SpigotIntegrationCommandHandler integrationHandler =
          new SpigotIntegrationCommandHandler(application.integrationRegistry(), helper);
      subcommands.put("integration", integrationHandler);
      subcommands.put("integrations", integrationHandler);
    }
    if (updateService != null) {
      subcommands.put("update", new SpigotUpdateCommandHandler(updateService, helper));
    }
  }

  public SpigotMainCommand(
      Plugin plugin,
      SpectraEventsApplication application,
      EventInstanceRepository repository,
      BukkitDefinitionConfigBootstrap definitions,
      SpigotActionAdapter actionAdapter,
      LocaleCatalog locales) {
    this(plugin, application, repository, definitions, actionAdapter, null, null, null, locales);
  }

  @Override
  public boolean onCommand(
      CommandSender sender, Command command, String label, String[] arguments) {
    if (arguments.length == 0 || "help".equalsIgnoreCase(arguments[0])) {
      help(sender);
      return true;
    }
    String sub = arguments[0].toLowerCase(Locale.ROOT);
    try {
      switch (sub) {
        case "version" -> diagnosticsHandler.version(sender);
        case "status" -> diagnosticsHandler.status(sender);
        case "doctor" -> diagnosticsHandler.doctor(sender);
        case "validate" -> definitionHandler.reloadOrValidate(sender, true);
        case "admin" -> openAdmin(sender);
        default -> {
          SpigotSubcommandHandler handler = subcommands.get(sub);
          if (handler != null) {
            String[] subArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
            handler.execute(sender, subArgs);
          } else {
            help(sender);
            return false;
          }
        }
      }
      return true;
    } catch (IllegalArgumentException exception) {
      helper.send(sender, "command.spigot.failed", Map.of("reason", exception.getMessage()));
      return false;
    } catch (RuntimeException exception) {
      helper.send(sender, "command.spigot.failed", Map.of("reason", exception.getMessage()));
      plugin.getLogger().warning("Command failed: " + exception.getMessage());
      return false;
    }
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] arguments) {
    if (arguments.length == 1) {
      return helper.matches(
          arguments[0],
          "help",
          "version",
          "status",
          "doctor",
          "event",
          "definition",
          "validate",
          "template",
          "schedule",
          "rewards",
          "location",
          "model",
          "asset",
          "assets",
          "integration",
          "integrations",
          "update",
          "admin");
    }
    if (arguments.length >= 2) {
      String sub = arguments[0].toLowerCase(Locale.ROOT);
      SpigotSubcommandHandler handler = subcommands.get(sub);
      if (handler != null) {
        String[] subArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
        return handler.tabComplete(sender, subArgs);
      }
    }
    return List.of();
  }

  private void openAdmin(CommandSender sender) {
    helper.require(sender, "spectraevents.admin");
    if (guiController == null) {
      helper.send(
          sender,
          "command.spigot.unsupported",
          Map.of("feature", "Inventory GUI", "platform", "Paper family"));
      return;
    }
    if (sender instanceof Player player) {
      guiController.openMainMenu(player);
    } else {
      helper.send(sender, "command.gui-player-only");
    }
  }

  private void help(CommandSender sender) {
    helper.send(sender, "command.spigot.help-general");
    helper.send(sender, "command.spigot.help-events");
    helper.send(sender, "command.spigot.help-rewards");
    helper.send(sender, "command.spigot.help-definitions");
    helper.send(sender, "command.spigot.help-schedules");
  }
}
