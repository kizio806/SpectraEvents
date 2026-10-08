package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.paper.action.PaperActionAdapter;
import io.github.kizio806.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import io.github.kizio806.spectraevents.platform.paper.gui.AdminGuiController;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.io.IOException;
import java.nio.file.Path;
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
  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final AdminGuiController guiController;
  private final io.github.kizio806.spectraevents.application.service.RewardMailboxService
      rewardMailboxService;
  private final PaperActionAdapter actionAdapter;
  private final Path schedulesFile;
  private final SpectraEventsApplication application;
  private final CommandText messages;
  private final RegionTaskScheduler regionScheduler;

  public SpectraMainCommand(
      Plugin plugin,
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
    this.definitionRegistry = definitionRegistry;
    this.instanceRepository = instanceRepository;
    this.guiController = guiController;
    this.rewardMailboxService = application.rewardMailboxService();
    this.actionAdapter = actionAdapter;
    this.regionScheduler = regionScheduler;
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
        .then(templateCommandHandler.build())
        .then(locationCommandHandler.build())
        .then(
            Commands.literal("admin")
                .requires(s -> hasPerm(s, "spectraevents.gui"))
                .executes(this::openGui))
        .then(eventCommandHandler.build())
        .then(definitionCommandHandler.buildRootValidationCommand())
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
        messages.component(
            "command.spigot.schedules-title",
            NamedTextColor.AQUA,
            Map.of("count", service.schedules().size())));
    for (var schedule : service.schedules()) {
      sender.sendMessage(
          messages.component(
              "command.spigot.schedule-entry",
              NamedTextColor.GRAY,
              Map.of(
                  "id", schedule.id(),
                  "definition", schedule.definitionId(),
                  "zone", schedule.zoneId())));
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
          messages.component(
              "command.spigot.schedules-reloaded",
              result.failures().isEmpty() ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
              Map.of("loaded", result.schedules().size(), "failed", result.failures().size())));
      result
          .failures()
          .forEach(
              failure ->
                  sender.sendMessage(
                      messages.component(
                          "command.spigot.diagnostic-entry",
                          NamedTextColor.RED,
                          Map.of("path", failure.path(), "message", failure.message()))));
      return 1;
    } catch (IOException exception) {
      sender.sendMessage(
          messages.component(
              "command.paper.schedules-read-failed",
              NamedTextColor.RED,
              Map.of("reason", exception.getMessage())));
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
        .then(
            Commands.literal("list")
                .requires(source -> hasPerm(source, "spectraevents.rewards.claim"))
                .executes(this::listRewards))
        .then(
            Commands.literal("claim")
                .requires(source -> hasPerm(source, "spectraevents.rewards.claim"))
                .then(
                    Commands.argument("claim-id", StringArgumentType.word())
                        .executes(this::claimReward)))
        .then(
            Commands.literal("reconcile")
                .requires(source -> hasPerm(source, "spectraevents.rewards.reconcile"))
                .then(Commands.literal("list").executes(this::listDeliveringClaims))
                .then(
                    Commands.literal("mark-delivered")
                        .then(
                            Commands.argument("claim-id", StringArgumentType.word())
                                .executes(
                                    context ->
                                        reconcileClaim(
                                            context,
                                            io.github.kizio806.spectraevents.application.service
                                                .RewardMailboxService.ReconciliationDecision
                                                .MARK_DELIVERED))))
                .then(
                    Commands.literal("return-pending")
                        .then(
                            Commands.argument("claim-id", StringArgumentType.word())
                                .executes(
                                    context ->
                                        reconcileClaim(
                                            context,
                                            io.github.kizio806.spectraevents.application.service
                                                .RewardMailboxService.ReconciliationDecision
                                                .RETURN_TO_PENDING)))));
  }

  @SuppressWarnings(
      "FutureReturnValueIgnored") // Completion is returned to the player's owning region.
  private int listRewards(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          messages.component("command.paper.rewards-player-only", NamedTextColor.RED));
      return 0;
    }
    rewardMailboxService
        .pendingClaims(player.getUniqueId())
        .whenComplete(
            (claims, failure) ->
                regionScheduler.executeFor(
                    player,
                    () -> {
                      if (failure != null) {
                        player.sendMessage(
                            messages.component(
                                "command.spigot.failed",
                                NamedTextColor.RED,
                                Map.of("reason", messageFor(failure))));
                        return;
                      }
                      player.sendMessage(
                          messages.component(
                              "command.spigot.rewards-title",
                              NamedTextColor.AQUA,
                              Map.of("count", claims.size())));
                      for (var claim : claims) {
                        player.sendMessage(
                            messages.component(
                                "command.spigot.reward-entry",
                                NamedTextColor.GRAY,
                                Map.of("id", claim.id(), "count", claim.items().size())));
                      }
                    }));
    return 1;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private int claimReward(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          messages.component("command.paper.rewards-player-only", NamedTextColor.RED));
      return 0;
    }
    java.util.UUID claimId;
    try {
      claimId = java.util.UUID.fromString(StringArgumentType.getString(context, "claim-id"));
    } catch (IllegalArgumentException exception) {
      sender.sendMessage(messages.component("command.paper.invalid-reward-id", NamedTextColor.RED));
      return 0;
    }
    rewardMailboxService
        .claim(
            player.getUniqueId(),
            claimId,
            (ignored, items) -> actionAdapter.deliverRewardItems(player, items))
        .whenComplete(
            (result, failure) ->
                regionScheduler.executeFor(
                    player,
                    () -> {
                      if (failure != null) {
                        player.sendMessage(
                            messages.component(
                                "command.spigot.failed",
                                NamedTextColor.RED,
                                Map.of("reason", messageFor(failure))));
                        return;
                      }
                      NamedTextColor color =
                          result.status()
                                  == io.github.kizio806.spectraevents.application.service
                                      .RewardMailboxService.ClaimDeliveryResult.Status.DELIVERED
                              ? NamedTextColor.GREEN
                              : NamedTextColor.YELLOW;
                      String key =
                          switch (result.status()) {
                            case DELIVERED -> "messages.reward-delivered";
                            case RETAINED -> "messages.reward-retained";
                            case BUSY -> "command.spigot.reward-busy";
                            case NOT_AVAILABLE -> "command.spigot.reward-unavailable";
                          };
                      player.sendMessage(messages.component(key, color));
                    }));
    return 1;
  }

  @SuppressWarnings(
      "FutureReturnValueIgnored") // Completion is returned to the command sender on its region.
  private int listDeliveringClaims(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    rewardMailboxService
        .deliveringClaims()
        .whenComplete(
            (claims, failure) ->
                sendRewardReconciliationResult(
                    sender,
                    claims,
                    failure,
                    "command.rewards.reconcile-title",
                    "command.rewards.reconcile-entry"));
    return 1;
  }

  @SuppressWarnings(
      "FutureReturnValueIgnored") // Completion is returned to the command sender on its region.
  private int reconcileClaim(
      CommandContext<CommandSourceStack> context,
      io.github.kizio806.spectraevents.application.service.RewardMailboxService
              .ReconciliationDecision
          decision) {
    CommandSender sender = context.getSource().getSender();
    java.util.UUID claimId;
    try {
      claimId = java.util.UUID.fromString(StringArgumentType.getString(context, "claim-id"));
    } catch (IllegalArgumentException exception) {
      sender.sendMessage(messages.component("command.paper.invalid-reward-id", NamedTextColor.RED));
      return 0;
    }
    rewardMailboxService
        .reconcile(claimId, decision)
        .whenComplete(
            (resolved, failure) ->
                regionScheduler.executeGlobal(
                    () -> {
                      if (failure != null) {
                        sender.sendMessage(
                            messages.component(
                                "command.spigot.failed",
                                NamedTextColor.RED,
                                Map.of("reason", messageFor(failure))));
                        return;
                      }
                      sender.sendMessage(
                          messages.component(
                              resolved
                                  ? "command.rewards.reconcile-resolved"
                                  : "command.rewards.reconcile-not-found",
                              resolved ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                              Map.of("id", claimId, "decision", decision.name())));
                    }));
    return 1;
  }

  private void sendRewardReconciliationResult(
      CommandSender sender,
      java.util.List<io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim> claims,
      Throwable failure,
      String titleKey,
      String entryKey) {
    regionScheduler.executeGlobal(
        () -> {
          if (failure != null) {
            sender.sendMessage(
                messages.component(
                    "command.spigot.failed",
                    NamedTextColor.RED,
                    Map.of("reason", messageFor(failure))));
            return;
          }
          sender.sendMessage(
              messages.component(titleKey, NamedTextColor.GOLD, Map.of("count", claims.size())));
          for (var claim : claims) {
            sender.sendMessage(
                messages.component(
                    entryKey,
                    NamedTextColor.YELLOW,
                    Map.of(
                        "id",
                        claim.id(),
                        "player",
                        claim.playerId(),
                        "count",
                        claim.items().size())));
          }
        });
  }

  private boolean hasPerm(CommandSourceStack source, String perm) {
    CommandSender sender = source.getSender();
    return sender.hasPermission(perm)
        || sender.hasPermission("spectraevents.admin")
        || sender.isOp();
  }

  private String messageFor(Throwable failure) {
    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }

  private int help(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    for (String key :
        java.util.List.of(
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
      sender.sendMessage(messages.component("command.paper.gui-player-only", NamedTextColor.RED));
    }
    return 1;
  }
}
