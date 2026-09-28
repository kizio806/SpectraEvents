package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

/** Read-only Brigadier command handler for registered model definitions. */
public final class ModelCommandHandler {
  private final ModelDefinitionRegistry modelRegistry;

  public ModelCommandHandler(ModelDefinitionRegistry modelRegistry) {
    this.modelRegistry = modelRegistry;
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
        Component.text("SpectraEvents 3D Models (", NamedTextColor.DARK_PURPLE)
            .append(Component.text(models.size(), NamedTextColor.LIGHT_PURPLE))
            .append(Component.text(" loaded):", NamedTextColor.DARK_PURPLE)));

    for (ModelDefinition model : models) {
      sender.sendMessage(
          Component.text(" - ", NamedTextColor.GRAY)
              .append(Component.text(model.id().value(), NamedTextColor.YELLOW))
              .append(
                  Component.text(
                      " ("
                          + model.parts().size()
                          + " parts, "
                          + model.interactions().size()
                          + " hitboxes)",
                      NamedTextColor.GRAY)));
    }
    return 1;
  }

  private int modelInfo(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String idStr = StringArgumentType.getString(ctx, "id");
    ModelId id = new ModelId(idStr);

    Optional<ModelDefinition> optModel = modelRegistry.get(id);
    if (optModel.isEmpty()) {
      sender.sendMessage(Component.text("Model '" + idStr + "' not found.", NamedTextColor.RED));
      return 0;
    }

    ModelDefinition model = optModel.get();
    sender.sendMessage(
        Component.text("Model Info: ", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
            .append(Component.text(model.id().value(), NamedTextColor.GOLD)));

    sender.sendMessage(
        Component.text(" Visual Parts (" + model.parts().size() + "):", NamedTextColor.YELLOW));
    for (ModelPartDefinition part : model.parts()) {
      String parentStr = part.parentPartId() != null ? part.parentPartId().value() : "root";
      sender.sendMessage(
          Component.text("   • ", NamedTextColor.GRAY)
              .append(Component.text(part.partId().value(), NamedTextColor.AQUA))
              .append(
                  Component.text(
                      " [" + part.type() + ", parent=" + parentStr + "]", NamedTextColor.GRAY)));
    }

    sender.sendMessage(
        Component.text(
            " Interaction Hitboxes (" + model.interactions().size() + "):", NamedTextColor.YELLOW));
    for (var interaction : model.interactions()) {
      sender.sendMessage(
          Component.text("   • ", NamedTextColor.GRAY)
              .append(Component.text(interaction.interactionId().value(), NamedTextColor.GREEN))
              .append(
                  Component.text(
                      " [" + interaction.width() + "x" + interaction.height() + "]",
                      NamedTextColor.GRAY)));
    }
    return 1;
  }
}
