package dev.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.spectraevents.application.config.loader.DefinitionLoadResult;
import dev.spectraevents.application.config.registry.EventDefinitionRegistry;
import dev.spectraevents.application.config.registry.RegisteredEventDefinition;
import dev.spectraevents.application.config.validation.ValidationDiagnostic;
import dev.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Command handler for definition subcommands (list, reload, validate). */
public final class DefinitionCommandHandler {
  private final EventDefinitionRegistry definitionRegistry;
  private final PaperDefinitionConfigBootstrap configBootstrap;

  public DefinitionCommandHandler(
      EventDefinitionRegistry definitionRegistry, PaperDefinitionConfigBootstrap configBootstrap) {
    this.definitionRegistry = definitionRegistry;
    this.configBootstrap = configBootstrap;
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("definition")
        .then(
            Commands.literal("list")
                .requires(s -> s.getSender().hasPermission("spectraevents.definition.list"))
                .executes(this::definitionList))
        .then(
            Commands.literal("reload")
                .requires(s -> s.getSender().hasPermission("spectraevents.definition.reload"))
                .executes(this::definitionReload))
        .then(
            Commands.literal("validate")
                .requires(s -> s.getSender().hasPermission("spectraevents.definition.validate"))
                .executes(this::definitionValidate));
  }

  private int definitionList(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    Collection<RegisteredEventDefinition> all = definitionRegistry.getAll();
    sender.sendMessage(
        Component.text("Registered Definitions (" + all.size() + "):", NamedTextColor.GOLD));
    for (RegisteredEventDefinition reg : all) {
      sender.sendMessage(
          Component.text(" - ID: ", NamedTextColor.GRAY)
              .append(Component.text(reg.definition().id().value(), NamedTextColor.GREEN))
              .append(Component.text(" Source: ", NamedTextColor.GRAY))
              .append(Component.text(reg.sourceFile(), NamedTextColor.YELLOW)));
    }
    return 1;
  }

  private int definitionReload(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    try {
      DefinitionLoadResult result = configBootstrap.reloadFromDisk();
      configBootstrap.logLoadResult(result);

      sender.sendMessage(
          Component.text(
              "Reload complete: "
                  + result.loaded().size()
                  + " loaded, "
                  + result.failures().size()
                  + " failed.",
              result.failures().isEmpty() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Failed to reload definitions: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }

  private int definitionValidate(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    try {
      DefinitionLoadResult result = configBootstrap.loadFromDisk();
      if (result.failures().isEmpty()) {
        sender.sendMessage(
            Component.text(
                "Validation passed! All " + result.loaded().size() + " files valid.",
                NamedTextColor.GREEN));
      } else {
        sender.sendMessage(
            Component.text(
                "Validation failed with " + result.failures().size() + " file errors:",
                NamedTextColor.RED));
        for (var failure : result.failures()) {
          sender.sendMessage(
              Component.text(" - " + failure.sourceFile() + ":", NamedTextColor.YELLOW));
          for (ValidationDiagnostic diag : failure.diagnostics()) {
            sender.sendMessage(
                Component.text("   [" + diag.code() + "] " + diag.message(), NamedTextColor.RED));
          }
        }
      }
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Validation check failed: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }
}
