package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Brigadier command handler for Asset Pipeline management (/spectra assets). */
public final class AssetCommandHandler {

  private final AssetPipelineService assetPipelineService;

  public AssetCommandHandler(AssetPipelineService assetPipelineService) {
    this.assetPipelineService = assetPipelineService;
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("assets")
        .requires(s -> hasPerm(s, "spectraevents.admin.assets"))
        .then(
            Commands.literal("build")
                .requires(s -> hasPerm(s, "spectraevents.admin.assets.build"))
                .executes(this::buildAssets));
  }

  private boolean hasPerm(CommandSourceStack source, String perm) {
    CommandSender sender = source.getSender();
    return sender.hasPermission(perm)
        || sender.hasPermission("spectraevents.admin")
        || sender.isOp();
  }

  private int buildAssets(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) {
      sender.sendMessage(
          Component.text("Asset Pipeline is not configured on this server.", NamedTextColor.RED));
      return 0;
    }

    sender.sendMessage(Component.text("Starting Asset Pipeline build...", NamedTextColor.YELLOW));

    // We execute synchronously for simplicity in this version, but it should ideally be async
    try {
      assetPipelineService.buildAssets();
      sender.sendMessage(
          Component.text("Asset Pipeline build completed successfully.", NamedTextColor.GREEN));
      return 1;
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Asset Pipeline build failed: " + e.getMessage(), NamedTextColor.RED));
      return 0;
    }
  }
}
