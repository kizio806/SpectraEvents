package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.paper.action.PaperActionAdapter;
import io.github.kizio806.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import io.github.kizio806.spectraevents.platform.paper.gui.AdminGuiController;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.io.IOException;
import java.nio.file.Path;
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
  private final AssetCommandHandler assetCommandHandler;
  private final LocationCommandHandler locationCommandHandler;
  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final AdminGuiController guiController;
  private final io.github.kizio806.spectraevents.application.service.RewardMailboxService
      rewardMailboxService;
  private final PaperActionAdapter actionAdapter;
  private final Path schedulesFile;
  private final SpectraEventsApplication application;

  public SpectraMainCommand(
      EventOrchestrationService orchestrationService,
      EventDefinitionRegistry definitionRegistry,
      EventInstanceRepository instanceRepository,
      PaperDefinitionConfigBootstrap configBootstrap,
      IntegrationRegistry integrationRegistry,
      UpdateService updateService,
      AdminGuiController guiController,
      SpectraEventsApplication application,
      PaperEventSettingsStore settingsStore,
      PaperActionAdapter actionAdapter,
      Path schedulesFile) {
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
    this.modelCommandHandler = new ModelCommandHandler(application.modelDefinitionRegistry());
    this.assetCommandHandler = new AssetCommandHandler(application.assetPipelineService());
    this.locationCommandHandler = new LocationCommandHandler(settingsStore);
    this.definitionRegistry = definitionRegistry;
    this.instanceRepository = instanceRepository;
    this.guiController = guiController;
    this.rewardMailboxService = application.rewardMailboxService();
    this.actionAdapter = actionAdapter;
    this.schedulesFile = schedulesFile;
    this.application = application;
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
        .then(locationCommandHandler.build())
        .then(
            Commands.literal("admin")
                .requires(s -> hasPerm(s, "spectraevents.gui"))
                .executes(this::openGui))
        .then(eventCommandHandler.build())
        .then(buildScheduleCommand())
        .then(buildRewardsCommand())
        .then(definitionCommandHandler.build())
        .then(updateCommandHandler.build());
  }

  private LiteralArgumentBuilder<CommandSourceStack> buildScheduleCommand() {
    return Commands.literal("schedule")
        .requires(source -> hasPerm(source, "spectraevents.schedule"))
        .executes(this::listSchedules)
        .then(Commands.literal("list").executes(this::listSchedules))
        .then(Commands.literal("reload").executes(this::reloadSchedules));
  }

  private int listSchedules(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    var service = requireScheduleService();
    sender.sendMessage(
        Component.text("Schedules: " + service.schedules().size(), NamedTextColor.AQUA));
    for (var schedule : service.schedules()) {
      sender.sendMessage(
          Component.text(
              " - "
                  + schedule.id()
                  + " -> "
                  + schedule.definitionId()
                  + " ("
                  + schedule.zoneId()
                  + ")",
              NamedTextColor.GRAY));
    }
    return 1;
  }

  private int reloadSchedules(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    try {
      var result =
          new io.github.kizio806.spectraevents.application.schedule.FileSystemScheduleLoader(
                  new io.github.kizio806.spectraevents.application.schedule.ScheduleYamlLoader())
              .load(schedulesFile);
      requireScheduleService().replace(result.schedules());
      sender.sendMessage(
          Component.text(
              "Schedules loaded="
                  + result.schedules().size()
                  + " failed="
                  + result.failures().size(),
              result.failures().isEmpty() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
      result
          .failures()
          .forEach(
              failure ->
                  sender.sendMessage(
                      Component.text(
                          " - " + failure.path() + ": " + failure.message(), NamedTextColor.RED)));
      return 1;
    } catch (IOException exception) {
      sender.sendMessage(
          Component.text(
              "Could not read schedules.yml: " + exception.getMessage(), NamedTextColor.RED));
      return 0;
    }
  }

  private io.github.kizio806.spectraevents.application.schedule.EventScheduleService
      requireScheduleService() {
    if (application.scheduleService() == null) {
      throw new IllegalStateException("Scheduling is unavailable on this platform");
    }
    return application.scheduleService();
  }

  private LiteralArgumentBuilder<CommandSourceStack> buildRewardsCommand() {
    return Commands.literal("rewards")
        .requires(source -> hasPerm(source, "spectraevents.rewards.claim"))
        .executes(this::listRewards)
        .then(Commands.literal("list").executes(this::listRewards))
        .then(
            Commands.literal("claim")
                .then(
                    Commands.argument("claim-id", StringArgumentType.word())
                        .executes(this::claimReward)));
  }

  private int listRewards(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          Component.text("Rewards are available to in-game players only.", NamedTextColor.RED));
      return 0;
    }
    var claims = rewardMailboxService.pendingClaims(player.getUniqueId());
    sender.sendMessage(Component.text("Pending rewards: " + claims.size(), NamedTextColor.AQUA));
    for (var claim : claims) {
      sender.sendMessage(
          Component.text(
              " - " + claim.id() + " (" + claim.items().size() + " item entries)",
              NamedTextColor.GRAY));
    }
    return 1;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private int claimReward(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          Component.text("Rewards are available to in-game players only.", NamedTextColor.RED));
      return 0;
    }
    java.util.UUID claimId;
    try {
      claimId = java.util.UUID.fromString(StringArgumentType.getString(context, "claim-id"));
    } catch (IllegalArgumentException exception) {
      sender.sendMessage(Component.text("Invalid reward claim ID.", NamedTextColor.RED));
      return 0;
    }
    rewardMailboxService
        .claim(
            player.getUniqueId(),
            claimId,
            (ignored, items) -> actionAdapter.deliverRewardItems(player, items))
        .thenAccept(
            result -> {
              NamedTextColor color =
                  result.status()
                          == io.github.kizio806.spectraevents.application.service
                              .RewardMailboxService.ClaimDeliveryResult.Status.DELIVERED
                      ? NamedTextColor.GREEN
                      : NamedTextColor.YELLOW;
              String message =
                  switch (result.status()) {
                    case DELIVERED -> "Reward delivered.";
                    case RETAINED ->
                        "Reward is retained. Make space in your inventory and try again.";
                    case BUSY -> "That reward is already being delivered.";
                    case NOT_AVAILABLE -> "That reward is not available.";
                  };
              player.sendMessage(Component.text(message, color));
            });
    return 1;
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

    sender.sendMessage(Component.text("\n[Models]", NamedTextColor.AQUA));
    sender.sendMessage(
        Component.text(
            " /spectraevents model <list|info> - Inspect registered models", NamedTextColor.GRAY));

    sender.sendMessage(Component.text("\n[Assets]", NamedTextColor.AQUA));
    sender.sendMessage(
        Component.text(
            " /spectraevents assets <list|info|import|validate|build> - Manage assets",
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
