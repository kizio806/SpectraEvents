package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.nio.file.Files;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Command handler for doctor/diagnostics command. */
public final class DiagnosticsCommandHandler {
  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final PaperDefinitionConfigBootstrap configBootstrap;
  private final IntegrationRegistry integrationRegistry;
  private final io.github.kizio806.spectraevents.application.SpectraEventsApplication application;
  private final CommandText messages;

  public DiagnosticsCommandHandler(
      EventDefinitionRegistry definitionRegistry,
      EventInstanceRepository instanceRepository,
      PaperDefinitionConfigBootstrap configBootstrap,
      IntegrationRegistry integrationRegistry,
      io.github.kizio806.spectraevents.application.SpectraEventsApplication application,
      LocaleCatalog locales) {
    this.definitionRegistry = definitionRegistry;
    this.instanceRepository = instanceRepository;
    this.configBootstrap = configBootstrap;
    this.integrationRegistry = integrationRegistry;
    this.application = application;
    this.messages = new CommandText(locales);
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("doctor")
        .requires(s -> s.getSender().hasPermission("spectraevents.doctor"))
        .executes(this::doctor);
  }

  public int doctor(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    sender.sendMessage(messages.component("command.doctor.title", NamedTextColor.DARK_PURPLE));

    boolean eventsDirOk = Files.isDirectory(configBootstrap.eventsDirectory());
    sender.sendMessage(
        messages.component(
            "command.doctor.events-directory",
            eventsDirOk ? NamedTextColor.GREEN : NamedTextColor.RED,
            Map.of(
                "status",
                eventsDirOk
                    ? messages.message(
                        "command.doctor.directory-ok",
                        Map.of("path", configBootstrap.eventsDirectory()))
                    : messages.message("command.doctor.directory-missing"))));

    int defCount = definitionRegistry.getAll().size();
    sender.sendMessage(
        messages.component(
            "command.doctor.definitions",
            defCount > 0 ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
            Map.of("count", defCount)));

    int activeCount =
        (int)
            instanceRepository.findAll().stream()
                .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
                .count();
    sender.sendMessage(
        messages.component(
            "command.doctor.active-instances", NamedTextColor.GREEN, Map.of("count", activeCount)));

    long enabledIntegrations =
        integrationRegistry.getAll().values().stream()
            .filter(
                i ->
                    i.state()
                        == io.github.kizio806.spectraevents.application.integration.IntegrationState
                            .ENABLED)
            .count();
    sender.sendMessage(
        messages.component(
            "command.doctor.integrations",
            enabledIntegrations > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY,
            Map.of("count", enabledIntegrations)));

    if (application.modelDefinitionRegistry() != null) {
      int modelCount = application.modelDefinitionRegistry().all().size();
      int activeModels =
          application.modelRuntimeService() != null
              ? application.modelRuntimeService().getActiveInstances().size()
              : 0;
      sender.sendMessage(
          messages.component(
              "command.doctor.models",
              NamedTextColor.GREEN,
              Map.of("loaded", modelCount, "active", activeModels)));
    }

    io.github.kizio806.spectraevents.application.service.EntityReconciliationReport report =
        application.lastReconciliationReport();
    if (report != null) {
      sender.sendMessage(
          messages.component(
              "command.doctor.reconciliation",
              NamedTextColor.YELLOW,
              Map.of(
                  "recovered", report.instancesRecovered(), "orphans", report.orphansRemoved())));
    }

    if (!eventsDirOk || defCount == 0) {
      sender.sendMessage(
          messages.component("command.doctor.definitions-action", NamedTextColor.RED));
    }
    if (activeCount > 0) {
      sender.sendMessage(
          messages.component("command.doctor.instances-action", NamedTextColor.GOLD));
      instanceRepository.findAll().stream()
          .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
          .forEach(
              instance -> {
                var diagnostics = application.executionEngine().diagnostics(instance.id());
                sender.sendMessage(
                    messages.component(
                        "command.doctor.instance-entry",
                        NamedTextColor.GRAY,
                        Map.of(
                            "id",
                            instance.id(),
                            "definition",
                            instance.definitionId().value(),
                            "claim",
                            diagnostics.claimant() != null
                                ? diagnostics.claimant()
                                : messages.message("command.doctor.unclaimed"))));
              });
    }

    sender.sendMessage(messages.component("command.doctor.complete", NamedTextColor.DARK_PURPLE));
    return 1;
  }
}
