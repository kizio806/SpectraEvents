package dev.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.spectraevents.application.service.EventOrchestrationService;
import dev.spectraevents.core.event.runtime.EventInstance;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.core.event.runtime.InvalidPhaseTransitionException;
import dev.spectraevents.core.visual.model.ModelDefinition;
import dev.spectraevents.core.visual.model.ModelId;
import dev.spectraevents.core.visual.model.ModelPartDefinition;
import dev.spectraevents.core.visual.model.Transform;
import dev.spectraevents.platform.paper.render.PaperModelRenderer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Debug and testing subcommands for SpectraEvents development in Paper platform family. */
public final class SpectraDebugCommand {
  private final EventOrchestrationService orchestrationService;
  private final PaperModelRenderer renderer;

  public SpectraDebugCommand(
      EventOrchestrationService orchestrationService, PaperModelRenderer renderer) {
    this.orchestrationService = orchestrationService;
    this.renderer = renderer;
  }

  public LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
    return Commands.literal("spectradebug")
        .requires(source -> source.getSender().hasPermission("spectra.dev"))
        .then(
            Commands.literal("event")
                .requires(s -> s.getSender().hasPermission("spectraevents.debug.event"))
                .then(
                    Commands.literal("next")
                        .then(
                            Commands.argument("instance", StringArgumentType.word())
                                .executes(this::nextPhase)))
                .then(
                    Commands.literal("info")
                        .then(
                            Commands.argument("instance", StringArgumentType.word())
                                .executes(this::eventInfo))))
        .then(
            Commands.literal("model")
                .then(
                    Commands.literal("spawn")
                        .then(
                            Commands.argument("model_id", StringArgumentType.word())
                                .executes(this::spawnModel)))
                .then(
                    Commands.literal("despawn")
                        .then(
                            Commands.argument("instance_id", StringArgumentType.word())
                                .executes(this::despawnModel))));
  }

  private int nextPhase(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String instanceIdStr = StringArgumentType.getString(ctx, "instance");
    try {
      orchestrationService.transitionPhase(instanceIdStr);
      sender.sendMessage(
          Component.text("Advanced phase for instance " + instanceIdStr, NamedTextColor.GREEN));
    } catch (InvalidPhaseTransitionException e) {
      sender.sendMessage(
          Component.text("Invalid phase transition: " + e.getMessage(), NamedTextColor.RED));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Error advancing phase: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }

  private int eventInfo(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String instanceIdStr = StringArgumentType.getString(ctx, "instance");
    try {
      EventInstance instance = orchestrationService.getEventInfo(instanceIdStr);
      sender.sendMessage(
          Component.text("Instance " + instance.id().toString() + ":", NamedTextColor.AQUA));
      sender.sendMessage(
          Component.text("  Definition: ", NamedTextColor.GRAY)
              .append(Component.text(instance.definitionId().value(), NamedTextColor.WHITE)));
      sender.sendMessage(
          Component.text("  State: ", NamedTextColor.GRAY)
              .append(Component.text(instance.state().name(), NamedTextColor.WHITE)));
      sender.sendMessage(
          Component.text("  Current Phase: ", NamedTextColor.GRAY)
              .append(
                  Component.text(
                      instance.currentPhase().map(p -> p.value()).orElse("none"),
                      NamedTextColor.WHITE)));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Failed to get info: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }

  private int spawnModel(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (!(sender instanceof org.bukkit.entity.Player player)) {
      sender.sendMessage(Component.text("Player only command.", NamedTextColor.RED));
      return 1;
    }
    String modelIdStr = StringArgumentType.getString(ctx, "model_id");
    ModelDefinition def =
        new ModelDefinition(
            new ModelId(modelIdStr),
            List.of(
                new ModelPartDefinition(
                    "core", new Transform(0, 0, 0, 0, 0, 0, 1, 1, 1), "minecraft:diamond_block")));

    EventInstanceId instId = EventInstanceId.generate();
    boolean spawned = renderer.spawn(instId, def, player.getLocation());
    if (spawned) {
      sender.sendMessage(
          Component.text(
              "Spawned model " + modelIdStr + " for instance " + instId, NamedTextColor.GREEN));
    } else {
      sender.sendMessage(Component.text("Failed to spawn model.", NamedTextColor.RED));
    }
    return 1;
  }

  private int despawnModel(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String instIdStr = StringArgumentType.getString(ctx, "instance_id");
    try {
      EventInstanceId instId = new EventInstanceId(UUID.fromString(instIdStr));
      renderer.remove(instId);
      sender.sendMessage(
          Component.text("Despawned model for instance " + instIdStr, NamedTextColor.GREEN));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Invalid instance ID: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }
}
