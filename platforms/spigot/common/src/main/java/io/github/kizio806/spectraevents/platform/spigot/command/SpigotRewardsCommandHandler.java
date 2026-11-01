package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.service.RewardMailboxService.ReconciliationDecision;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Handles `/spectraevents rewards` subcommands on Spigot. */
public final class SpigotRewardsCommandHandler implements SpigotSubcommandHandler {

  private final Plugin plugin;
  private final SpectraEventsApplication application;
  private final SpigotActionAdapter actionAdapter;
  private final SpigotCommandHelper helper;

  public SpigotRewardsCommandHandler(
      Plugin plugin,
      SpectraEventsApplication application,
      SpigotActionAdapter actionAdapter,
      SpigotCommandHelper helper) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.application = Objects.requireNonNull(application, "application");
    this.actionAdapter = Objects.requireNonNull(actionAdapter, "actionAdapter");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public boolean execute(CommandSender sender, String[] arguments) {
    if (arguments.length > 0 && "reconcile".equalsIgnoreCase(arguments[0])) {
      return reconcileRewards(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
    }
    helper.require(sender, "spectraevents.rewards.claim");
    if (!(sender instanceof Player player)) {
      throw new IllegalStateException("Rewards are available to in-game players only");
    }
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      var _ =
          application
              .rewardMailboxService()
              .pendingClaims(player.getUniqueId())
              .whenComplete(
                  (claims, failure) ->
                      Bukkit.getScheduler()
                          .runTask(
                              plugin,
                              () -> {
                                if (failure != null) {
                                  helper.send(
                                      sender,
                                      "command.spigot.failed",
                                      Map.of("reason", helper.messageFor(failure)));
                                  return;
                                }
                                helper.send(
                                    sender,
                                    "command.spigot.rewards-title",
                                    Map.of("count", claims.size()));
                                for (var claim : claims) {
                                  helper.send(
                                      sender,
                                      "command.spigot.reward-entry",
                                      Map.of("id", claim.id(), "count", claim.items().size()));
                                }
                              }));
      return true;
    }
    if (!"claim".equalsIgnoreCase(arguments[0])) {
      throw new IllegalArgumentException("Unknown rewards subcommand: " + arguments[0]);
    }
    helper.requireArguments(arguments, 2, "/spectraevents rewards claim <claim-id>");
    UUID claimId = UUID.fromString(arguments[1]);
    var _ =
        application
            .rewardMailboxService()
            .claim(
                player.getUniqueId(),
                claimId,
                (ignored, items) -> actionAdapter.deliverRewardItems(player, items))
            .whenComplete(
                (result, failure) ->
                    Bukkit.getScheduler()
                        .runTask(
                            plugin,
                            () -> {
                              if (failure != null) {
                                helper.send(
                                    sender,
                                    "command.spigot.failed",
                                    Map.of("reason", helper.messageFor(failure)));
                                return;
                              }
                              helper.send(
                                  sender,
                                  switch (result.status()) {
                                    case DELIVERED -> "messages.reward-delivered";
                                    case RETAINED -> "messages.reward-retained";
                                    case BUSY -> "command.spigot.reward-busy";
                                    case NOT_AVAILABLE -> "command.spigot.reward-unavailable";
                                  });
                            }));
    return true;
  }

  private boolean reconcileRewards(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.rewards.reconcile");
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      var _ =
          application
              .rewardMailboxService()
              .deliveringClaims()
              .whenComplete(
                  (claims, failure) ->
                      Bukkit.getScheduler()
                          .runTask(
                              plugin,
                              () -> {
                                if (failure != null) {
                                  helper.send(
                                      sender,
                                      "command.spigot.failed",
                                      Map.of("reason", helper.messageFor(failure)));
                                  return;
                                }
                                helper.send(
                                    sender,
                                    "command.rewards.reconcile-title",
                                    Map.of("count", claims.size()));
                                for (var claim : claims) {
                                  helper.send(
                                      sender,
                                      "command.rewards.reconcile-entry",
                                      Map.of(
                                          "id",
                                          claim.id(),
                                          "player",
                                          claim.playerId(),
                                          "count",
                                          claim.items().size()));
                                }
                              }));
      return true;
    }
    if (arguments.length != 2
        || (!"mark-delivered".equalsIgnoreCase(arguments[0])
            && !"return-pending".equalsIgnoreCase(arguments[0]))) {
      throw new IllegalArgumentException(
          "Usage: /spectraevents rewards reconcile <list|mark-delivered|return-pending> [claim-id]");
    }
    UUID claimId = UUID.fromString(arguments[1]);
    ReconciliationDecision decision =
        "mark-delivered".equalsIgnoreCase(arguments[0])
            ? ReconciliationDecision.MARK_DELIVERED
            : ReconciliationDecision.RETURN_TO_PENDING;
    var _ =
        application
            .rewardMailboxService()
            .reconcile(claimId, decision)
            .whenComplete(
                (resolved, failure) ->
                    Bukkit.getScheduler()
                        .runTask(
                            plugin,
                            () -> {
                              if (failure != null) {
                                helper.send(
                                    sender,
                                    "command.spigot.failed",
                                    Map.of("reason", helper.messageFor(failure)));
                                return;
                              }
                              helper.send(
                                  sender,
                                  resolved
                                      ? "command.rewards.reconcile-resolved"
                                      : "command.rewards.reconcile-not-found",
                                  Map.of("id", claimId, "decision", decision.name()));
                            }));
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] arguments) {
    if (arguments.length == 1) {
      return helper.matches(arguments[0], "list", "claim");
    }
    return List.of();
  }
}
