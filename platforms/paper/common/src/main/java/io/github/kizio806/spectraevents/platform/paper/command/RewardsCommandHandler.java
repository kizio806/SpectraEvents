package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.service.RewardMailboxService;
import io.github.kizio806.spectraevents.application.service.RewardMailboxService.ReconciliationDecision;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.platform.bukkit.concurrent.AsyncFutures;
import io.github.kizio806.spectraevents.platform.paper.action.PaperActionAdapter;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Brigadier command handler for reward claims and reconciliation (/event rewards). */
public final class RewardsCommandHandler {
  private final RewardMailboxService rewardMailboxService;
  private final PaperActionAdapter actionAdapter;
  private final RegionTaskScheduler regionScheduler;
  private final CommandText messages;

  public RewardsCommandHandler(
      RewardMailboxService rewardMailboxService,
      PaperActionAdapter actionAdapter,
      RegionTaskScheduler regionScheduler,
      LocaleCatalog locales) {
    this.rewardMailboxService =
        Objects.requireNonNull(rewardMailboxService, "rewardMailboxService");
    this.actionAdapter = Objects.requireNonNull(actionAdapter, "actionAdapter");
    this.regionScheduler = Objects.requireNonNull(regionScheduler, "regionScheduler");
    this.messages = new CommandText(Objects.requireNonNull(locales, "locales"));
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
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
                                            context, ReconciliationDecision.MARK_DELIVERED))))
                .then(
                    Commands.literal("return-pending")
                        .then(
                            Commands.argument("claim-id", StringArgumentType.word())
                                .executes(
                                    context ->
                                        reconcileClaim(
                                            context, ReconciliationDecision.RETURN_TO_PENDING)))));
  }

  private int listRewards(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          messages.component("command.paper.rewards-player-only", NamedTextColor.RED));
      return 0;
    }
    AsyncFutures.discard(
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
                                    "command.paper.failed",
                                    NamedTextColor.RED,
                                    Map.of("reason", messageFor(failure))));
                            return;
                          }
                          player.sendMessage(
                              messages.component(
                                  "command.paper.rewards-title",
                                  NamedTextColor.AQUA,
                                  Map.of("count", claims.size())));
                          for (var claim : claims) {
                            player.sendMessage(
                                messages.component(
                                    "command.paper.reward-entry",
                                    NamedTextColor.GRAY,
                                    Map.of("id", claim.id(), "count", claim.items().size())));
                          }
                        })));
    return 1;
  }

  private int claimReward(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          messages.component("command.paper.rewards-player-only", NamedTextColor.RED));
      return 0;
    }
    UUID claimId;
    try {
      claimId = UUID.fromString(StringArgumentType.getString(context, "claim-id"));
    } catch (IllegalArgumentException exception) {
      sender.sendMessage(messages.component("command.paper.invalid-reward-id", NamedTextColor.RED));
      return 0;
    }
    AsyncFutures.discard(
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
                                    "command.paper.failed",
                                    NamedTextColor.RED,
                                    Map.of("reason", messageFor(failure))));
                            return;
                          }
                          NamedTextColor color =
                              result.status()
                                      == RewardMailboxService.ClaimDeliveryResult.Status.DELIVERED
                                  ? NamedTextColor.GREEN
                                  : NamedTextColor.YELLOW;
                          String key =
                              switch (result.status()) {
                                case DELIVERED -> "messages.reward-delivered";
                                case RETAINED -> "messages.reward-retained";
                                case BUSY -> "command.paper.reward-busy";
                                case NOT_AVAILABLE -> "command.paper.reward-unavailable";
                              };
                          player.sendMessage(messages.component(key, color));
                        })));
    return 1;
  }

  private int listDeliveringClaims(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    AsyncFutures.discard(
        rewardMailboxService
            .deliveringClaims()
            .whenComplete(
                (claims, failure) ->
                    sendRewardReconciliationResult(
                        sender,
                        claims,
                        failure,
                        "command.rewards.reconcile-title",
                        "command.rewards.reconcile-entry")));
    return 1;
  }

  private int reconcileClaim(
      CommandContext<CommandSourceStack> context, ReconciliationDecision decision) {
    CommandSender sender = context.getSource().getSender();
    UUID claimId;
    try {
      claimId = UUID.fromString(StringArgumentType.getString(context, "claim-id"));
    } catch (IllegalArgumentException exception) {
      sender.sendMessage(messages.component("command.paper.invalid-reward-id", NamedTextColor.RED));
      return 0;
    }
    AsyncFutures.discard(
        rewardMailboxService
            .reconcile(claimId, decision)
            .whenComplete(
                (resolved, failure) ->
                    regionScheduler.executeGlobal(
                        () -> {
                          if (failure != null) {
                            sender.sendMessage(
                                messages.component(
                                    "command.paper.failed",
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
                        })));
    return 1;
  }

  private void sendRewardReconciliationResult(
      CommandSender sender,
      List<RewardClaim> claims,
      Throwable failure,
      String titleKey,
      String entryKey) {
    regionScheduler.executeGlobal(
        () -> {
          if (failure != null) {
            sender.sendMessage(
                messages.component(
                    "command.paper.failed",
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
}
