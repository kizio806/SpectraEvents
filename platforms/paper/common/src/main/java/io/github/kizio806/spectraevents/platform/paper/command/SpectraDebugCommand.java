package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.InvalidPhaseTransitionException;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
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
    return Commands.literal("eventdebug")
        .requires(source -> source.getSender().hasPermission("spectraevents.dev"))
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
                                .executes(this::eventInfo))));
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
}
