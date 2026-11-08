package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeStateStore;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdatePort;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import io.github.kizio806.spectraevents.platform.bukkit.gui.AdminGuiController;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.paper.action.PaperActionAdapter;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.configuration.PluginMeta;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpectraMainCommandTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private CommandSourceStack source;
  private SpectraMainCommand mainCommand;

  @BeforeEach
  void setUp() throws IOException {
    Path schedulesFile = tempDir.resolve("schedules.yml");
    Files.writeString(schedulesFile, "schedules: []\n");

    PluginMeta mockMeta =
        (PluginMeta)
            Proxy.newProxyInstance(
                PluginMeta.class.getClassLoader(),
                new Class<?>[] {PluginMeta.class},
                (p, m, a) -> {
                  if ("getVersion".equals(m.getName())) return "1.0.0";
                  return null;
                });

    Plugin mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (p, m, a) -> {
                  if ("getDataFolder".equals(m.getName())) return tempDir.toFile();
                  if ("getPluginMeta".equals(m.getName())) return mockMeta;
                  return null;
                });

    EventDefinitionRegistry defRegistry = new EventDefinitionRegistry();
    InMemoryEventInstanceRepository instanceRepo = new InMemoryEventInstanceRepository();

    LifecycleReporter mockReporter =
        (LifecycleReporter)
            Proxy.newProxyInstance(
                LifecycleReporter.class.getClassLoader(),
                new Class<?>[] {LifecycleReporter.class},
                (p, m, a) -> null);

    EventTaskScheduler mockScheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (p, m, a) -> null);

    PlatformActionPort mockActionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (p, m, a) -> CompletableFuture.completedFuture(true));

    PlatformEntityReconcilerPort mockReconciler =
        (PlatformEntityReconcilerPort)
            Proxy.newProxyInstance(
                PlatformEntityReconcilerPort.class.getClassLoader(),
                new Class<?>[] {PlatformEntityReconcilerPort.class},
                (p, m, a) -> null);

    PlatformCapabilityQuery mockCapability =
        (PlatformCapabilityQuery)
            Proxy.newProxyInstance(
                PlatformCapabilityQuery.class.getClassLoader(),
                new Class<?>[] {PlatformCapabilityQuery.class},
                (p, m, a) -> null);

    SpectraEventsApplication application =
        new SpectraEventsApplication(
            mockReporter,
            mockScheduler,
            mockActionPort,
            instanceRepo,
            mockReconciler,
            mockCapability,
            null);

    io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader loader =
        new io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader(
            new io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser(),
            new io.github.kizio806.spectraevents.application.config.compiler
                .EventDefinitionCompiler(),
            defRegistry);

    BukkitDefinitionConfigBootstrap configBootstrap =
        new BukkitDefinitionConfigBootstrap(mockPlugin, loader);
    IntegrationRegistry integrationRegistry = new IntegrationRegistry();

    UpdatePort updatePort =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(UpdateInfo.upToDate(version));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(String url, String fileName) {
            return CompletableFuture.completedFuture(false);
          }
        };
    UpdateService updateService = new UpdateService("1.0.0", updatePort);

    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "command.main.help-title", "SpectraEvents Help:",
                "command.main.version", "Version: %version%",
                "command.main.status", "Status: Active: %active%, Definitions: %definitions%",
                "command.main.gui-player-only", "Only players can open GUI"));

    BukkitEventSettingsStore settingsStore =
        new BukkitEventSettingsStore(tempDir.resolve("settings.yml"));

    AdminGuiController guiController =
        new AdminGuiController(
            defRegistry,
            instanceRepo,
            integrationRegistry,
            updateService,
            application.orchestrationService(),
            settingsStore,
            locales);

    RegionTaskScheduler regionScheduler =
        new RegionTaskScheduler() {
          @Override
          public void executeGlobal(Runnable task) {
            task.run();
          }

          @Override
          public void executeAt(Location location, Runnable task) {
            task.run();
          }

          @Override
          public void executeFor(Entity entity, Runnable task) {
            task.run();
          }
        };

    PaperModelRenderer renderer = new PaperModelRenderer(mockPlugin);
    PaperResourceCleaner cleaner = new PaperResourceCleaner(renderer, mockScheduler);
    PaperActionAdapter actionAdapter = new PaperActionAdapter(regionScheduler, cleaner, locales);

    EventOrchestrationService orchestrationService =
        new EventOrchestrationService(
            instanceRepo,
            defRegistry,
            new EventExecutionEngine(
                instanceRepo,
                defRegistry,
                mockScheduler,
                mockActionPort,
                new EventRuntimeStateStore()));

    mainCommand =
        new SpectraMainCommand(
            mockPlugin,
            orchestrationService,
            defRegistry,
            instanceRepo,
            configBootstrap,
            integrationRegistry,
            updateService,
            guiController,
            application,
            settingsStore,
            actionAdapter,
            regionScheduler,
            schedulesFile,
            locales);

    messages = new ArrayList<>();
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(mainCommand.buildCommand());
  }

  @Test
  void testHelpVersionAndStatus() throws Exception {
    dispatcher.execute("spectraevents", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("spectraevents help", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("spectraevents version", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("spectraevents status", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testAdminConsole() throws Exception {
    dispatcher.execute("spectraevents admin", source);
    assertFalse(messages.isEmpty());
  }
}
