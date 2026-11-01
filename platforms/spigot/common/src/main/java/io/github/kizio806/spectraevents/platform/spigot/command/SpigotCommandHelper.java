package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Shared helper for Spigot command permission validation, localization, and tab completion. */
public final class SpigotCommandHelper {

  private final LocaleCatalog locales;

  public SpigotCommandHelper(LocaleCatalog locales) {
    this.locales = Objects.requireNonNull(locales, "locales");
  }

  public LocaleCatalog locales() {
    return locales;
  }

  public void send(CommandSender sender, String key) {
    sender.sendMessage(locales.message(key));
  }

  public void send(CommandSender sender, String key, Map<String, ?> placeholders) {
    sender.sendMessage(locales.message(key, placeholders));
  }

  public void require(CommandSender sender, String permission) {
    if (!sender.hasPermission(permission)
        && !sender.hasPermission("spectraevents.admin")
        && !sender.isOp()) {
      throw new IllegalStateException("Missing permission: " + permission);
    }
  }

  public void requireArguments(String[] arguments, int count, String usage) {
    if (arguments.length < count) {
      throw new IllegalArgumentException("Usage: " + usage);
    }
  }

  public String messageFor(Throwable failure) {
    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }

  public Location eventLocation(CommandSender sender) {
    if (sender instanceof Player player) {
      return player.getLocation();
    }
    if (Bukkit.getWorlds().isEmpty()) {
      throw new IllegalStateException("No loaded world is available for the event location");
    }
    return Bukkit.getWorlds().getFirst().getSpawnLocation();
  }

  public List<String> matches(String prefix, String... candidates) {
    String normalized = prefix.toLowerCase(Locale.ROOT);
    return Arrays.stream(candidates)
        .filter(candidate -> candidate.startsWith(normalized))
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
