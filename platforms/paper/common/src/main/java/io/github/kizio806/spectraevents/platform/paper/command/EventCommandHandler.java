package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command handler for event management subcommands. */
public final class EventCommandHandler {
  private final EventOrchestrationService orchestrationService;
  private final EventInstanceRepository instanceRepository;

  public EventCommandHandler(
      EventOrchestrationService orchestrationService, EventInstanceRepository instanceRepository) {
    this.orchestrationService = orchestrationService;
    this.instanceRepository = instanceRepository;
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("event")
        .then(
            Commands.literal("list")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.list"))
                .executes(this::eventList))
        .then(
            Commands.literal("start")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.start"))
                .then(
                    Commands.argument("definition", StringArgumentType.word())
                        .executes(this::eventStart)))
        .then(
            Commands.literal("stop")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.stop"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .executes(this::eventStop)))
        .then(
            Commands.literal("cancel")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.cancel"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .executes(this::eventCancel)));
  }

  private int eventList(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    Collection<EventInstance> instances = instanceRepository.findAll();
    sender.sendMessage(
        Component.text("Active Event Instances (" + instances.size() + "):", NamedTextColor.GOLD));
    for (EventInstance inst : instances) {
      sender.sendMessage(
          Component.text(" - ID: ", NamedTextColor.GRAY)
              .append(Component.text(inst.id().toString(), NamedTextColor.AQUA))
              .append(Component.text(" Definition: ", NamedTextColor.GRAY))
              .append(Component.text(inst.definitionId().value(), NamedTextColor.YELLOW))
              .append(Component.text(" State: ", NamedTextColor.GRAY))
              .append(Component.text(inst.state().name(), NamedTextColor.GREEN)));
    }
    return 1;
  }

  private int eventStart(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String defIdStr = StringArgumentType.getString(ctx, "definition");

    try {
      Object platformLocation = null;
      if (sender instanceof Player player) {
        platformLocation = player.getLocation();
      }
      var instance = orchestrationService.startDefinition(defIdStr, platformLocation);
      sender.sendMessage(
          Component.text(
              "Started event instance "
                  + instance.id().toString()
                  + " from definition '"
                  + defIdStr
                  + "'",
              NamedTextColor.GREEN));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Failed to start event: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }

  private int eventStop(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String instIdStr = StringArgumentType.getString(ctx, "instance");

    try {
      orchestrationService.cancelEvent(instIdStr);
      sender.sendMessage(
          Component.text("Stopped event instance " + instIdStr, NamedTextColor.GREEN));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Failed to stop event: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }

  private int eventCancel(CommandContext<CommandSourceStack> ctx) {
    return eventStop(ctx);
  }
}
