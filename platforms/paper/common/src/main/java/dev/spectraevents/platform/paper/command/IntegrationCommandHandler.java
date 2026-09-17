package dev.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.spectraevents.application.integration.IntegrationRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Command handler for integrations list subcommand. */
public final class IntegrationCommandHandler {
  private final IntegrationRegistry integrationRegistry;

  public IntegrationCommandHandler(IntegrationRegistry integrationRegistry) {
    this.integrationRegistry = integrationRegistry;
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("integrations")
        .requires(s -> s.getSender().hasPermission("spectraevents.integrations"))
        .executes(this::integrations);
  }

  public int integrations(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    var all = integrationRegistry.getAll();
    sender.sendMessage(
        Component.text("Plugin Integrations (" + all.size() + "):", NamedTextColor.AQUA));
    for (var entry : all.entrySet()) {
      var info = entry.getValue();
      NamedTextColor color =
          switch (info.state()) {
            case ENABLED -> NamedTextColor.GREEN;
            case DISABLED -> NamedTextColor.YELLOW;
            case MISSING -> NamedTextColor.GRAY;
          };
      sender.sendMessage(
          Component.text(" - " + info.name() + ": ", NamedTextColor.WHITE)
              .append(Component.text(info.state().name(), color))
              .append(Component.text(" (" + info.details() + ")", NamedTextColor.DARK_GRAY)));
    }
    return 1;
  }
}
