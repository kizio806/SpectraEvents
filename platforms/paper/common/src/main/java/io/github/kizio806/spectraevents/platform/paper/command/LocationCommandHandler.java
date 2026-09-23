package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Player-friendly named location commands for event starts. */
public final class LocationCommandHandler {
  private final PaperEventSettingsStore settingsStore;

  public LocationCommandHandler(PaperEventSettingsStore settingsStore) {
    this.settingsStore = settingsStore;
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("location")
        .requires(source -> source.getSender().hasPermission("spectraevents.event.start"))
        .executes(this::usage)
        .then(Commands.literal("list").executes(this::list))
        .then(
            Commands.literal("set")
                .executes(ctx -> missingName(ctx, "/spectraevents location set <name>"))
                .then(Commands.argument("name", StringArgumentType.word()).executes(this::set)))
        .then(
            Commands.literal("remove")
                .executes(ctx -> missingName(ctx, "/spectraevents location remove <name>"))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .suggests(
                            (ctx, builder) -> {
                              settingsStore.locations().keySet().forEach(builder::suggest);
                              return builder.buildFuture();
                            })
                        .executes(this::remove)));
  }

  private int usage(CommandContext<CommandSourceStack> ctx) {
    ctx.getSource()
        .getSender()
        .sendMessage(
            Component.text(
                "Use /spectraevents location set <name>, list, or remove <name>.",
                NamedTextColor.AQUA));
    return 1;
  }

  private int missingName(CommandContext<CommandSourceStack> ctx, String example) {
    ctx.getSource()
        .getSender()
        .sendMessage(
            Component.text("A location name is required. Example: " + example, NamedTextColor.RED));
    return 0;
  }

  private int set(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          Component.text("Only players can save their current location.", NamedTextColor.RED));
      return 0;
    }
    try {
      var location = player.getLocation();
      settingsStore.saveLocation(
          StringArgumentType.getString(ctx, "name"),
          new EventLocation(
              location.getWorld().getName(),
              location.getX(),
              location.getY(),
              location.getZ(),
              location.getYaw(),
              location.getPitch()));
      sender.sendMessage(Component.text("Event location saved.", NamedTextColor.GREEN));
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          Component.text("Location was not saved: " + exception.getMessage(), NamedTextColor.RED));
      return 0;
    }
  }

  private int list(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (settingsStore.locations().isEmpty()) {
      sender.sendMessage(
          Component.text("No named event locations are saved.", NamedTextColor.YELLOW));
      return 1;
    }
    sender.sendMessage(Component.text("Saved event locations:", NamedTextColor.AQUA));
    settingsStore
        .locations()
        .forEach(
            (name, location) ->
                sender.sendMessage(
                    Component.text(
                        " - "
                            + name
                            + ": "
                            + location.world()
                            + " "
                            + Math.round(location.x())
                            + ", "
                            + Math.round(location.y())
                            + ", "
                            + Math.round(location.z()),
                        NamedTextColor.GRAY)));
    return 1;
  }

  private int remove(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    try {
      String name = StringArgumentType.getString(ctx, "name");
      if (!settingsStore.removeLocation(name)) {
        sender.sendMessage(
            Component.text("That named location does not exist.", NamedTextColor.RED));
        return 0;
      }
      sender.sendMessage(Component.text("Event location removed.", NamedTextColor.GREEN));
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          Component.text(
              "Location was not removed: " + exception.getMessage(), NamedTextColor.RED));
      return 0;
    }
  }
}
