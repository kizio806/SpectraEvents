package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import io.github.kizio806.spectraevents.platform.paper.gui.AdminGuiController;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Main production command tree for /spectraevents in Paper platform family. */
public final class SpectraMainCommand {
  private final EventCommandHandler eventCommandHandler;
  private final DefinitionCommandHandler definitionCommandHandler;
  private final UpdateCommandHandler updateCommandHandler;
  private final DiagnosticsCommandHandler diagnosticsCommandHandler;
  private final IntegrationCommandHandler integrationCommandHandler;
  private final ModelCommandHandler modelCommandHandler;
  private final AnimationCommandHandler animationCommandHandler;
  private final AssetCommandHandler assetCommandHandler;
  private final LocationCommandHandler locationCommandHandler;
  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final AdminGuiController guiController;

  public SpectraMainCommand(
      EventOrchestrationService orchestrationService,
      EventDefinitionRegistry definitionRegistry,
      EventInstanceRepository instanceRepository,
      PaperDefinitionConfigBootstrap configBootstrap,
      IntegrationRegistry integrationRegistry,
      UpdateService updateService,
      AdminGuiController guiController,
      SpectraEventsApplication application,
      PaperEventSettingsStore settingsStore) {
    this.eventCommandHandler =
        new EventCommandHandler(
            orchestrationService, instanceRepository, definitionRegistry, settingsStore);
    this.definitionCommandHandler =
        new DefinitionCommandHandler(definitionRegistry, configBootstrap);
    this.updateCommandHandler = new UpdateCommandHandler(updateService);
    this.diagnosticsCommandHandler =
        new DiagnosticsCommandHandler(
            definitionRegistry,
            instanceRepository,
            configBootstrap,
            integrationRegistry,
            application);
    this.integrationCommandHandler = new IntegrationCommandHandler(integrationRegistry);
    this.modelCommandHandler =
        new ModelCommandHandler(
            application.modelDefinitionRegistry(), application.modelRuntimeService());
    this.animationCommandHandler =
        new AnimationCommandHandler(
            application.modelDefinitionRegistry(),
            application.modelRuntimeService(),
            application.animationDefinitionRegistry(),
            application.animationRuntimeService());
    this.assetCommandHandler = new AssetCommandHandler(application.assetPipelineService());
    this.locationCommandHandler = new LocationCommandHandler(settingsStore);
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
        .then(animationCommandHandler.build())
        .then(assetCommandHandler.build())
        .then(locationCommandHandler.build())
        .then(
            Commands.literal("admin")
                .requires(s -> hasPerm(s, "spectraevents.gui"))
                .executes(this::openGui))
        .then(eventCommandHandler.build())
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
    sender.sendMessage(
        Component.text("SpectraEvents", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

    sender.sendMessage(Component.text("\n[General]", NamedTextColor.AQUA));
    sender.sendMessage(
        Component.text(" /spectraevents help - Show available commands", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(" /spectraevents status - View server event status", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(
            " /spectraevents event <list|start|stop|cancel> - Manage events", NamedTextColor.GRAY));

    sender.sendMessage(Component.text("\n[Models & Animations]", NamedTextColor.AQUA));
    sender.sendMessage(
        Component.text(
            " /spectraevents model <list|info|validate|spawn|remove> - Manage models",
            NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(
            " /spectraevents animation <list|info|play|pause|resume|seek|stop> - Manage animations",
            NamedTextColor.GRAY));

    sender.sendMessage(Component.text("\n[Assets]", NamedTextColor.AQUA));
    sender.sendMessage(
        Component.text(
            " /spectraevents assets <list|info|import|validate|build|refresh|clean> - Manage assets",
            NamedTextColor.GRAY));

    sender.sendMessage(Component.text("\n[Administration]", NamedTextColor.AQUA));
    sender.sendMessage(
        Component.text(" /spectraevents admin - Open the admin panel", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(
            " /spectraevents definition <list|reload|validate> - Manage definitions",
            NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(
            " /spectraevents location <set|list|remove> - Manage locations", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(" /spectraevents doctor - Run diagnostics", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(
            " /spectraevents update <check|info> - Check for updates", NamedTextColor.GRAY));
    return 1;
  }

  private int version(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    sender.sendMessage(
        Component.text(
            "SpectraEvents 1.0.0-SNAPSHOT (Beta Foundation)", NamedTextColor.DARK_PURPLE));
    return 1;
  }

  private int status(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    int activeCount = instanceRepository.findAll().size();
    int defCount = definitionRegistry.getAll().size();
    sender.sendMessage(
        Component.text("[SpectraEvents Status] ", NamedTextColor.DARK_PURPLE)
            .append(Component.text("Active Events: " + activeCount, NamedTextColor.GREEN))
            .append(
                Component.text(" | Registered Definitions: " + defCount, NamedTextColor.YELLOW)));
    return 1;
  }

  private int openGui(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (sender instanceof Player player) {
      guiController.openMainMenu(player);
    } else {
      sender.sendMessage(
          Component.text("GUI can only be opened by in-game players.", NamedTextColor.RED));
    }
    return 1;
  }
}
