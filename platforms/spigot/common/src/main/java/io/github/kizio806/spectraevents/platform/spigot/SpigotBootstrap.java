package io.github.kizio806.spectraevents.platform.spigot;

import io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchProjectReader;
import io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository;
import io.github.kizio806.spectraevents.adapter.update.http.HttpUpdateAdapter;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.ImportedAssetModelRegistrar;
import io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder;
import io.github.kizio806.spectraevents.application.config.DataDirectoryLayout;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext;
import io.github.kizio806.spectraevents.application.model.animation.runtime.ModelAnimationActionService;
import io.github.kizio806.spectraevents.application.service.EntityReconciliationReport;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.bukkit.asset.delivery.BukkitResourcePackDeliveryBootstrap;
import io.github.kizio806.spectraevents.platform.bukkit.common.BukkitLifecycleReporter;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitScheduleBootstrapHelper;
import io.github.kizio806.spectraevents.platform.bukkit.gui.AdminGuiController;
import io.github.kizio806.spectraevents.platform.bukkit.integration.BukkitIntegrationManager;
import io.github.kizio806.spectraevents.platform.bukkit.lifecycle.BukkitBootstrapLifecycle;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootListener;
import io.github.kizio806.spectraevents.platform.bukkit.util.BukkitPluginVersions;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import io.github.kizio806.spectraevents.platform.spigot.asset.delivery.SpigotPlayerResourcePackAdapter;
import io.github.kizio806.spectraevents.platform.spigot.command.SpigotMainCommand;
import io.github.kizio806.spectraevents.platform.spigot.interaction.SpigotEventRouter;
import io.github.kizio806.spectraevents.platform.spigot.lifecycle.SpigotEntityReconciler;
import io.github.kizio806.spectraevents.platform.spigot.render.SpigotModelRenderer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/** Composition root for the Spigot/Bukkit distribution. */
public final class SpigotBootstrap implements BukkitBootstrapLifecycle {
  private final JavaPlugin plugin;
  private final BukkitAudiences adventure;
  private SpectraEventsApplication application;
  private SQLiteEventInstanceRepository repository;
  private SpigotActionAdapter actionAdapter;
  private CompletableFuture<Void> resourcePackDelivery;

