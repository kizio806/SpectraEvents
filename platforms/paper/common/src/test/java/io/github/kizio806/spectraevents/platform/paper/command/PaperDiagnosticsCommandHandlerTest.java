package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EntityReconciliationReport;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperDiagnosticsCommandHandlerTest {

  @TempDir Path tempDir;
  private EventDefinitionRegistry defRegistry;
  private InMemoryEventInstanceRepository instanceRepo;
  private BukkitDefinitionConfigBootstrap configBootstrap;
  private IntegrationRegistry integrationRegistry;
  private SpectraEventsApplication application;
  private LocaleCatalog locales;
  private DiagnosticsCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private CommandSourceStack source;
  private Path eventsDir;

  @BeforeEach
  void setUp() throws Exception {
    eventsDir = tempDir.resolve("events");
    Files.createDirectories(eventsDir);

    defRegistry = new EventDefinitionRegistry();
    instanceRepo = new InMemoryEventInstanceRepository();

    Plugin mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (p, m, a) -> {
                  if ("getDataFolder".equals(m.getName())) return tempDir.toFile();
                  return null;
                });

    io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader loader =
        new io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader(
            new io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser(),
            new io.github.kizio806.spectraevents.application.config.compiler
                .EventDefinitionCompiler(),
            defRegistry);

    configBootstrap = new BukkitDefinitionConfigBootstrap(mockPlugin, loader);
    integrationRegistry = new IntegrationRegistry();

    io.github.kizio806.spectraevents.application.port.LifecycleReporter mockReporter =
        (io.github.kizio806.spectraevents.application.port.LifecycleReporter)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.LifecycleReporter.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.LifecycleReporter.class
                },
                (p, m, a) -> null);
    io.github.kizio806.spectraevents.application.port.EventTaskScheduler mockScheduler =
        (io.github.kizio806.spectraevents.application.port.EventTaskScheduler)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.EventTaskScheduler.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.EventTaskScheduler.class
                },
                (p, m, a) -> {
                  if (m.getReturnType().equals(int.class)) return 0;
                  return null;
                });
    io.github.kizio806.spectraevents.application.port.PlatformActionPort mockActionPort =
        (io.github.kizio806.spectraevents.application.port.PlatformActionPort)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.PlatformActionPort.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.PlatformActionPort.class
                },
                (p, m, a) -> {
                  if (m.getReturnType().equals(int.class)) return 0;
                  return null;
                });
    io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort mockReconciler =
        (io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort
                      .class
                },
                (p, m, a) -> null);
    io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery mockCapability =
        (io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery.class
                },
                (p, m, a) -> null);

    application =
        new SpectraEventsApplication(
            mockReporter,
            mockScheduler,
            mockActionPort,
            instanceRepo,
            mockReconciler,
            mockCapability,
            null);

    Map<String, String> localeMap = new HashMap<>();
    localeMap.put("command.doctor.title", "Diagnostics:");
    localeMap.put("command.doctor.events-directory", "Events dir: %status%");
    localeMap.put("command.doctor.directory-ok", "OK: %path%");
    localeMap.put("command.doctor.directory-missing", "Missing");
    localeMap.put("command.doctor.definitions", "Definitions: %count%");
    localeMap.put("command.doctor.definitions-action", "Action needed for definitions");
    localeMap.put("command.doctor.active-instances", "Active: %count%");
    localeMap.put("command.doctor.instances-action", "Action needed for instances");
    localeMap.put("command.doctor.instance-entry", " - %id% (%definition%) claim=%claim%");
    localeMap.put("command.doctor.integrations", "Integrations: %count%");
    localeMap.put("command.doctor.models", "Models loaded=%loaded% active=%active%");
    localeMap.put(
        "command.doctor.reconciliation", "Reconciliation recovered=%recovered% orphans=%orphans%");
    localeMap.put("command.doctor.complete", "Diagnostics complete.");
    localeMap.put("command.doctor.unclaimed", "None");

    locales = LocaleCatalog.of(localeMap);

    handler =
        new DiagnosticsCommandHandler(
            defRegistry, instanceRepo, configBootstrap, integrationRegistry, application, locales);

    messages = new ArrayList<>();
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
  }

  @Test
  void testDoctorExecutionEmpty() throws Exception {
    dispatcher.execute("doctor", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testDoctorExecutionWithActiveEventsDefinitionsAndIntegrations() throws Exception {
    // 1. Definition in registry
    PhaseDefinition phase =
        new PhaseDefinition(new PhaseId("start"), Set.of(), List.of(), List.of());
    EventDefinition def =
        new EventDefinition(
            new EventDefinitionId("meteor"),
            new PhaseId("start"),
            Map.of(new PhaseId("start"), phase));
    defRegistry.register(def, "events/meteor.yml");

    // 2. Active running event instances (one with claimant, one unclaimed)
    EventInstanceId id1 = EventInstanceId.generate();
    EventInstance inst1 =
        EventInstance.reconstitute(
            id1,
            new EventDefinitionId("meteor"),
            EventLifecycleState.RUNNING,
            new PhaseId("start"));
    instanceRepo.save(inst1);

    EventRuntimeState state1 = new EventRuntimeState(id1);
    state1.tryClaim("player-claimant");
    application.executionEngine().stateStore().put(state1);

    EventInstanceId id2 = EventInstanceId.generate();
    EventInstance inst2 =
        EventInstance.reconstitute(
            id2,
            new EventDefinitionId("meteor"),
            EventLifecycleState.RUNNING,
            new PhaseId("start"));
    instanceRepo.save(inst2);

    // 3. Active integration in registry
    integrationRegistry.register("placeholderapi", IntegrationState.ACTIVE, "2.11.0");

    // 4. Reconciliation report
    application.setLastReconciliationReport(new EntityReconciliationReport(1L, 0L, 0L, 0L, 0L));

    messages.clear();
    dispatcher.execute("doctor", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testDoctorWithMissingEventsDirectory() throws Exception {
    // Delete events directory
    Files.deleteIfExists(eventsDir);

    messages.clear();
    dispatcher.execute("doctor", source);
    assertFalse(messages.isEmpty());
  }
}
