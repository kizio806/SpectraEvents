package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

/** Read-only Brigadier command handler for registered model definitions. */
public final class ModelCommandHandler {
  private final ModelDefinitionRegistry modelRegistry;
  private final CommandText messages;

  public ModelCommandHandler(ModelDefinitionRegistry modelRegistry, LocaleCatalog locales) {
    this.modelRegistry = modelRegistry;
    this.messages = new CommandText(locales);
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("model")
        .requires(s -> hasPerm(s, "spectraevents.admin.model"))
        .then(
            Commands.literal("list")
                .requires(s -> hasPerm(s, "spectraevents.admin.model.list"))
                .executes(this::listModels))
        .then(
            Commands.literal("info")
                .requires(s -> hasPerm(s, "spectraevents.admin.model.info"))
                .then(
                    Commands.argument("id", StringArgumentType.string())
                        .suggests(this::suggestModelIds)
                        .executes(this::modelInfo)));
  }

  private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
      suggestModelIds(
          CommandContext<CommandSourceStack> ctx,
          com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
    if (modelRegistry != null) {
      for (ModelDefinition model : modelRegistry.all()) {
        String id = model.id().value();
        if (id.toLowerCase(Locale.ROOT).startsWith(remaining)) {
          builder.suggest(id);
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

  private int listModels(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    Collection<ModelDefinition> models = modelRegistry.all();

    sender.sendMessage(
        messages.component(
            "command.models.list-title",
            NamedTextColor.DARK_PURPLE,
            Map.of("count", models.size())));

    for (ModelDefinition model : models) {
      sender.sendMessage(
          messages.component(
              "command.models.entry",
              NamedTextColor.GRAY,
              Map.of(
                  "id", model.id().value(),
                  "parts", model.parts().size(),
                  "hitboxes", model.interactions().size())));
    }
    return 1;
  }

  private int modelInfo(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String idStr = StringArgumentType.getString(ctx, "id");
    ModelId id = new ModelId(idStr);

    Optional<ModelDefinition> optModel = modelRegistry.get(id);
    if (optModel.isEmpty()) {
      sender.sendMessage(
          messages.component("command.models.missing", NamedTextColor.RED, Map.of("id", idStr)));
      return 0;
    }

    ModelDefinition model = optModel.get();
    sender.sendMessage(
        messages
            .component(
                "command.models.info-title",
                NamedTextColor.DARK_PURPLE,
                Map.of("id", model.id().value()))
            .decorate(TextDecoration.BOLD));

    sender.sendMessage(
        messages.component(
            "command.models.parts-title",
            NamedTextColor.YELLOW,
            Map.of("count", model.parts().size())));
    for (ModelPartDefinition part : model.parts()) {
      String parentStr = part.parentPartId() != null ? part.parentPartId().value() : "root";
      sender.sendMessage(
          messages.component(
              "command.models.part-entry",
              NamedTextColor.GRAY,
              Map.of("id", part.partId().value(), "type", part.type(), "parent", parentStr)));
    }

    sender.sendMessage(
        messages.component(
            "command.models.hitboxes-title",
            NamedTextColor.YELLOW,
            Map.of("count", model.interactions().size())));
    for (var interaction : model.interactions()) {
      sender.sendMessage(
          messages.component(
              "command.models.hitbox-entry",
              NamedTextColor.GRAY,
              Map.of(
                  "id", interaction.interactionId().value(),
                  "width", interaction.width(),
                  "height", interaction.height())));
    }
    return 1;
  }
}