  public SpigotBootstrap(JavaPlugin plugin, BukkitAudiences adventure) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.adventure = Objects.requireNonNull(adventure, "adventure");
  }

  @Override
  public void onEnable() {
    plugin
        .getLogger()
        .info(
            "SpectraEvents "
                + BukkitPluginVersions.resolveVersion(plugin)
                + " starting on Spigot/Bukkit");

    Path dataDirectory = plugin.getDataFolder().toPath();
    DataDirectoryLayout dataLayout;
    try {
      dataLayout = DataDirectoryLayout.prepare(dataDirectory);
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not prepare the SpectraEvents data directory", exception);
    }
    LocaleCatalog locales;
    try {
      locales =
          new FileSystemLocaleLoader(dataLayout.root())
              .loadConfiguredCatalog(dataLayout.configFile());
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not prepare the SpectraEvents locale catalog", exception);
    }
    repository = new SQLiteEventInstanceRepository(dataLayout.databaseFile());
    repository.initialize();

    SpigotModelRenderer renderer = new SpigotModelRenderer(plugin);
    actionAdapter = new SpigotActionAdapter(plugin, adventure, locales);
    SpigotEntityReconciler reconciler = new SpigotEntityReconciler(plugin, actionAdapter);
    SpigotEventTaskScheduler scheduler = new SpigotEventTaskScheduler(plugin);

    application =
        new SpectraEventsApplication(
            new BukkitLifecycleReporter(plugin),
            scheduler,
            actionAdapter,
            repository,
            reconciler,
            new SpigotCapabilityQuery(),
            renderer);
    BukkitIntegrationManager integrationManager =
        new BukkitIntegrationManager(application.integrationRegistry());
    integrationManager.detectAll();
    AssetTargetProfile assetTargetProfile =
        AssetTargetProfile.forMinecraftVersion(minecraftVersion());
    application.setAssetPipelineService(
        new AssetPipelineService(
            new BlockbenchProjectReader(),
            new ResourcePackBuilder(dataLayout.resourcePackCacheDirectory()),
            dataDirectory.resolve("assets").resolve("source"),
            assetTargetProfile,
            new ImportedAssetModelRegistrar(
                application.modelCompiler(),
                application.modelDefinitionRegistry(),
                application.animationDefinitionRegistry())));
    resourcePackDelivery =
        BukkitResourcePackDeliveryBootstrap.configure(
            plugin,
            BukkitPluginVersions.resolveVersion(plugin),
            assetTargetProfile,
            locales,
            (service, settings) ->
                new SpigotPlayerResourcePackAdapter(
                    service,
                    BukkitPluginVersions.resolveVersion(plugin),
                    assetTargetProfile,
                    settings.sourceConfigId(),
                    locales));
    actionAdapter.setModelRuntimeService(application.modelRuntimeService());
    actionAdapter.setModelAnimationActionService(
        new ModelAnimationActionService(
            application.modelRuntimeService(), application.animationRuntimeService()));

    extractBundledTemplates();
    application.assetPipelineService().buildAssets();
    BukkitDefinitionConfigBootstrap definitions =
        new BukkitDefinitionConfigBootstrap(plugin, application.definitionLoader());
    loadDefinitions(definitions);

    BukkitScheduleBootstrapHelper.loadAndRegisterSchedules(
        dataLayout.schedulesFile(), application, plugin.getLogger());

    EntityReconciliationReport reconciliation = application.reconciliationService().reconcileAll();
    application.setLastReconciliationReport(reconciliation);
    application.start();
    IntegrationInitializationContext integrationContext =
        new IntegrationInitializationContext(application, repository, plugin.getLogger());
    integrationManager.initializeModules(plugin, integrationContext);
    plugin
        .getLogger()
        .info(
            "Reconciliation: recovered="
                + reconciliation.instancesRecovered()
                + " reconnected="
                + reconciliation.entitiesReconnected()
                + " orphans="
                + reconciliation.orphansRemoved());

    plugin.getServer().getPluginManager().registerEvents(actionAdapter, plugin);
    plugin
        .getServer()
        .getPluginManager()
        .registerEvents(
            new SpigotEventRouter(
                application.orchestrationService(), application.executionEngine()),
            plugin);
    plugin
        .getServer()
        .getPluginManager()
        .registerEvents(
            new BukkitSharedLootListener(
                plugin,
                repository,
                application.executionEngine(),
                (player, task) -> Bukkit.getScheduler().runTask(plugin, task)),
            plugin);

    Path updateDir = plugin.getDataFolder().toPath().resolve("update");
    HttpUpdateAdapter updateAdapter = new HttpUpdateAdapter(updateDir);
    UpdateService updateService =
        new UpdateService(BukkitPluginVersions.resolveVersion(plugin), updateAdapter);

    BukkitEventSettingsStore eventSettingsStore = new BukkitEventSettingsStore(plugin);
    AdminGuiController adminGuiController =
        new AdminGuiController(
            application.definitionRegistry(),
            repository,
            application.integrationRegistry(),
            updateService,
            application.orchestrationService(),
            eventSettingsStore,
            locales);
    plugin.getServer().getPluginManager().registerEvents(adminGuiController, plugin);

    PluginCommand eventCommand = plugin.getCommand("spectraevents");
    if (eventCommand == null) {
      throw new IllegalStateException("plugin.yml does not declare the /spectraevents command");
    }
    SpigotMainCommand commandHandler =
        new SpigotMainCommand(
            plugin,
            application,
            repository,
            definitions,
            actionAdapter,
            eventSettingsStore,
            adminGuiController,
            updateService,
            locales);
    eventCommand.setExecutor(commandHandler);
    eventCommand.setTabCompleter(commandHandler);

    plugin
        .getLogger()
        .info(
            "READY platform=spigot definitions="
                + application.definitionRegistry().getAll().size());
  }

  private String minecraftVersion() {
    String bukkitVersion = plugin.getServer().getBukkitVersion();
    int separator = bukkitVersion.indexOf('-');
    return separator < 0 ? bukkitVersion : bukkitVersion.substring(0, separator);
  }

  @Override
  public void onDisable() {
    if (resourcePackDelivery != null) {
      resourcePackDelivery.cancel(true);
      resourcePackDelivery = null;
    }
    if (actionAdapter != null) {
      try {
        actionAdapter.cleanupAll();
      } catch (Exception exception) {
        plugin.getLogger().log(Level.WARNING, "Error cleaning up action adapter", exception);
      }
      actionAdapter = null;
    }
    if (application != null) {
      try {
        application.stop();
      } catch (Exception exception) {
        plugin.getLogger().log(Level.WARNING, "Error stopping application", exception);
      }
      application = null;
      repository = null;
    } else if (repository != null) {
      try {
        repository.close();
      } catch (Exception exception) {
        plugin.getLogger().log(Level.WARNING, "Error closing SQLite repository", exception);
      }
      repository = null;
    }
    try {
      HandlerList.unregisterAll(plugin);
    } catch (Exception exception) {
      plugin.getLogger().log(Level.WARNING, "Error unregistering Bukkit listeners", exception);
    }
    try {
      Bukkit.getScheduler().cancelTasks(plugin);
    } catch (Exception exception) {
      plugin.getLogger().log(Level.WARNING, "Error cancelling Bukkit scheduler tasks", exception);
    }
    plugin.getLogger().info("SpectraEvents disabled cleanly.");
  }

  private void loadDefinitions(BukkitDefinitionConfigBootstrap definitions) {
    try {
      DefinitionLoadResult result = definitions.loadFromDisk();
      definitions.logLoadResult(result);
      if (result.loaded().isEmpty()) {
        plugin
            .getLogger()
            .info(
                "No active event definitions are installed. Install a template with "
                    + "/spectraevents template install metin.");
      }
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to load event definitions", exception);
    }
  }

  private void extractBundledTemplates() {
    Path target = dataDirectory().resolve("templates").resolve("metin.spectra.zip");
    if (!java.nio.file.Files.exists(target)) {
      plugin.saveResource("templates/metin.spectra.zip", false);
    }
  }

  private Path dataDirectory() {
    return plugin.getDataFolder().toPath();
  }
}
