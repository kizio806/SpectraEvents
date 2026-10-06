package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.validation.EventReferenceValidator;
import io.github.kizio806.spectraevents.application.validation.OperationalReadinessValidator;
import io.github.kizio806.spectraevents.platform.paper.asset.delivery.PaperResourcePackDeliveryBootstrap;
import io.github.kizio806.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/** Command handler for definition subcommands (list, reload, validate). */
public final class DefinitionCommandHandler {
  private final EventDefinitionRegistry definitionRegistry;
  private final PaperDefinitionConfigBootstrap configBootstrap;
  private final io.github.kizio806.spectraevents.application.asset.AssetPipelineService
      assetPipeline;
  private final Plugin plugin;
  private final IntegrationRegistry integrations;
  private final CommandText messages;
  private final EventReferenceValidator referenceValidator;

  public DefinitionCommandHandler(
      EventDefinitionRegistry definitionRegistry,
      PaperDefinitionConfigBootstrap configBootstrap,
      io.github.kizio806.spectraevents.application.SpectraEventsApplication application,
      Plugin plugin,
      IntegrationRegistry integrations,
      LocaleCatalog locales) {
    this.definitionRegistry = definitionRegistry;
    this.configBootstrap = configBootstrap;
    this.assetPipeline = application.assetPipelineService();
    this.plugin = plugin;
    this.integrations = integrations;
    this.messages = new CommandText(locales);
    this.referenceValidator =
        new EventReferenceValidator(
            application.modelDefinitionRegistry(), application.animationDefinitionRegistry());
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
                .executes(this::definitionReload)
                .then(
                    Commands.argument(
                            "definitionId",
                            com.mojang.brigadier.arguments.StringArgumentType.string())
                        .suggests(this::suggestDefinitionIds)
                        .executes(this::definitionReload)))
        .then(
            Commands.literal("validate")
                .requires(s -> s.getSender().hasPermission("spectraevents.definition.validate"))
                .executes(this::definitionValidate)
                .then(
                    Commands.argument(
                            "definitionId",
                            com.mojang.brigadier.arguments.StringArgumentType.string())
                        .suggests(this::suggestDefinitionIds)
                        .executes(this::definitionValidate)));
  }

  public LiteralArgumentBuilder<CommandSourceStack> buildRootValidationCommand() {
    return Commands.literal("validate")
        .requires(s -> s.getSender().hasPermission("spectraevents.definition.validate"))
        .executes(this::definitionValidate);
  }

  private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
      suggestDefinitionIds(
          CommandContext<CommandSourceStack> ctx,
          com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
    if ("all".startsWith(remaining)) {
      builder.suggest("all");
    }
    if (definitionRegistry != null) {
      for (RegisteredEventDefinition reg : definitionRegistry.getAll()) {
        String id = reg.definition().id().value();
        if (id.toLowerCase(Locale.ROOT).startsWith(remaining)) {
          builder.suggest(id);
        }
      }
    }
    return builder.buildFuture();
  }

  private int definitionList(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    Collection<RegisteredEventDefinition> all = definitionRegistry.getAll();
    sender.sendMessage(
        messages.component(
            "command.definitions.title",
            NamedTextColor.GOLD,
            java.util.Map.of("count", all.size())));
    for (RegisteredEventDefinition reg : all) {
      sender.sendMessage(
          messages
              .component("command.definitions.entry-id", NamedTextColor.GRAY)
              .append(Component.text(reg.definition().id().value(), NamedTextColor.GREEN))
              .append(messages.component("command.definitions.entry-source", NamedTextColor.GRAY))
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
          messages.component(
              "command.definitions.reloaded",
              result.failures().isEmpty() ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
              java.util.Map.of(
                  "loaded", result.loaded().size(), "failed", result.failures().size())));
    } catch (Exception e) {
      sender.sendMessage(
          messages.component(
              "command.definitions.reload-failed",
              NamedTextColor.RED,
              java.util.Map.of("reason", e.getMessage())));
    }
    return 1;
  }

  private int definitionValidate(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    try {
      // Rebuild first so model and animation references are checked against the current on-disk
      // sources, including unsupported Blockbench geometry introduced after installation.
      assetPipeline.buildAssets();
      DefinitionLoadResult result = configBootstrap.validateFromDisk();
      java.util.List<ValidationDiagnostic> references = referenceValidator.validate(result);
      java.util.List<ValidationDiagnostic> diagnostics = new ArrayList<>(references);
      diagnostics.addAll(
          new OperationalReadinessValidator()
              .validate(
                  PaperResourcePackDeliveryBootstrap.readSettings(plugin.getDataFolder().toPath()),
                  integrations));
      long diagnosticErrors =
          diagnostics.stream()
              .filter(diagnostic -> diagnostic.severity() == ValidationDiagnostic.Severity.ERROR)
              .count();
      if (result.failures().isEmpty() && diagnosticErrors == 0) {
        sender.sendMessage(
            messages.component(
                "command.definitions.validation-passed",
                NamedTextColor.GREEN,
                java.util.Map.of("count", result.loaded().size())));
      } else {
        sender.sendMessage(
            messages.component(
                "command.definitions.validation-failed",
                NamedTextColor.RED,
                java.util.Map.of("count", result.failures().size() + diagnosticErrors)));
        for (var failure : result.failures()) {
          sender.sendMessage(
              messages.component(
                  "command.definitions.failure-source",
                  NamedTextColor.YELLOW,
                  java.util.Map.of("source", failure.sourceFile())));
          for (ValidationDiagnostic diag : failure.diagnostics()) {
            sender.sendMessage(
                messages.component(
                    "command.definitions.diagnostic",
                    NamedTextColor.RED,
                    java.util.Map.of("code", diag.code(), "message", diag.message())));
          }
        }
      }
      for (ValidationDiagnostic diagnostic : diagnostics) {
        sender.sendMessage(
            messages.component(
                "command.definitions.diagnostic",
                color(diagnostic.severity()),
                java.util.Map.of("code", diagnostic.code(), "message", diagnostic.message())));
      }
    } catch (Exception e) {
      sender.sendMessage(
          messages.component(
              "command.definitions.validation-error",
              NamedTextColor.RED,
              java.util.Map.of("reason", e.getMessage())));
    }
    return 1;
  }

  private static NamedTextColor color(ValidationDiagnostic.Severity severity) {
    return switch (severity) {
      case ERROR -> NamedTextColor.RED;
      case WARNING -> NamedTextColor.YELLOW;
      case INFO -> NamedTextColor.GRAY;
    };
  }
}
