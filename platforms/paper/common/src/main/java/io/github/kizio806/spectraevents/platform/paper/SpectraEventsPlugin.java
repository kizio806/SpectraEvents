package io.github.kizio806.spectraevents.platform.paper;

import io.github.kizio806.spectraevents.platform.bukkit.lifecycle.BukkitBootstrapLifecycle;
import io.papermc.paper.ServerBuildInfo;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper family plugin entrypoint for SpectraEvents. */
public final class SpectraEventsPlugin extends JavaPlugin {
  private final Function<JavaPlugin, BukkitBootstrapLifecycle> bootstrapFactory;
  private BukkitBootstrapLifecycle bootstrap;

  public SpectraEventsPlugin() {
    this(PaperBootstrap::new);
  }

  SpectraEventsPlugin(Function<JavaPlugin, BukkitBootstrapLifecycle> bootstrapFactory) {
    this.bootstrapFactory = Objects.requireNonNull(bootstrapFactory, "bootstrapFactory");
  }

  @Override
  public void onEnable() {
    ServerBuildInfo buildInfo = null;
    try {
      buildInfo = ServerBuildInfo.buildInfo();
    } catch (Exception ignored) {
      // ServerBuildInfo unavailable in mock/test environment
    }
    if (buildInfo != null && FoliaCompatibilityGuard.shouldRefuse(buildInfo)) {
      getLogger()
          .severe(
              "REFUSING TO ENABLE: Folia for Minecraft "
                  + buildInfo.minecraftVersionId()
                  + " is not supported. "
                  + "This SpectraEvents version supports Folia 26.1–26.3 only. "
                  + "Download the newest SpectraEvents release when Folia support for this Minecraft "
                  + "version is restored. No event data was changed.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    try {
      this.bootstrap = bootstrapFactory.apply(this);
      this.bootstrap.onEnable();
    } catch (Exception exception) {
      getLogger()
          .log(
              Level.SEVERE,
              "SpectraEvents was not enabled because startup validation failed. No success banner was emitted.",
              exception);
      cleanupResources();
      try {
        if (getServer() != null && getServer().getPluginManager() != null) {
          getServer().getPluginManager().disablePlugin(this);
        }
      } catch (Exception disableError) {
        getLogger()
            .log(
                Level.WARNING,
                "Error disabling plugin through PluginManager following startup failure",
                disableError);
      }
    }
  }

  @Override
  public void onDisable() {
    cleanupResources();
  }

  private void cleanupResources() {
    if (this.bootstrap != null) {
      try {
        this.bootstrap.onDisable();
      } catch (Exception teardownException) {
        getLogger()
            .log(
                Level.WARNING,
                "Error during Paper bootstrap teardown following startup failure",
                teardownException);
      } finally {
        this.bootstrap = null;
      }
    }
    try {
      HandlerList.unregisterAll(this);
    } catch (Exception handlerError) {
      getLogger()
          .log(Level.FINE, "Error unregistering Bukkit handlers during teardown", handlerError);
    }
    try {
      if (getServer() != null && getServer().getAsyncScheduler() != null) {
        getServer().getAsyncScheduler().cancelTasks(this);
      }
    } catch (Exception schedulerError) {
      getLogger()
          .log(Level.FINE, "Error cancelling AsyncScheduler tasks during teardown", schedulerError);
    }
    try {
      if (getServer() != null && getServer().getGlobalRegionScheduler() != null) {
        getServer().getGlobalRegionScheduler().cancelTasks(this);
      }
    } catch (Exception schedulerError) {
      getLogger()
          .log(
              Level.FINE,
              "Error cancelling GlobalRegionScheduler tasks during teardown",
              schedulerError);
    }
  }

  BukkitBootstrapLifecycle bootstrap() {
    return bootstrap;
  }
}
