package io.github.kizio806.spectraevents.platform.spigot;

import io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.DataDirectoryLayout;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.model.animation.runtime.ModelAnimationActionService;
import io.github.kizio806.spectraevents.application.service.EntityReconciliationReport;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import io.github.kizio806.spectraevents.platform.spigot.command.SpigotMainCommand;
import io.github.kizio806.spectraevents.platform.spigot.config.SpigotDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.spigot.interaction.SpigotEventRouter;
import io.github.kizio806.spectraevents.platform.spigot.lifecycle.SpigotEntityReconciler;
import io.github.kizio806.spectraevents.platform.spigot.loot.SpigotSharedLootListener;
import io.github.kizio806.spectraevents.platform.spigot.render.SpigotModelRenderer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Composition root for the Spigot/Bukkit distribution. */
@SuppressWarnings("StringConcatToTextBlock")
public final class SpigotBootstrap {
  private final JavaPlugin plugin;
  private final BukkitAudiences adventure;
  private SpectraEventsApplication application;
  private CompletableFuture<Void> resourcePackDelivery;

  public SpigotBootstrap(JavaPlugin plugin, BukkitAudiences adventure) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.adventure = Objects.requireNonNull(adventure, "adventure");
  }

  public void onEnable() {
    plugin
        .getLogger()
        .info(
            "SpectraEvents " + plugin.getDescription().getVersion() + " starting on Spigot/Bukkit");

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
          new io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader(
                  dataLayout.root())
              .loadConfiguredCatalog(dataLayout.configFile());
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not prepare the SpectraEvents locale catalog", exception);
    }
    SQLiteEventInstanceRepository repository =
        new SQLiteEventInstanceRepository(dataLayout.databaseFile());
    repository.initialize();

    SpigotModelRenderer renderer = new SpigotModelRenderer(plugin);
    SpigotActionAdapter actionAdapter = new SpigotActionAdapter(plugin, adventure, locales);
    SpigotEntityReconciler reconciler = new SpigotEntityReconciler(plugin, actionAdapter);
    SpigotEventTaskScheduler scheduler = new SpigotEventTaskScheduler(plugin);

    application =
        new SpectraEventsApplication(
            new SpigotLifecycleReporter(plugin),
            scheduler,
            actionAdapter,
            repository,
            reconciler,
            new SpigotCapabilityQuery(),
            renderer);
    io.github.kizio806.spectraevents.application.asset.AssetTargetProfile assetTargetProfile =
        io.github.kizio806.spectraevents.application.asset.AssetTargetProfile.forMinecraftVersion(
            minecraftVersion());
    application.setAssetPipelineService(
        new io.github.kizio806.spectraevents.application.asset.AssetPipelineService(
            new io.github.kizio806.spectraevents.adapter.blockbench.BlockbenchProjectReader(),
            new io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder(
                dataLayout.resourcePackCacheDirectory()),
            dataDirectory.resolve("assets").resolve("source"),
            assetTargetProfile,
            new io.github.kizio806.spectraevents.application.asset.ImportedAssetModelRegistrar(
                application.modelCompiler(),
                application.modelDefinitionRegistry(),
                application.animationDefinitionRegistry())));
    resourcePackDelivery =
        io.github.kizio806.spectraevents.platform.spigot.asset.delivery
            .SpigotResourcePackDeliveryBootstrap.configure(
            plugin, plugin.getDescription().getVersion(), assetTargetProfile, locales);
    actionAdapter.setModelRuntimeService(application.modelRuntimeService());
    actionAdapter.setModelAnimationActionService(
        new ModelAnimationActionService(
            application.modelRuntimeService(), application.animationRuntimeService()));

    extractBundledTemplates();
    application.assetPipelineService().buildAssets();
    SpigotDefinitionConfigBootstrap definitions =
        new SpigotDefinitionConfigBootstrap(plugin, application.definitionLoader());
    loadDefinitions(definitions);

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

    EntityReconciliationReport reconciliation = application.reconciliationService().reconcileAll();
    application.setLastReconciliationReport(reconciliation);
    application.start();
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
            new SpigotSharedLootListener(plugin, repository, application.executionEngine()),
            plugin);

    PluginCommand eventCommand = plugin.getCommand("spectraevents");
    if (eventCommand == null) {
      throw new IllegalStateException("plugin.yml does not declare the /spectraevents command");
    }
    SpigotMainCommand commandHandler =
        new SpigotMainCommand(plugin, application, repository, definitions, actionAdapter, locales);
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

  public void onDisable() {
    if (resourcePackDelivery != null) {
      resourcePackDelivery.cancel(true);
      resourcePackDelivery = null;
    }
    if (application != null) {
      application.stop();
      application = null;
    }
    plugin.getLogger().info("SpectraEvents disabled cleanly.");
  }

  private void loadDefinitions(SpigotDefinitionConfigBootstrap definitions) {
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
