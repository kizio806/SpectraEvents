package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command handler for update management subcommands. */
public final class UpdateCommandHandler {
  private final UpdateService updateService;
  private final RegionTaskScheduler regionScheduler;
  private final CommandText messages;

  public UpdateCommandHandler(
      UpdateService updateService, RegionTaskScheduler regionScheduler, LocaleCatalog locales) {
    this.updateService = updateService;
    this.regionScheduler = regionScheduler;
    this.messages = new CommandText(locales);
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("update")
        .then(
            Commands.literal("check")
                .requires(s -> s.getSender().hasPermission("spectraevents.update.check"))
                .executes(this::updateCheck))
        .then(
            Commands.literal("info")
                .requires(s -> s.getSender().hasPermission("spectraevents.update.info"))
                .executes(this::updateInfo));
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private int updateCheck(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    sender.sendMessage(messages.component("command.update.checking", NamedTextColor.GRAY));
    updateService
        .checkNow("stable")
        .whenComplete(
            (info, failure) ->
                replyLater(
                    sender,
                    failure != null
                        ? messages.component(
                            "command.update.failed",
                            NamedTextColor.RED,
                            java.util.Map.of("reason", safeMessage(failure)))
                        : updateResult(info)));
    return 1;
  }

  private Component updateResult(UpdateInfo info) {
    if (info.updateAvailable()) {
      return messages.component(
          "command.update.available",
          NamedTextColor.GREEN,
          java.util.Map.of("current", info.currentVersion(), "latest", info.latestVersion()));
    }
    if (info.status()
        == io.github.kizio806.spectraevents.application.update.UpdateCheckStatus.FAILED) {
      return messages.component(
          "command.update.failed",
          NamedTextColor.RED,
          java.util.Map.of("reason", info.releaseNotes()));
    }
    return messages.component(
        "command.update.current",
        NamedTextColor.GREEN,
        java.util.Map.of("version", info.currentVersion()));
  }

  private void replyLater(CommandSender sender, Component message) {
    if (sender instanceof Player player) {
      regionScheduler.executeFor(player, () -> player.sendMessage(message));
    } else {
      regionScheduler.executeGlobal(() -> sender.sendMessage(message));
    }
  }

  private String safeMessage(Throwable failure) {
    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }

  private int updateInfo(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    UpdateInfo info = updateService.currentInfo();
    sender.sendMessage(messages.component("command.update.info-title", NamedTextColor.GOLD));
    sender.sendMessage(
        messages
            .component("command.update.current-label", NamedTextColor.GRAY)
            .append(Component.text(info.currentVersion(), NamedTextColor.YELLOW)));
    sender.sendMessage(
        messages
            .component("command.update.latest-label", NamedTextColor.GRAY)
            .append(Component.text(info.latestVersion(), NamedTextColor.YELLOW)));
    sender.sendMessage(
        messages
            .component("command.update.status-label", NamedTextColor.GRAY)
            .append(statusComponent(info)));
    if (!info.releaseNotes().isEmpty()) {
      sender.sendMessage(
          messages
              .component("command.update.notes-label", NamedTextColor.GRAY)
              .append(Component.text(info.releaseNotes(), NamedTextColor.WHITE)));
    }
    return 1;
  }

  private Component statusComponent(UpdateInfo info) {
    if (info.status()
        == io.github.kizio806.spectraevents.application.update.UpdateCheckStatus.UPDATE_AVAILABLE) {
      return messages.component("command.update.status-available", NamedTextColor.GREEN);
    }
    if (info.status()
        == io.github.kizio806.spectraevents.application.update.UpdateCheckStatus.FAILED) {
      return messages.component("command.update.status-failed", NamedTextColor.RED);
    }
    if (info.status()
        == io.github.kizio806.spectraevents.application.update.UpdateCheckStatus.NOT_CHECKED) {
      return messages.component("command.update.status-not-checked", NamedTextColor.GRAY);
    }
    return messages.component("command.update.status-current", NamedTextColor.GRAY);
  }
}
