package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
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
                .executes(this::buildAssets))
        .then(
            Commands.literal("list")
                .requires(s -> hasPerm(s, "spectraevents.admin.assets.list"))
                .executes(this::listAssets))
        .then(
            Commands.literal("clean")
                .requires(s -> hasPerm(s, "spectraevents.admin.assets.clean"))
                .executes(this::cleanAssets))
        .then(
            Commands.literal("import")
                .requires(s -> hasPerm(s, "spectraevents.admin.assets.import"))
                .then(
                    Commands.argument("file", StringArgumentType.string())
                        .executes(this::importAsset)))
        .then(
            Commands.literal("info")
                .requires(s -> hasPerm(s, "spectraevents.admin.assets.info"))
                .then(
                    Commands.argument("id", StringArgumentType.string()).executes(this::assetInfo)))
        .then(
            Commands.literal("validate")
                .requires(s -> hasPerm(s, "spectraevents.admin.assets.validate"))
                .then(
                    Commands.argument("id", StringArgumentType.string())
                        .executes(this::validateAsset)));
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

  private int listAssets(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) return 0;
    var models = assetPipelineService.listModels();
    sender.sendMessage(
        Component.text("Compiled Assets (" + models.size() + "):", NamedTextColor.YELLOW));
    for (String m : models) {
      sender.sendMessage(Component.text("- " + m, NamedTextColor.GRAY));
    }
    return 1;
  }

  private int cleanAssets(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) return 0;
    assetPipelineService.clean();
    sender.sendMessage(
        Component.text("Asset generated cache and files cleaned.", NamedTextColor.GREEN));
    return 1;
  }

  private int importAsset(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) return 0;
    String file = StringArgumentType.getString(ctx, "file");
    try {
      assetPipelineService.importFile(file);
      sender.sendMessage(Component.text("Successfully imported " + file, NamedTextColor.GREEN));
      return 1;
    } catch (Exception e) {
      sender.sendMessage(Component.text("Import failed: " + e.getMessage(), NamedTextColor.RED));
      return 0;
    }
  }

  private int assetInfo(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) return 0;
    String id = StringArgumentType.getString(ctx, "id");
    SpectraAssetDocument doc = assetPipelineService.getModelInfo(id);
    if (doc == null) {
      sender.sendMessage(Component.text("Asset not found: " + id, NamedTextColor.RED));
      return 0;
    }
    sender.sendMessage(Component.text("Asset Info: " + id, NamedTextColor.YELLOW));
    sender.sendMessage(Component.text("Nodes: " + doc.nodes().size(), NamedTextColor.GRAY));
    sender.sendMessage(Component.text("Textures: " + doc.textures().size(), NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text("Animations: " + doc.animations().size(), NamedTextColor.GRAY));
    return 1;
  }

  private int validateAsset(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) return 0;
    String id = StringArgumentType.getString(ctx, "id");
    boolean valid = assetPipelineService.validateModel(id);
    if (valid) {
      sender.sendMessage(Component.text("Asset " + id + " is valid.", NamedTextColor.GREEN));
    } else {
      sender.sendMessage(
          Component.text("Asset " + id + " is invalid or missing.", NamedTextColor.RED));
    }
    return 1;
  }
}
