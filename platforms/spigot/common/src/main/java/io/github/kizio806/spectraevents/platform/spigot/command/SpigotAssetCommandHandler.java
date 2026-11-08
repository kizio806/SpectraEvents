package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import java.util.Objects;
import org.bukkit.command.CommandSender;

/** Handles `/spectraevents asset` subcommands on Spigot. */
public final class SpigotAssetCommandHandler implements SpigotSubcommandHandler {

  private final AssetPipelineService assetPipeline;
  private final SpigotCommandHelper helper;

  public SpigotAssetCommandHandler(AssetPipelineService assetPipeline, SpigotCommandHelper helper) {
    this.assetPipeline = Objects.requireNonNull(assetPipeline, "assetPipeline");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public void execute(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.assets");
    if (arguments.length > 0 && "build".equalsIgnoreCase(arguments[0])) {
      try {
        assetPipeline.buildAssets();
        helper.send(sender, "command.assets.build-complete");
      } catch (Exception e) {
        helper.send(
            sender,
            "command.spigot.failed",
            java.util.Map.of("reason", e.getMessage() != null ? e.getMessage() : "unknown error"));
      }
      return;
    }
    helper.send(sender, "command.assets.usage");
  }

  @Override
  public java.util.List<String> tabComplete(CommandSender sender, String[] arguments) {
    if (arguments.length == 1) {
      return helper.matches(arguments[0], "build");
    }
    return java.util.List.of();
  }
}
