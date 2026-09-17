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
            new SpigotCapabilityQuery());

    application.start();
  }

  public void onDisable() {
    if (application != null) {
      application.stop();
    }
  }
}
