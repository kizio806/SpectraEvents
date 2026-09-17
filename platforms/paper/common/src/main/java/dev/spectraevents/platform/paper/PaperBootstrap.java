package dev.spectraevents.platform.paper;

import dev.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository;
import dev.spectraevents.adapter.update.http.HttpUpdateAdapter;
import dev.spectraevents.application.SpectraEventsApplication;
import dev.spectraevents.application.integration.IntegrationRegistry;
import dev.spectraevents.application.update.UpdateService;
import dev.spectraevents.platform.paper.action.PaperActionAdapter;
import dev.spectraevents.platform.paper.command.SpectraDebugCommand;
import dev.spectraevents.platform.paper.command.SpectraMainCommand;
import dev.spectraevents.platform.paper.common.PaperLifecycleReporter;
import dev.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import dev.spectraevents.platform.paper.gui.AdminGuiController;
import dev.spectraevents.platform.paper.integration.PaperIntegrationManager;
import dev.spectraevents.platform.paper.interaction.PaperEntityDeathRouter;
import dev.spectraevents.platform.paper.interaction.PaperInteractionRouter;
import dev.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import dev.spectraevents.platform.paper.render.PaperModelRenderer;
import dev.spectraevents.platform.paper.scheduler.PaperEventTaskScheduler;
import dev.spectraevents.platform.paper.scheduler.PaperRegionTaskScheduler;
import dev.spectraevents.platform.paper.update.UpdateNotificationListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.nio.file.Path;

/** Composition Root for the Paper platform family. */
public final class PaperBootstrap {
  private final org.bukkit.plugin.java.JavaPlugin plugin;
  private SpectraEventsApplication application;
  private PaperModelRenderer renderer;
  private PaperEventTaskScheduler eventTaskScheduler;
  private PaperRegionTaskScheduler regionScheduler;
  private PaperResourceCleaner cleaner;
  private PaperDefinitionConfigBootstrap definitionConfigBootstrap;
  private SQLiteEventInstanceRepository sqliteRepository;
  private IntegrationRegistry integrationRegistry;
  private UpdateService updateService;

  public PaperBootstrap(org.bukkit.plugin.java.JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void enable() {
    renderer = new PaperModelRenderer(plugin);
    eventTaskScheduler = new PaperEventTaskScheduler(plugin);
    regionScheduler = new PaperRegionTaskScheduler(plugin);
    cleaner = new PaperResourceCleaner(renderer, eventTaskScheduler);

    Path dbPath = plugin.getDataFolder().toPath().resolve("spectraevents.db");
    sqliteRepository = new SQLiteEventInstanceRepository(dbPath);
    sqliteRepository.initialize();

    PaperActionAdapter actionAdapter = new PaperActionAdapter(renderer, regionScheduler, cleaner);

    application =
        new SpectraEventsApplication(
            new PaperLifecycleReporter(plugin),
            eventTaskScheduler,
            actionAdapter,
            sqliteRepository);
    application.start();

    definitionConfigBootstrap =
        new PaperDefinitionConfigBootstrap(plugin, application.definitionLoader());

    try {
      definitionConfigBootstrap.ensureDefaultConfiguration();
      definitionConfigBootstrap.logLoadResult(definitionConfigBootstrap.loadFromDisk());
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to load event definitions: " + e.getMessage());
    }

    integrationRegistry = new IntegrationRegistry();
    PaperIntegrationManager integrationManager = new PaperIntegrationManager(integrationRegistry);
    integrationManager.detectAll();

    Path updateDir = plugin.getDataFolder().toPath().resolve("update");
    HttpUpdateAdapter updateAdapter = new HttpUpdateAdapter(updateDir);
    updateService = new UpdateService(plugin.getPluginMeta().getVersion(), updateAdapter);
    updateService
        .checkNow("stable")
        .thenAccept(
            info -> {
              if (info.updateAvailable()) {
                plugin
                    .getLogger()
                    .info(
                        "[SpectraEvents] Update available: "
                            + info.currentVersion()
                            + " -> "
                            + info.latestVersion()
                            + ". Run /spectra update info");
              }
            });
    org.bukkit.Bukkit.getPluginManager()
        .registerEvents(new UpdateNotificationListener(updateService), plugin);

    AdminGuiController guiController =
        new AdminGuiController(
            plugin,
            application.orchestrationService(),
            application.definitionRegistry(),
            sqliteRepository,
            integrationRegistry,
            updateService);
    org.bukkit.Bukkit.getPluginManager().registerEvents(guiController, plugin);

    PaperInteractionRouter interactionRouter =
        new PaperInteractionRouter(
            application.orchestrationService(), application.executionEngine());

    PaperEntityDeathRouter entityDeathRouter =
        new PaperEntityDeathRouter(
            application.orchestrationService(), application.executionEngine());

    org.bukkit.Bukkit.getPluginManager().registerEvents(interactionRouter, plugin);
    org.bukkit.Bukkit.getPluginManager().registerEvents(entityDeathRouter, plugin);

    SpectraMainCommand mainCommand =
        new SpectraMainCommand(
            application.orchestrationService(),
            application.definitionRegistry(),
            sqliteRepository,
            definitionConfigBootstrap,
            integrationRegistry,
            updateService,
            guiController);

    SpectraDebugCommand debugCommand =
        new SpectraDebugCommand(application.orchestrationService(), renderer);

    plugin
        .getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            event -> {
              event
                  .registrar()
                  .register(mainCommand.buildCommand().build(), "SpectraEvents management command");
              event
                  .registrar()
                  .register(
                      debugCommand.buildCommand().build(),
                      "Developer debug commands for SpectraEvents");
            });
  }

  public void disable() {
    if (cleaner != null) {
      cleaner.cleanupAll();
    }
    if (application != null) {
      application.stop();
      application = null;
    }
  }
}
