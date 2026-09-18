package io.github.kizio806.spectraevents.platform.spigot;

import io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import io.github.kizio806.spectraevents.platform.spigot.lifecycle.SpigotEntityReconciler;
import io.github.kizio806.spectraevents.platform.spigot.render.SpigotModelRenderer;
import java.nio.file.Path;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.Plugin;

public class SpigotBootstrap {
  private final Plugin plugin;
  private final BukkitAudiences adventure;
  private SpectraEventsApplication application;
  private SpigotModelRenderer renderer;

  public SpigotBootstrap(Plugin plugin, BukkitAudiences adventure) {
    this.plugin = plugin;
    this.adventure = adventure;
  }

  public void onEnable() {
    plugin
        .getLogger()
        .info(
            "[SpectraEvents] SpectraEvents " + plugin.getDescription().getVersion() + " starting");
    plugin.getLogger().info("[SpectraEvents] Platform: Spigot");

    plugin.getDataFolder().mkdirs();
    Path dbPath = plugin.getDataFolder().toPath().resolve("events.db");
    SQLiteEventInstanceRepository sqliteRepository = new SQLiteEventInstanceRepository(dbPath);

    renderer = new SpigotModelRenderer(plugin);
    SpigotActionAdapter actionAdapter = new SpigotActionAdapter(plugin, renderer, adventure);
    SpigotEntityReconciler reconciler = new SpigotEntityReconciler(plugin, renderer);
    SpigotEventTaskScheduler eventTaskScheduler = new SpigotEventTaskScheduler(plugin);

    application =
        new SpectraEventsApplication(
            new SpigotLifecycleReporter(plugin),
            eventTaskScheduler,
            actionAdapter,
            sqliteRepository,
            reconciler,
            new SpigotCapabilityQuery(),
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

    application.start();
  }

  public void onDisable() {
    if (application != null) {
      application.stop();
    }
    plugin.getLogger().info("[SpectraEvents] SpectraEvents disabled cleanly.");
  }
}
