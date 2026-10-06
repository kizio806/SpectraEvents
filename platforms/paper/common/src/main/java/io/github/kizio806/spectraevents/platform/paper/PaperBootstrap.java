package io.github.kizio806.spectraevents.platform.paper;

import io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository;
import io.github.kizio806.spectraevents.adapter.update.http.HttpUpdateAdapter;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.DataDirectoryLayout;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.model.animation.runtime.ModelAnimationActionService;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.paper.action.PaperActionAdapter;
import io.github.kizio806.spectraevents.platform.paper.command.SpectraMainCommand;
import io.github.kizio806.spectraevents.platform.paper.common.PaperCapabilityQuery;
import io.github.kizio806.spectraevents.platform.paper.common.PaperLifecycleReporter;
import io.github.kizio806.spectraevents.platform.paper.common.PaperStartupLogger;
import io.github.kizio806.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import io.github.kizio806.spectraevents.platform.paper.gui.AdminGuiController;
import io.github.kizio806.spectraevents.platform.paper.integration.PaperIntegrationManager;
import io.github.kizio806.spectraevents.platform.paper.interaction.PaperEntityDeathRouter;
import io.github.kizio806.spectraevents.platform.paper.interaction.PaperInteractionRouter;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperEntityReconciler;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import io.github.kizio806.spectraevents.platform.paper.loot.PaperSharedLootListener;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import io.github.kizio806.spectraevents.platform.paper.scheduler.PaperEventTaskScheduler;
import io.github.kizio806.spectraevents.platform.paper.scheduler.PaperRegionTaskScheduler;
import io.github.kizio806.spectraevents.platform.paper.update.UpdateNotificationListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Composition Root for the Paper platform family. */
public final class PaperBootstrap {
  private final org.bukkit.plugin.java.JavaPlugin plugin;
  private SpectraEventsApplication application;
  private PaperModelRenderer renderer;
  private PaperEventTaskScheduler eventTaskScheduler;
  private PaperRegionTaskScheduler regionScheduler;
  private PaperResourceCleaner cleaner;
  private PaperActionAdapter actionAdapter;
  private PaperDefinitionConfigBootstrap definitionConfigBootstrap;
  private SQLiteEventInstanceRepository sqliteRepository;
  private IntegrationRegistry integrationRegistry;
  private UpdateService updateService;
  private PaperEventSettingsStore eventSettingsStore;
  private CompletableFuture<Void> updateCheck;
  private CompletableFuture<Void> resourcePackDelivery;

