package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles `/spectraevents location` subcommands on Spigot. */
public final class SpigotLocationCommandHandler implements SpigotSubcommandHandler {

  private final BukkitEventSettingsStore settingsStore;
  private final SpigotCommandHelper helper;

  public SpigotLocationCommandHandler(
      BukkitEventSettingsStore settingsStore, SpigotCommandHelper helper) {
    this.settingsStore = Objects.requireNonNull(settingsStore, "settingsStore");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public void execute(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.event.start");
    if (arguments.length == 0 || "help".equalsIgnoreCase(arguments[0])) {
      helper.send(sender, "command.location.usage");
      return;
    }
    String sub = arguments[0].toLowerCase(java.util.Locale.ROOT);
    switch (sub) {
      case "list" -> {
        Map<String, EventLocation> locations = settingsStore.locations();
        helper.send(sender, "command.location.list-title", Map.of("count", locations.size()));
        for (var entry : locations.entrySet()) {
          EventLocation loc = entry.getValue();
          helper.send(
              sender,
              "command.location.list-entry",
              Map.of(
                  "name",
                  entry.getKey(),
                  "world",
                  loc.world(),
                  "x",
                  String.format(java.util.Locale.ROOT, "%.1f", loc.x()),
                  "y",
                  String.format(java.util.Locale.ROOT, "%.1f", loc.y()),
                  "z",
                  String.format(java.util.Locale.ROOT, "%.1f", loc.z())));
        }
      }
      case "set" -> {
        if (!(sender instanceof Player player)) {
          helper.send(sender, "command.spigot.player-only");
          return;
        }
        if (arguments.length < 2) {
          helper.send(
              sender,
              "command.location.name-required",
              Map.of("example", "/spectraevents location set <name>"));
          return;
        }
        String name = arguments[1].toLowerCase(java.util.Locale.ROOT);
        Location loc = player.getLocation();
        String worldName = player.getWorld().getName();
        EventLocation eventLocation =
            new EventLocation(
                worldName, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        settingsStore.saveLocation(name, eventLocation);
        helper.send(sender, "command.location.saved", Map.of("name", name));
      }
      case "remove" -> {
        if (arguments.length < 2) {
          helper.send(
              sender,
              "command.location.name-required",
              Map.of("example", "/spectraevents location remove <name>"));
          return;
        }
        String name = arguments[1].toLowerCase(java.util.Locale.ROOT);
        if (settingsStore.removeLocation(name)) {
          helper.send(sender, "command.location.removed", Map.of("name", name));
        } else {
          helper.send(sender, "command.location.not-found", Map.of("name", name));
        }
      }
      default -> helper.send(sender, "command.location.usage");
    }
  }

  @Override
  public java.util.List<String> tabComplete(CommandSender sender, String[] arguments) {
    if (arguments.length == 1) {
      return helper.matches(arguments[0], "list", "set", "remove");
    }
    if (arguments.length == 2 && "remove".equalsIgnoreCase(arguments[0])) {
      return helper.matches(arguments[1], settingsStore.locations().keySet());
    }
    return java.util.List.of();
  }
}
