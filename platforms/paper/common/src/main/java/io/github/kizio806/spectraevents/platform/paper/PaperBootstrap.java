package io.github.kizio806.spectraevents.platform.paper;

import io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository;
import io.github.kizio806.spectraevents.adapter.update.http.HttpUpdateAdapter;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.paper.action.PaperActionAdapter;
import io.github.kizio806.spectraevents.platform.paper.command.SpectraDebugCommand;
import io.github.kizio806.spectraevents.platform.paper.command.SpectraMainCommand;
import io.github.kizio806.spectraevents.platform.paper.common.PaperCapabilityQuery;
import io.github.kizio806.spectraevents.platform.paper.common.PaperLifecycleReporter;
import io.github.kizio806.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.paper.gui.AdminGuiController;
import io.github.kizio806.spectraevents.platform.paper.integration.LuckPermsIntegration;
import io.github.kizio806.spectraevents.platform.paper.integration.PaperIntegrationManager;
import io.github.kizio806.spectraevents.platform.paper.integration.PlaceholderAPIIntegration;
import io.github.kizio806.spectraevents.platform.paper.integration.VaultIntegration;
import io.github.kizio806.spectraevents.platform.paper.integration.WorldGuardIntegration;
import io.github.kizio806.spectraevents.platform.paper.interaction.PaperEntityDeathRouter;
import io.github.kizio806.spectraevents.platform.paper.interaction.PaperInteractionRouter;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperEntityReconciler;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import io.github.kizio806.spectraevents.platform.paper.scheduler.PaperEventTaskScheduler;
import io.github.kizio806.spectraevents.platform.paper.scheduler.PaperRegionTaskScheduler;
import io.github.kizio806.spectraevents.platform.paper.update.UpdateNotificationListener;
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
    plugin
        .getLogger()
        .info("[SpectraEvents] SpectraEvents " + plugin.getPluginMeta().getVersion() + " starting");
    plugin
        .getLogger()
        .info(
            "[SpectraEvents] Platform: Paper (API: "
                + plugin.getPluginMeta().getAPIVersion()
                + ")");
    eventTaskScheduler = new PaperEventTaskScheduler(plugin);
    regionScheduler = new PaperRegionTaskScheduler(plugin);

    integrationRegistry = new IntegrationRegistry();
    PaperIntegrationManager integrationManager = new PaperIntegrationManager(integrationRegistry);
    integrationManager.detectAll();

    renderer = new PaperModelRenderer(plugin);
    cleaner = new PaperResourceCleaner(renderer, eventTaskScheduler);

    Path dbPath = plugin.getDataFolder().toPath().resolve("spectraevents.db");
    sqliteRepository = new SQLiteEventInstanceRepository(dbPath);
    sqliteRepository.initialize();

    PaperActionAdapter actionAdapter = new PaperActionAdapter(renderer, regionScheduler, cleaner);
    PaperEntityReconciler reconciler = new PaperEntityReconciler(plugin, renderer);

    application =
        new SpectraEventsApplication(
            new PaperLifecycleReporter(plugin),
            eventTaskScheduler,
            actionAdapter,
            sqliteRepository,
            reconciler,
            new PaperCapabilityQuery(),
            renderer);

    io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchProjectReader bbReader =
        new io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchProjectReader();
    io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder rpBuilder =
        new io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder(
            plugin.getDataFolder().toPath().resolve("generated").resolve("resource-pack"));
    io.github.kizio806.spectraevents.application.asset.AssetPipelineService assetPipelineService =
        new io.github.kizio806.spectraevents.application.asset.AssetPipelineService(
            bbReader,
            application.modelDefinitionRegistry(),
            application.animationDefinitionRegistry(),
            rpBuilder,
            plugin.getDataFolder().toPath().resolve("assets").resolve("source"),
            io.github.kizio806.spectraevents.application.asset.AssetTargetProfile.PROFILE_26_1);
    application.setAssetPipelineService(assetPipelineService);

    actionAdapter.setModelRuntimeService(application.modelRuntimeService());
    application.start();

    // Load 3D Models
    try {
      io.github.kizio806.spectraevents.application.model.loader.FileSystemModelLoader
          modelFileSystemLoader =
              new io.github.kizio806.spectraevents.application.model.loader.FileSystemModelLoader(
                  plugin.getDataFolder().toPath(), application.modelLoader());
      var modelLoadResult = modelFileSystemLoader.loadFromDisk();
      plugin
          .getLogger()
          .info(
              "[SpectraEvents] Models: "
                  + modelLoadResult.loadedCount()
                  + " loaded, "
                  + modelLoadResult.invalidCount()
                  + " invalid.");
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to load 3D models: " + e.getMessage());
    }

    definitionConfigBootstrap =
        new PaperDefinitionConfigBootstrap(plugin, application.definitionLoader());

    try {
      definitionConfigBootstrap.ensureDefaultConfiguration();
      definitionConfigBootstrap.logLoadResult(definitionConfigBootstrap.loadFromDisk());

      // Reconcile entities after definitions are loaded
      io.github.kizio806.spectraevents.application.service.EntityReconciliationReport report =
          application.reconciliationService().reconcileAll();
      application.setLastReconciliationReport(report);
      plugin
          .getLogger()
          .info(
              String.format(
                  "[SpectraEvents] Definitions: %d events registered. Entity Reconciliation: %d recovered, %d reconnected, %d orphans removed.",
                  application.definitionRegistry().getAll().size(),
                  report.instancesRecovered(),
                  report.entitiesReconnected(),
                  report.orphansRemoved()));
    } catch (Exception e) {
      plugin
          .getLogger()
          .severe("Failed to initialize event definitions or reconciliation: " + e.getMessage());
    }

    // Initialize specific integrations
    new PlaceholderAPIIntegration(sqliteRepository, application.executionEngine().stateStore());

    application.executionEngine().registerConditionResolver(new LuckPermsIntegration());
    application.executionEngine().registerConditionResolver(new WorldGuardIntegration());
    application.executionEngine().registerActionResolver(new VaultIntegration());

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
                            + ". Run /event update info");
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
    org.bukkit.Bukkit.getPluginManager().registerEvents(actionAdapter.bossBarManager(), plugin);
    org.bukkit.Bukkit.getPluginManager().registerEvents(actionAdapter.scoreboardManager(), plugin);

    SpectraMainCommand mainCommand =
        new SpectraMainCommand(
            application.orchestrationService(),
            application.definitionRegistry(),
            sqliteRepository,
            definitionConfigBootstrap,
            integrationRegistry,
            updateService,
            guiController,
            application);

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
    plugin.getLogger().info("[SpectraEvents] SpectraEvents disabled cleanly.");
  }
}
