package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.update.UpdateService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.CommandSender;

/** Handles `/spectraevents update` subcommands on Spigot. */
public final class SpigotUpdateCommandHandler implements SpigotSubcommandHandler {

  private final UpdateService updateService;
  private final SpigotCommandHelper helper;

  public SpigotUpdateCommandHandler(UpdateService updateService, SpigotCommandHelper helper) {
    this.updateService = Objects.requireNonNull(updateService, "updateService");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public void execute(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.update");
    if (arguments.length > 0 && "check".equalsIgnoreCase(arguments[0])) {
      helper.send(sender, "command.update.checking");
      updateService
          .checkNow("stable")
          .thenAccept(
              info -> {
                if (info == null || !info.updateAvailable()) {
                  helper.send(sender, "command.update.up-to-date");
                } else {
                  helper.send(
                      sender,
                      "command.update.available",
                      Map.of(
                          "current",
                          info.currentVersion(),
                          "latest",
                          info.latestVersion(),
                          "url",
                          info.downloadUrl()));
                }
              })
          .exceptionally(
              ex -> {
                helper.send(
                    sender,
                    "command.update.check-failed",
                    Map.of("reason", ex.getMessage() != null ? ex.getMessage() : "unknown"));
                return null;
              });
      return;
    }
    helper.send(sender, "command.update.usage");
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] arguments) {
    if (arguments.length == 1) {
      return helper.matches(arguments[0], "check");
    }
    return List.of();
  }
}
