package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Brigadier command handler for 3D model management (/spectra model). */
public final class ModelCommandHandler {
  private final ModelDefinitionRegistry modelRegistry;
  private final ModelRuntimeService modelRuntimeService;

  public ModelCommandHandler(
      ModelDefinitionRegistry modelRegistry, ModelRuntimeService modelRuntimeService) {
    this.modelRegistry = modelRegistry;
    this.modelRuntimeService = modelRuntimeService;
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
                    Commands.argument("id", StringArgumentType.string()).executes(this::modelInfo)))
        .then(
            Commands.literal("spawn")
                .requires(s -> hasPerm(s, "spectraevents.admin.model.spawn"))
                .then(
                    Commands.argument("id", StringArgumentType.string())
                        .executes(this::spawnModel)))
        .then(
            Commands.literal("remove")
                .requires(s -> hasPerm(s, "spectraevents.admin.model.remove"))
                .then(
                    Commands.argument("runtimeId", StringArgumentType.string())
                        .executes(this::removeModel)));
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

  private int spawnModel(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          Component.text("This command can only be executed by a player.", NamedTextColor.RED));
      return 0;
    }

    if (modelRuntimeService == null) {
      sender.sendMessage(
          Component.text("3D Model Runtime Service is not available.", NamedTextColor.RED));
      return 0;
    }

    String idStr = StringArgumentType.getString(ctx, "id");
    ModelId id = new ModelId(idStr);

    if (!modelRegistry.contains(id)) {
      sender.sendMessage(
          Component.text("Model definition '" + idStr + "' not found.", NamedTextColor.RED));
      return 0;
    }

    Location loc = player.getLocation();
    ModelAnchor anchor =
        ModelAnchor.of(
            loc.getWorld().getName(),
            loc.getX(),
            loc.getY(),
            loc.getZ(),
            loc.getYaw(),
            loc.getPitch());

    try {
      RenderedModelHandle handle = modelRuntimeService.spawnModel(id, anchor, null);
      if (handle != null) {
        player.sendMessage(
            Component.text("Spawned 3D model preview '", NamedTextColor.GREEN)
                .append(Component.text(idStr, NamedTextColor.YELLOW))
                .append(Component.text("' with runtime ID: ", NamedTextColor.GREEN))
                .append(Component.text(handle.runtimeId().value(), NamedTextColor.GOLD)));
        return 1;
      } else {
        player.sendMessage(Component.text("Failed to spawn 3D model preview.", NamedTextColor.RED));
        return 0;
      }
    } catch (Exception e) {
      player.sendMessage(
          Component.text("Error spawning model: " + e.getMessage(), NamedTextColor.RED));
      return 0;
    }
  }

  private int removeModel(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (modelRuntimeService == null) {
      sender.sendMessage(
          Component.text("3D Model Runtime Service is not available.", NamedTextColor.RED));
      return 0;
    }

    String runtimeIdStr = StringArgumentType.getString(ctx, "runtimeId");
    ModelRuntimeId runtimeId = ModelRuntimeId.of(runtimeIdStr);

    boolean removed = modelRuntimeService.removeModel(runtimeId);
    if (removed) {
      sender.sendMessage(
          Component.text(
              "Removed model runtime instance '" + runtimeIdStr + "'.", NamedTextColor.GREEN));
      return 1;
    } else {
      sender.sendMessage(
          Component.text(
              "Model runtime instance '" + runtimeIdStr + "' not found.", NamedTextColor.RED));
      return 0;
    }
  }
}
