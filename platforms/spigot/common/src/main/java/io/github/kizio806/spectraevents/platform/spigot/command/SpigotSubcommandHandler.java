package io.github.kizio806.spectraevents.platform.spigot.command;

import java.util.List;
import org.bukkit.command.CommandSender;

/** Contract for modular Spigot subcommand execution and completion. */
public interface SpigotSubcommandHandler {

  boolean execute(CommandSender sender, String[] arguments);

  List<String> tabComplete(CommandSender sender, String[] arguments);
}
