package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Command handler for update management subcommands. */
public final class UpdateCommandHandler {
  private final UpdateService updateService;

  public UpdateCommandHandler(UpdateService updateService) {
    this.updateService = updateService;
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

  private int updateCheck(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    sender.sendMessage(
        Component.text("Checking for SpectraEvents updates...", NamedTextColor.GRAY));
    updateService
        .checkNow("stable")
        .thenAccept(
            info -> {
              if (info.updateAvailable()) {
                sender.sendMessage(
                    Component.text(
                        "Update available: "
                            + info.currentVersion()
                            + " -> "
                            + info.latestVersion(),
                        NamedTextColor.GREEN));
              } else {
                sender.sendMessage(
                    Component.text(
                        "SpectraEvents is up to date (" + info.currentVersion() + ")",
                        NamedTextColor.GREEN));
              }
            })
        .exceptionally(
            ex -> {
              sender.sendMessage(
                  Component.text(
                      "Failed to check for updates: " + ex.getMessage(), NamedTextColor.RED));
              return null;
            });
    return 1;
  }

  private int updateInfo(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    UpdateInfo info = updateService.currentInfo();
    sender.sendMessage(Component.text("SpectraEvents Update Info:", NamedTextColor.GOLD));
    sender.sendMessage(
        Component.text(" Current Version: ", NamedTextColor.GRAY)
            .append(Component.text(info.currentVersion(), NamedTextColor.YELLOW)));
    sender.sendMessage(
        Component.text(" Latest Version: ", NamedTextColor.GRAY)
            .append(Component.text(info.latestVersion(), NamedTextColor.YELLOW)));
    sender.sendMessage(
        Component.text(" Status: ", NamedTextColor.GRAY)
            .append(
                Component.text(
                    info.updateAvailable() ? "Update Available" : "Up to date",
                    info.updateAvailable() ? NamedTextColor.GREEN : NamedTextColor.GREEN)));
    if (!info.releaseNotes().isEmpty()) {
      sender.sendMessage(
          Component.text(" Notes: ", NamedTextColor.GRAY)
              .append(Component.text(info.releaseNotes(), NamedTextColor.WHITE)));
    }
    return 1;
  }
}
