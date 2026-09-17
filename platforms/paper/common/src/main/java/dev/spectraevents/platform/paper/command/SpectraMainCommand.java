package dev.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.spectraevents.application.SpectraEventsApplication;
import dev.spectraevents.application.config.registry.EventDefinitionRegistry;
import dev.spectraevents.application.integration.IntegrationRegistry;
import dev.spectraevents.application.port.EventInstanceRepository;
import dev.spectraevents.application.service.EventOrchestrationService;
import dev.spectraevents.application.update.UpdateService;
import dev.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import dev.spectraevents.platform.paper.gui.AdminGuiController;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Main production command tree for /spectra in Paper platform family. */
public final class SpectraMainCommand {
  private final EventCommandHandler eventCommandHandler;
  private final DefinitionCommandHandler definitionCommandHandler;
  private final UpdateCommandHandler updateCommandHandler;
  private final DiagnosticsCommandHandler diagnosticsCommandHandler;
  private final IntegrationCommandHandler integrationCommandHandler;
  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final AdminGuiController guiController;
  private final SpectraEventsApplication application;

  public SpectraMainCommand(
      EventOrchestrationService orchestrationService,
      EventDefinitionRegistry definitionRegistry,
      EventInstanceRepository instanceRepository,
      PaperDefinitionConfigBootstrap configBootstrap,
      IntegrationRegistry integrationRegistry,
      UpdateService updateService,
      AdminGuiController guiController,
      SpectraEventsApplication application) {
    this.eventCommandHandler = new EventCommandHandler(orchestrationService, instanceRepository);
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
    this.definitionRegistry = definitionRegistry;
    this.instanceRepository = instanceRepository;
    this.guiController = guiController;
    this.application = application;
  }

  public LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
    return Commands.literal("spectra")
        .executes(this::help)
        .then(Commands.literal("help").executes(this::help))
        .then(Commands.literal("version").executes(this::version))
        .then(
            Commands.literal("status")
                .requires(s -> hasPerm(s, "spectraevents.status"))
                .executes(this::status))
        .then(diagnosticsCommandHandler.build())
        .then(integrationCommandHandler.build())
        .then(
            Commands.literal("admin")
                .requires(s -> hasPerm(s, "spectraevents.gui"))
                .executes(this::openGui))
        .then(
            Commands.literal("gui")
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
        Component.text(
            "SpectraEvents Management Commands", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
    sender.sendMessage(Component.text(" /spectra help - Show this help menu", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(" /spectra version - Display plugin version", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(" /spectra status - Quick status overview", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(" /spectra doctor - Run full diagnostic health check", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(" /spectra gui - Open admin inventory GUI", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(
            " /spectra event <list|start|stop|cancel> - Manage instances", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(
            " /spectra definition <list|reload|validate> - Manage configs", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(" /spectra integrations - View integration status", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(" /spectra update <check|info> - Check for updates", NamedTextColor.GRAY));
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
