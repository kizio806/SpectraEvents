package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Brigadier command handler for Asset Pipeline management (/event assets). */
public final class AssetCommandHandler {

  private final AssetPipelineService assetPipelineService;
  private final CommandText messages;
  private final RegionTaskScheduler regionScheduler;

  public AssetCommandHandler(
      RegionTaskScheduler regionScheduler,
      AssetPipelineService assetPipelineService,
      LocaleCatalog locales) {
    this.regionScheduler = regionScheduler;
    this.assetPipelineService = assetPipelineService;
    this.messages = new CommandText(locales);
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
            Commands.literal("import")
                .requires(s -> hasPerm(s, "spectraevents.admin.assets.import"))
                .then(
                    Commands.argument("file", StringArgumentType.string())
                        .suggests(this::suggestAssetIds)
                        .executes(this::importAsset)))
        .then(
            Commands.literal("info")
                .requires(s -> hasPerm(s, "spectraevents.admin.assets.info"))
                .then(
                    Commands.argument("id", StringArgumentType.string())
                        .suggests(this::suggestAssetIds)
                        .executes(this::assetInfo)))
        .then(
            Commands.literal("validate")
                .requires(s -> hasPerm(s, "spectraevents.admin.assets.validate"))
                .then(
                    Commands.argument("id", StringArgumentType.string())
                        .suggests(this::suggestAssetIds)
                        .executes(this::validateAsset)));
  }

  private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
      suggestAssetIds(
          CommandContext<CommandSourceStack> ctx,
          com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
    if (assetPipelineService != null) {
      for (String modelId : assetPipelineService.listModels()) {
        if (modelId.toLowerCase(Locale.ROOT).startsWith(remaining)) {
          builder.suggest(modelId);
        }
      }
    }
    return builder.buildFuture();
  }

  private boolean hasPerm(CommandSourceStack source, String perm) {
    CommandSender sender = source.getSender();
    return sender.hasPermission(perm)
        || sender.hasPermission("spectraevents.admin")
        || sender.isOp();
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private int buildAssets(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) {
      sender.sendMessage(messages.component("command.assets.unavailable", NamedTextColor.RED));
      return 0;
    }

    sender.sendMessage(messages.component("command.assets.build-started", NamedTextColor.YELLOW));

    assetPipelineService
        .buildAssetsAsync()
        .whenComplete(
            (ignored, failure) ->
                replyLater(
                    sender,
                    failure == null
                        ? messages.component("command.assets.build-complete", NamedTextColor.GREEN)
                        : messages.component(
                            "command.assets.build-failed",
                            NamedTextColor.RED,
                            Map.of("reason", messageFor(failure)))));
    return 1;
  }

  private int listAssets(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) return unavailable(sender);
    var models = assetPipelineService.listModels();
    int modelCount = models.size();
    sender.sendMessage(
        messages.component(
            "command.assets.list-title", NamedTextColor.YELLOW, Map.of("count", modelCount)));
    for (String m : models) {
      sender.sendMessage(
          messages.component("command.assets.entry", NamedTextColor.GRAY, Map.of("id", m)));
    }
    return 1;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private int importAsset(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) return unavailable(sender);
    String file = StringArgumentType.getString(ctx, "file");
    assetPipelineService
        .importFileAsync(file)
        .whenComplete(
            (ignored, failure) ->
                replyLater(
                    sender,
                    failure == null
                        ? messages.component(
                            "command.assets.imported", NamedTextColor.GREEN, Map.of("file", file))
                        : messages.component(
                            "command.assets.import-failed",
                            NamedTextColor.RED,
                            Map.of("reason", messageFor(failure)))));
    return 1;
  }

  private int assetInfo(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) return unavailable(sender);
    String id = StringArgumentType.getString(ctx, "id");
    SpectraAssetDocument doc = assetPipelineService.getModelInfo(id);
    if (doc == null) {
      sender.sendMessage(
          messages.component("command.assets.missing", NamedTextColor.RED, Map.of("id", id)));
      return 0;
    }
    sender.sendMessage(
        messages.component("command.assets.info-title", NamedTextColor.YELLOW, Map.of("id", id)));
    sender.sendMessage(
        messages.component(
            "command.assets.nodes", NamedTextColor.GRAY, Map.of("count", doc.nodes().size())));
    sender.sendMessage(
        messages.component(
            "command.assets.textures",
            NamedTextColor.GRAY,
            Map.of("count", doc.textures().size())));
    sender.sendMessage(
        messages.component(
            "command.assets.animations",
            NamedTextColor.GRAY,
            Map.of("count", doc.animations().size())));
    return 1;
  }

  private int validateAsset(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (assetPipelineService == null) return unavailable(sender);
    String id = StringArgumentType.getString(ctx, "id");
    boolean valid = assetPipelineService.validateModel(id);
    if (valid) {
      sender.sendMessage(
          messages.component("command.assets.valid", NamedTextColor.GREEN, Map.of("id", id)));
    } else {
      sender.sendMessage(
          messages.component("command.assets.invalid", NamedTextColor.RED, Map.of("id", id)));
    }
    return 1;
  }

  private String messageFor(Throwable failure) {
    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }

  private void replyLater(CommandSender sender, net.kyori.adventure.text.Component message) {
    if (sender instanceof Player player) {
      regionScheduler.executeFor(player, () -> player.sendMessage(message));
    } else {
      regionScheduler.executeGlobal(() -> sender.sendMessage(message));
    }
  }

  private int unavailable(CommandSender sender) {
    sender.sendMessage(messages.component("command.assets.unavailable", NamedTextColor.RED));
    return 0;
  }
}
