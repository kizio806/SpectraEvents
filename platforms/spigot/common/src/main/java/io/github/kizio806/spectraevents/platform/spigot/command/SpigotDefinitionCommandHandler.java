package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.validation.EventReferenceValidator;
import io.github.kizio806.spectraevents.application.validation.OperationalReadinessValidator;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.spigot.asset.delivery.SpigotResourcePackDeliveryBootstrap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/** Handles `/spectraevents definition` and `/spectraevents validate` subcommands on Spigot. */
public final class SpigotDefinitionCommandHandler implements SpigotSubcommandHandler {

  private final Plugin plugin;
  private final SpectraEventsApplication application;
  private final BukkitDefinitionConfigBootstrap definitions;
  private final SpigotCommandHelper helper;

  public SpigotDefinitionCommandHandler(
      Plugin plugin,
      SpectraEventsApplication application,
      BukkitDefinitionConfigBootstrap definitions,
      SpigotCommandHelper helper) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.application = Objects.requireNonNull(application, "application");
    this.definitions = Objects.requireNonNull(definitions, "definitions");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public void execute(CommandSender sender, String[] arguments) {
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      list(sender);
      return;
    }
    if ("reload".equalsIgnoreCase(arguments[0]) || "validate".equalsIgnoreCase(arguments[0])) {
      reloadOrValidate(sender, "validate".equalsIgnoreCase(arguments[0]));
      return;
    }
    throw new IllegalArgumentException("Unknown definition subcommand: " + arguments[0]);
  }

  private void list(CommandSender sender) {
    helper.require(sender, "spectraevents.definition.list");
    helper.send(
        sender,
        "command.spigot.definitions-title",
        Map.of("count", application.definitionRegistry().getAll().size()));
    for (var registered : application.definitionRegistry().getAll()) {
      helper.send(
          sender,
          "command.spigot.definition-entry",
          Map.of("definition", registered.definition().id().value()));
    }
  }

  public void reloadOrValidate(CommandSender sender, boolean validateOnly) {
    try {
      helper.require(
          sender,
          validateOnly ? "spectraevents.definition.validate" : "spectraevents.definition.reload");
      if (validateOnly) {
        application.assetPipelineService().buildAssets();
      }
      DefinitionLoadResult result =
          validateOnly ? definitions.validateFromDisk() : definitions.reloadFromDisk();
      var referenceDiagnostics =
          validateOnly
              ? new EventReferenceValidator(
                      application.modelDefinitionRegistry(),
                      application.animationDefinitionRegistry())
                  .validate(result)
              : List.<ValidationDiagnostic>of();
      List<ValidationDiagnostic> readinessDiagnostics = readinessDiagnostics(validateOnly);
      List<ValidationDiagnostic> diagnostics = new ArrayList<>(referenceDiagnostics);
      diagnostics.addAll(readinessDiagnostics);
      long diagnosticErrors = countErrors(diagnostics);
      definitions.logLoadResult(result);
      helper.send(
          sender,
          validateOnly
              ? "command.spigot.definitions-validated"
              : "command.spigot.definitions-reloaded",
          Map.of(
              "loaded",
              result.loaded().size(),
              "failed",
              result.failures().size() + diagnosticErrors));
      sendDefinitionFailures(sender, result);
      sendDiagnostics(sender, diagnostics);
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot read definitions: " + exception.getMessage(), exception);
    }
  }

  private List<ValidationDiagnostic> readinessDiagnostics(boolean validateOnly) throws IOException {
    if (!validateOnly) {
      return List.of();
    }
    return new OperationalReadinessValidator()
        .validateResourcePack(
            SpigotResourcePackDeliveryBootstrap.readSettings(plugin.getDataFolder().toPath()));
  }

  private static long countErrors(List<ValidationDiagnostic> diagnostics) {
    return diagnostics.stream()
        .filter(diagnostic -> diagnostic.severity() == ValidationDiagnostic.Severity.ERROR)
        .count();
  }

  private void sendDefinitionFailures(CommandSender sender, DefinitionLoadResult result) {
    for (var failure : result.failures()) {
      String formattedDiagnostics = definitions.formatDiagnostics(failure.diagnostics());
      helper.send(
          sender,
          "command.spigot.diagnostic-entry",
          Map.of("path", failure.sourceFile(), "message", formattedDiagnostics));
    }
  }

  private void sendDiagnostics(CommandSender sender, List<ValidationDiagnostic> diagnostics) {
    for (ValidationDiagnostic diagnostic : diagnostics) {
      helper.send(
          sender,
          "command.spigot.diagnostic-entry",
          Map.of("path", diagnostic.path(), "message", diagnostic.message()));
    }
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] arguments) {
    if (sender == null || !sender.hasPermission("spectraevents.definition")) {
      return List.of();
    }
    if (arguments.length == 1) {
      return helper.matches(arguments[0], "list", "reload", "validate");
    }
    return List.of();
  }
}