  public PaperBootstrap(org.bukkit.plugin.java.JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void enable() {
    long startedAt = System.nanoTime();
    PaperStartupLogger startupLogger = new PaperStartupLogger(plugin);
    startupLogger.printBanner(
        plugin.getPluginMeta().getVersion(),
        plugin.getServer().getName(),
        plugin.getServer().getMinecraftVersion());
    eventTaskScheduler = new PaperEventTaskScheduler(plugin);
    regionScheduler = new PaperRegionTaskScheduler(plugin);

    integrationRegistry = new IntegrationRegistry();
    PaperIntegrationManager integrationManager = new PaperIntegrationManager(integrationRegistry);
    integrationManager.detectAll();

    renderer = new PaperModelRenderer(plugin);
    cleaner = new PaperResourceCleaner(renderer, eventTaskScheduler);

    DataDirectoryLayout dataLayout;
    try {
      dataLayout = DataDirectoryLayout.prepare(plugin.getDataFolder().toPath());
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not prepare the SpectraEvents data directory", exception);
    }
    eventSettingsStore = new PaperEventSettingsStore(plugin);
    LocaleCatalog locales;
    try {
      locales =
          new io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader(
                  dataLayout.root())
              .loadCatalog(eventSettingsStore.locale());
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not prepare the SpectraEvents locale catalog", exception);
    }
    Path dbPath = dataLayout.databaseFile();
    sqliteRepository = new SQLiteEventInstanceRepository(dbPath);
    sqliteRepository.initialize();
    startupLogger.storage(dbPath, sqliteRepository.findAll().size());

    actionAdapter = new PaperActionAdapter(regionScheduler, cleaner, locales);
    PaperEntityReconciler reconciler = new PaperEntityReconciler(plugin, cleaner);

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
            dataLayout.resourcePackCacheDirectory());
    io.github.kizio806.spectraevents.application.asset.AssetTargetProfile assetTargetProfile =
        io.github.kizio806.spectraevents.application.asset.AssetTargetProfile.forMinecraftVersion(
            plugin.getServer().getMinecraftVersion());
    io.github.kizio806.spectraevents.application.asset.AssetPipelineService assetPipelineService =
        new io.github.kizio806.spectraevents.application.asset.AssetPipelineService(
            bbReader,
            rpBuilder,
            plugin.getDataFolder().toPath().resolve("assets").resolve("source"),
            assetTargetProfile,
            new io.github.kizio806.spectraevents.application.asset.ImportedAssetModelRegistrar(
                application.modelCompiler(),
                application.modelDefinitionRegistry(),
                application.animationDefinitionRegistry()));
    application.setAssetPipelineService(assetPipelineService);
    resourcePackDelivery =
        io.github.kizio806.spectraevents.platform.paper.asset.delivery
            .PaperResourcePackDeliveryBootstrap.configure(
            plugin, plugin.getPluginMeta().getVersion(), assetTargetProfile, locales);
    actionAdapter.setModelRuntimeService(application.modelRuntimeService());
    actionAdapter.setModelAnimationActionService(
        new ModelAnimationActionService(
            application.modelRuntimeService(), application.animationRuntimeService()));

    extractBundledTemplates();
    assetPipelineService.buildAssetsAsync().join();

    definitionConfigBootstrap =
        new PaperDefinitionConfigBootstrap(plugin, application.definitionLoader());

    try {
      definitionConfigBootstrap.ensureDefaultConfiguration();
      var definitionLoadResult = definitionConfigBootstrap.loadFromDisk();
      definitionConfigBootstrap.logLoadResult(definitionLoadResult);
      io.github.kizio806.spectraevents.application.service.EntityReconciliationReport report =
          application.reconciliationService().reconcileAll();
      application.setLastReconciliationReport(report);
      List<String> definitionIds =
          application.definitionRegistry().getAll().stream()
              .map(registered -> registered.definition().id().value())
              .toList();
      startupLogger.definitions(definitionIds, definitionLoadResult.failures().size());
      if (definitionIds.isEmpty()) {
        plugin
            .getLogger()
            .info(
                "No active event definitions are installed. Install a template with "
                    + "/spectraevents template install metin.");
      }
    } catch (IOException | RuntimeException exception) {
      throw new IllegalStateException(
          "Cannot safely enable SpectraEvents: definitions or entity reconciliation failed",
          exception);
    }

    try {
      var schedules =
          new io.github.kizio806.spectraevents.application.schedule.FileSystemScheduleLoader(
                  new io.github.kizio806.spectraevents.application.schedule.ScheduleYamlLoader())
              .load(dataLayout.schedulesFile());
      schedules
          .failures()
          .forEach(
              failure ->
                  plugin
                      .getLogger()
                      .warning(
                          "Ignoring invalid schedule "
                              + failure.path()
                              + ": "
                              + failure.message()));
      application.startSchedules(schedules.schedules());
      plugin.getLogger().info("Loaded schedules=" + schedules.schedules().size());
    } catch (IOException exception) {
      plugin
          .getLogger()
          .warning(
              "Could not load schedules.yml; scheduling remains disabled: "
                  + exception.getMessage());
    }

    application.start();

    // Initialize specific integrations safely

    // Initialize specific integrations safely using ServiceLoader
    io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext
        integrationContext =
            new io.github.kizio806.spectraevents.application.integration
                .IntegrationInitializationContext(
                application, sqliteRepository, plugin.getLogger());

    for (io.github.kizio806.spectraevents.application.integration.PlatformIntegrationModule module :
        java.util.ServiceLoader.load(
            io.github.kizio806.spectraevents.application.integration.PlatformIntegrationModule
                .class,
            plugin.getClass().getClassLoader())) {
      if (org.bukkit.Bukkit.getPluginManager().getPlugin(module.requiredPluginName()) != null) {
        try {
          module.initialize(integrationContext);
        } catch (RuntimeException e) {
          plugin
              .getLogger()
              .warning(
                  "Failed to initialize integration "
                      + module.requiredPluginName()
                      + ": "
                      + e.getMessage());
        }
      }
    }
    startupLogger.integrations(integrationRegistry);

    Path updateDir = plugin.getDataFolder().toPath().resolve("update");
    HttpUpdateAdapter updateAdapter = new HttpUpdateAdapter(updateDir);
    updateService = new UpdateService(plugin.getPluginMeta().getVersion(), updateAdapter);
    startupLogger.checkingForUpdates();
    updateCheck =
        updateService
            .checkNow("stable")
            .thenAccept(
                info -> {
                  if (info.updateAvailable()) {
                    plugin
                        .getLogger()
                        .info(
                            "Update available: "
                                + info.currentVersion()
                                + " -> "
                                + info.latestVersion()
                                + ". Run /spectraevents update info");
                  }
                })
            .exceptionally(
                exception -> {
                  plugin
                      .getLogger()
                      .warning("Could not check for updates: " + exception.getMessage());
                  return null;
                });
    org.bukkit.Bukkit.getPluginManager()
        .registerEvents(new UpdateNotificationListener(updateService, locales), plugin);

    AdminGuiController guiController =
        new AdminGuiController(
            application.definitionRegistry(),
            sqliteRepository,
            integrationRegistry,
            updateService,
            application.orchestrationService(),
            eventSettingsStore,
            locales);
    org.bukkit.Bukkit.getPluginManager().registerEvents(guiController, plugin);

    PaperInteractionRouter interactionRouter =
        new PaperInteractionRouter(
            application.orchestrationService(), application.executionEngine(), locales);

    PaperEntityDeathRouter entityDeathRouter =
        new PaperEntityDeathRouter(
            application.orchestrationService(), application.executionEngine());

    org.bukkit.Bukkit.getPluginManager().registerEvents(interactionRouter, plugin);
    org.bukkit.Bukkit.getPluginManager()
        .registerEvents(
            new PaperSharedLootListener(plugin, sqliteRepository, application.executionEngine()),
            plugin);
    org.bukkit.Bukkit.getPluginManager().registerEvents(entityDeathRouter, plugin);
    org.bukkit.Bukkit.getPluginManager().registerEvents(actionAdapter.bossBarManager(), plugin);
    org.bukkit.Bukkit.getPluginManager().registerEvents(actionAdapter.scoreboardManager(), plugin);

    SpectraMainCommand mainCommand =
        new SpectraMainCommand(
            plugin,
            application.orchestrationService(),
            application.definitionRegistry(),
            sqliteRepository,
            definitionConfigBootstrap,
            integrationRegistry,
            updateService,
            guiController,
            application,
            eventSettingsStore,
            actionAdapter,
            regionScheduler,
            dataLayout.schedulesFile(),
            locales);

    plugin
        .getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            event -> {
              event
                  .registrar()
                  .register(
                      mainCommand.buildCommand().build(),
                      "SpectraEvents management command",
                      List.of("se"));
            });

    startupLogger.enabled((System.nanoTime() - startedAt) / 1_000_000L);
  }

  public void disable() {
    if (updateCheck != null) {
      updateCheck.cancel(true);
      updateCheck = null;
    }
    if (resourcePackDelivery != null) {
      resourcePackDelivery.cancel(true);
      resourcePackDelivery = null;
    }
    if (actionAdapter != null) {
      actionAdapter.cleanupAll();
      actionAdapter = null;
    } else if (cleaner != null) {
      cleaner.cleanupAll();
    }
    if (application != null) {
      application.stop();
      application = null;
    }
    plugin.getLogger().info("SpectraEvents disabled cleanly.");
  }

  private void extractBundledTemplates() {
    Path target = plugin.getDataFolder().toPath().resolve("templates").resolve("metin.spectra.zip");
    if (!java.nio.file.Files.exists(target)) {
      plugin.saveResource("templates/metin.spectra.zip", false);
    }
  }
}
