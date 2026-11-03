package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import io.github.kizio806.spectraevents.platform.bukkit.gui.AdminGuiController;
import io.github.kizio806.spectraevents.platform.paper.action.PaperActionAdapter;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Main production command tree for /spectraevents in Paper platform family. */
public final class SpectraMainCommand {
  private final EventCommandHandler eventCommandHandler;
  private final DefinitionCommandHandler definitionCommandHandler;
  private final UpdateCommandHandler updateCommandHandler;
  private final DiagnosticsCommandHandler diagnosticsCommandHandler;
  private final IntegrationCommandHandler integrationCommandHandler;
  private final ModelCommandHandler modelCommandHandler;
  private final AssetCommandHandler assetCommandHandler;
  private final LocationCommandHandler locationCommandHandler;
  private final TemplateCommandHandler templateCommandHandler;
  private final ScheduleCommandHandler scheduleCommandHandler;
  private final RewardsCommandHandler rewardsCommandHandler;
  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final AdminGuiController guiController;
  private final CommandText messages;

  public SpectraMainCommand(
      Plugin plugin,
      EventOrchestrationService orchestrationService,
      EventDefinitionRegistry definitionRegistry,
      EventInstanceRepository instanceRepository,
      BukkitDefinitionConfigBootstrap configBootstrap,
      IntegrationRegistry integrationRegistry,
      UpdateService updateService,
      AdminGuiController guiController,
      SpectraEventsApplication application,
      BukkitEventSettingsStore settingsStore,
      PaperActionAdapter actionAdapter,
      RegionTaskScheduler regionScheduler,
      Path schedulesFile,
      LocaleCatalog locales) {
    this.messages = new CommandText(locales);
    this.eventCommandHandler =
        new EventCommandHandler(
            orchestrationService,
            instanceRepository,
            definitionRegistry,
            settingsStore,
            regionScheduler,
            locales);
    this.definitionCommandHandler =
        new DefinitionCommandHandler(
            definitionRegistry, configBootstrap, application, plugin, integrationRegistry, locales);
    this.updateCommandHandler = new UpdateCommandHandler(updateService, regionScheduler, locales);
    this.diagnosticsCommandHandler =
        new DiagnosticsCommandHandler(
            definitionRegistry,
            instanceRepository,
            configBootstrap,
            integrationRegistry,
            application,
            locales);
    this.integrationCommandHandler = new IntegrationCommandHandler(integrationRegistry, locales);
    this.modelCommandHandler =
        new ModelCommandHandler(application.modelDefinitionRegistry(), locales);
    this.assetCommandHandler =
        new AssetCommandHandler(regionScheduler, application.assetPipelineService(), locales);
    this.locationCommandHandler = new LocationCommandHandler(settingsStore, locales);
    this.templateCommandHandler =
        new TemplateCommandHandler(plugin, application, configBootstrap, regionScheduler);
    this.scheduleCommandHandler = new ScheduleCommandHandler(application, schedulesFile, locales);
    this.rewardsCommandHandler =
        new RewardsCommandHandler(
            application.rewardMailboxService(), actionAdapter, regionScheduler, locales);
    this.definitionRegistry = definitionRegistry;
    this.instanceRepository = instanceRepository;
    this.guiController = guiController;
  }

  public LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
    return Commands.literal("spectraevents")
        .executes(this::help)
        .then(Commands.literal("help").executes(this::help))
        .then(Commands.literal("version").executes(this::version))
        .then(
            Commands.literal("status")
                .requires(s -> hasPerm(s, "spectraevents.status"))
                .executes(this::status))
        .then(diagnosticsCommandHandler.build())
        .then(integrationCommandHandler.build())
        .then(modelCommandHandler.build())
        .then(assetCommandHandler.build())
        .then(templateCommandHandler.build())
        .then(locationCommandHandler.build())
        .then(
            Commands.literal("admin")
                .requires(s -> hasPerm(s, "spectraevents.gui"))
                .executes(this::openGui))
        .then(eventCommandHandler.build())
        .then(definitionCommandHandler.buildRootValidationCommand())
        .then(scheduleCommandHandler.build())
        .then(rewardsCommandHandler.build())
        .then(definitionCommandHandler.build())
        .then(updateCommandHandler.build());
  }

  private boolean hasPerm(CommandSourceStack source, String perm) {
    CommandSender sender = source.getSender();
    return sender.hasPermission(perm)
        || sender.hasPermission("spectraevents.admin")
        || sender.isOp();
  }

  private int help(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    for (String key :
        List.of(
            "command.paper.help-title",
            "command.paper.help-general",
            "command.paper.help-command",
            "command.paper.help-status",
            "command.paper.help-events",
            "command.paper.help-models",
            "command.paper.help-model-command",
            "command.paper.help-assets",
            "command.paper.help-asset-command",
            "command.paper.help-administration",
            "command.paper.help-admin-command",
            "command.paper.help-definition-command",
            "command.paper.help-location-command",
            "command.paper.help-doctor-command",
            "command.paper.help-update-command")) {
      sender.sendMessage(
          messages.component(
              key,
              key.endsWith("title")
                  ? NamedTextColor.DARK_PURPLE
                  : key.startsWith("command.paper.help-") && !key.contains("command")
                      ? NamedTextColor.AQUA
                      : NamedTextColor.GRAY));
    }
    return 1;
  }

  private int version(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    sender.sendMessage(messages.component("command.paper.version", NamedTextColor.DARK_PURPLE));
    return 1;
  }

  private int status(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    int activeCount = instanceRepository.findAll().size();
    int defCount = definitionRegistry.getAll().size();
    sender.sendMessage(
        messages.component(
            "command.paper.status",
            NamedTextColor.GREEN,
            Map.of("events", activeCount, "definitions", defCount)));
    return 1;
  }

  private int openGui(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (sender instanceof Player player) {
      guiController.openMainMenu(player);
    } else {
      sender.sendMessage(messages.component("command.gui-player-only", NamedTextColor.RED));
    }
    return 1;
  }
}
