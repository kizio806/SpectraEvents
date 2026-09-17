package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.nio.file.Files;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Command handler for doctor/diagnostics command. */
public final class DiagnosticsCommandHandler {
  private final EventDefinitionRegistry definitionRegistry;
  private final EventInstanceRepository instanceRepository;
  private final PaperDefinitionConfigBootstrap configBootstrap;
  private final IntegrationRegistry integrationRegistry;
  private final io.github.kizio806.spectraevents.application.SpectraEventsApplication application;

  public DiagnosticsCommandHandler(
      EventDefinitionRegistry definitionRegistry,
      EventInstanceRepository instanceRepository,
      PaperDefinitionConfigBootstrap configBootstrap,
      IntegrationRegistry integrationRegistry,
      io.github.kizio806.spectraevents.application.SpectraEventsApplication application) {
    this.definitionRegistry = definitionRegistry;
    this.instanceRepository = instanceRepository;
    this.configBootstrap = configBootstrap;
    this.integrationRegistry = integrationRegistry;
    this.application = application;
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("doctor")
        .requires(s -> s.getSender().hasPermission("spectraevents.doctor"))
        .executes(this::doctor);
  }

  public int doctor(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    sender.sendMessage(Component.text("=== SpectraEvents Doctor ===", NamedTextColor.DARK_PURPLE));

    boolean eventsDirOk = Files.isDirectory(configBootstrap.eventsDirectory());
    sender.sendMessage(
        Component.text(" [1] Events Directory: ", NamedTextColor.GRAY)
            .append(
                Component.text(
                    eventsDirOk ? "OK (" + configBootstrap.eventsDirectory() + ")" : "MISSING",
                    eventsDirOk ? NamedTextColor.GREEN : NamedTextColor.RED)));

    int defCount = definitionRegistry.getAll().size();
    sender.sendMessage(
        Component.text(" [2] Registered Definitions: ", NamedTextColor.GRAY)
            .append(
                Component.text(
                    defCount + " loaded",
                    defCount > 0 ? NamedTextColor.GREEN : NamedTextColor.YELLOW)));

    int activeCount = instanceRepository.findAll().size();
    sender.sendMessage(
        Component.text(" [3] Active Instances: ", NamedTextColor.GRAY)
            .append(Component.text(activeCount + " running", NamedTextColor.GREEN)));

    long enabledIntegrations =
        integrationRegistry.getAll().values().stream()
            .filter(
                i ->
                    i.state()
                        == io.github.kizio806.spectraevents.application.integration.IntegrationState
                            .ENABLED)
            .count();
    sender.sendMessage(
        Component.text(" [4] Integrations: ", NamedTextColor.GRAY)
            .append(
                Component.text(
                    enabledIntegrations + " active",
                    enabledIntegrations > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY)));

    if (application.modelDefinitionRegistry() != null) {
      int modelCount = application.modelDefinitionRegistry().all().size();
      int activeModels =
          application.modelRuntimeService() != null
              ? application.modelRuntimeService().getActiveInstances().size()
              : 0;
      sender.sendMessage(
          Component.text(" [5] 3D Models: ", NamedTextColor.GRAY)
              .append(Component.text(modelCount + " loaded, ", NamedTextColor.GREEN))
              .append(Component.text(activeModels + " active instances", NamedTextColor.GREEN)));
    }

    io.github.kizio806.spectraevents.application.service.EntityReconciliationReport report =
        application.lastReconciliationReport();
    if (report != null) {
      sender.sendMessage(
          Component.text(" [6] Last Startup Reconciliation: ", NamedTextColor.GRAY)
              .append(
                  Component.text(
                      report.instancesRecovered() + " recovered, ", NamedTextColor.GREEN))
              .append(
                  Component.text(
                      report.orphansRemoved() + " orphans removed", NamedTextColor.YELLOW)));
    }

    sender.sendMessage(Component.text("Doctor check finished.", NamedTextColor.DARK_PURPLE));
    return 1;
  }
}
