package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.CommandSender;

/** Handles `/spectraevents model` subcommands on Spigot. */
public final class SpigotModelCommandHandler implements SpigotSubcommandHandler {

  private final ModelDefinitionRegistry modelRegistry;
  private final SpigotCommandHelper helper;

  public SpigotModelCommandHandler(
      ModelDefinitionRegistry modelRegistry, SpigotCommandHelper helper) {
    this.modelRegistry = Objects.requireNonNull(modelRegistry, "modelRegistry");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public void execute(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.models");
    var all = modelRegistry.all();
    helper.send(sender, "command.models.title", Map.of("count", all.size()));
    for (var model : all) {
      helper.send(
          sender,
          "command.models.entry",
          Map.of(
              "id",
              model.id().value(),
              "parts",
              model.parts().size(),
              "interactions",
              model.interactions().size()));
    }
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] arguments) {
    return List.of();
  }
}
