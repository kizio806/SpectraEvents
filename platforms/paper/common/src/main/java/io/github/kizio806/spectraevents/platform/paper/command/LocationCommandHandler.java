package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Player-friendly named location commands for event starts. */
public final class LocationCommandHandler {
  private final PaperEventSettingsStore settingsStore;
  private final CommandText messages;

  public LocationCommandHandler(PaperEventSettingsStore settingsStore, LocaleCatalog locales) {
    this.settingsStore = settingsStore;
    this.messages = new CommandText(locales);
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
        .sendMessage(messages.component("command.location.usage", NamedTextColor.AQUA));
    return 1;
  }

  private int missingName(CommandContext<CommandSourceStack> ctx, String example) {
    ctx.getSource()
        .getSender()
        .sendMessage(
            messages.component(
                "command.location.name-required", NamedTextColor.RED, Map.of("example", example)));
    return 0;
  }

  private int set(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (!(sender instanceof Player player)) {
      sender.sendMessage(messages.component("command.location.player-only", NamedTextColor.RED));
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
      sender.sendMessage(messages.component("command.location.saved", NamedTextColor.GREEN));
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          messages.component(
              "command.location.save-failed",
              NamedTextColor.RED,
              Map.of("reason", exception.getMessage())));
      return 0;
    }
  }

  private int list(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (settingsStore.locations().isEmpty()) {
      sender.sendMessage(messages.component("command.location.none", NamedTextColor.YELLOW));
      return 1;
    }
    sender.sendMessage(messages.component("command.location.list-title", NamedTextColor.AQUA));
    settingsStore
        .locations()
        .forEach(
            (name, location) ->
                sender.sendMessage(
                    messages.component(
                        "command.location.entry",
                        NamedTextColor.GRAY,
                        Map.of(
                            "name", name,
                            "world", location.world(),
                            "x", Math.round(location.x()),
                            "y", Math.round(location.y()),
                            "z", Math.round(location.z())))));
    return 1;
  }

  private int remove(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    try {
      String name = StringArgumentType.getString(ctx, "name");
      if (!settingsStore.removeLocation(name)) {
        sender.sendMessage(messages.component("command.location.missing", NamedTextColor.RED));
        return 0;
      }
      sender.sendMessage(messages.component("command.location.removed", NamedTextColor.GREEN));
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          messages.component(
              "command.location.remove-failed",
              NamedTextColor.RED,
              Map.of("reason", exception.getMessage())));
      return 0;
    }
  }
}
