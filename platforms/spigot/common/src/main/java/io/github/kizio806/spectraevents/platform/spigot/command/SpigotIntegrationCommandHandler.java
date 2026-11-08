package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.CommandSender;

/** Handles `/spectraevents integrations` subcommands on Spigot. */
public final class SpigotIntegrationCommandHandler implements SpigotSubcommandHandler {

  private final IntegrationRegistry integrationRegistry;
  private final SpigotCommandHelper helper;

  public SpigotIntegrationCommandHandler(
      IntegrationRegistry integrationRegistry, SpigotCommandHelper helper) {
    this.integrationRegistry = Objects.requireNonNull(integrationRegistry, "integrationRegistry");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public void execute(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.integrations");
    var all = integrationRegistry.getAll();
    helper.send(sender, "command.integrations.title", Map.of("count", all.size()));
    for (var entry : all.entrySet()) {
      var info = entry.getValue();
      helper.send(
          sender,
          "command.integrations.entry",
          Map.of("name", entry.getKey(), "state", info.state().name(), "details", info.details()));
    }
  }

  @Override
  public java.util.List<String> tabComplete(CommandSender sender, String[] arguments) {
    return java.util.List.of();
  }
}
