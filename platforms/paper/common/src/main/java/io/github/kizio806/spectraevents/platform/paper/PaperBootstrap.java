package io.github.kizio806.spectraevents.platform.paper;

import io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository;
import io.github.kizio806.spectraevents.adapter.update.http.HttpUpdateAdapter;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
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
        plugin.getPluginMeta().getVersion(), plugin.getPluginMeta().getAPIVersion());
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
    eventSettingsStore = new PaperEventSettingsStore(plugin);
    startupLogger.storage(dbPath, sqliteRepository.findAll().size());

    PaperActionAdapter actionAdapter = new PaperActionAdapter(regionScheduler, cleaner);
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
            plugin.getDataFolder().toPath().resolve("generated").resolve("resource-pack"));
    io.github.kizio806.spectraevents.application.asset.AssetPipelineService assetPipelineService =
        new io.github.kizio806.spectraevents.application.asset.AssetPipelineService(
            bbReader,
            rpBuilder,
            plugin.getDataFolder().toPath().resolve("assets").resolve("source"),
            io.github.kizio806.spectraevents.application.asset.AssetTargetProfile.PROFILE_26_1,
            new io.github.kizio806.spectraevents.application.asset.ImportedAssetModelRegistrar(
                application.modelCompiler(),
                application.modelDefinitionRegistry(),
                application.animationDefinitionRegistry()));
    application.setAssetPipelineService(assetPipelineService);
    resourcePackDelivery =
        io.github.kizio806.spectraevents.platform.paper.asset.delivery
            .PaperResourcePackDeliveryBootstrap.configure(
            plugin,
            plugin.getPluginMeta().getVersion(),
            io.github.kizio806.spectraevents.application.asset.AssetTargetProfile.PROFILE_26_1);

    actionAdapter.setModelRuntimeService(application.modelRuntimeService());
    actionAdapter.setModelAnimationActionService(
        new ModelAnimationActionService(
            application.modelRuntimeService(), application.animationRuntimeService()));

    extractBundledAssetSources();
    assetPipelineService.buildAssets();

    // Load 3D Models
    try {
      io.github.kizio806.spectraevents.application.model.loader.FileSystemModelLoader
          modelFileSystemLoader =
              new io.github.kizio806.spectraevents.application.model.loader.FileSystemModelLoader(
                  plugin.getDataFolder().toPath(), application.modelLoader());
      var modelLoadResult = modelFileSystemLoader.loadFromDisk();
      startupLogger.models(modelLoadResult.loadedCount(), modelLoadResult.invalidCount());
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to load 3D models: " + e.getMessage());
    }

    definitionConfigBootstrap =
        new PaperDefinitionConfigBootstrap(plugin, application.definitionLoader());

    try {
      definitionConfigBootstrap.ensureDefaultConfiguration();
      var definitionLoadResult = definitionConfigBootstrap.loadFromDisk();
      definitionConfigBootstrap.logLoadResult(definitionLoadResult);

      // Reconcile entities after definitions are loaded
      io.github.kizio806.spectraevents.application.service.EntityReconciliationReport report =
          application.reconciliationService().reconcileAll();
      application.setLastReconciliationReport(report);
      List<String> definitionIds =
          application.definitionRegistry().getAll().stream()
              .map(registered -> registered.definition().id().value())
              .toList();
      startupLogger.definitions(definitionIds, definitionLoadResult.failures().size());
    } catch (Exception e) {
      plugin
          .getLogger()
          .severe("Failed to initialize event definitions or reconciliation: " + e.getMessage());
    }

    application.start();

    // Initialize specific integrations
    new PlaceholderAPIIntegration(sqliteRepository, application.executionEngine().stateStore());

    application.executionEngine().registerConditionResolver(new LuckPermsIntegration());
    application.executionEngine().registerConditionResolver(new WorldGuardIntegration());
    application.executionEngine().registerActionResolver(new VaultIntegration());
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
        .registerEvents(new UpdateNotificationListener(updateService), plugin);

    AdminGuiController guiController =
        new AdminGuiController(
            application.definitionRegistry(),
            sqliteRepository,
            integrationRegistry,
            updateService,
            application.orchestrationService(),
            eventSettingsStore);
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
            application,
            eventSettingsStore);

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
    if (cleaner != null) {
      cleaner.cleanupAll();
    }
    if (application != null) {
      application.stop();
      application = null;
    }
    plugin.getLogger().info("SpectraEvents disabled cleanly.");
  }

  private void extractBundledAssetSources() {
    for (String file :
        List.of(
            "meteor_core.bbmodel",
            "airdrop_crate.bbmodel",
            "metin_stone.bbmodel",
            "pinata.bbmodel",
            "boss_portal.bbmodel")) {
      Path target =
          plugin.getDataFolder().toPath().resolve("assets").resolve("source").resolve(file);
      if (!java.nio.file.Files.exists(target)) {
        plugin.saveResource("assets/source/" + file, false);
      }
    }
  }
}
