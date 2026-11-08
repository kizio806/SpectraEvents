package io.github.kizio806.spectraevents.platform.spigot;

import io.github.kizio806.spectraevents.platform.bukkit.lifecycle.BukkitBootstrapLifecycle;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpectraEventsSpigotPlugin extends JavaPlugin {
  private final Function<Plugin, BukkitAudiences> audienceFactory;
  private final BiFunction<JavaPlugin, BukkitAudiences, BukkitBootstrapLifecycle> bootstrapFactory;

  private BukkitAudiences adventure;
  private BukkitBootstrapLifecycle bootstrap;

  public SpectraEventsSpigotPlugin() {
    this(BukkitAudiences::create, SpigotBootstrap::new);
  }

  SpectraEventsSpigotPlugin(
      Function<Plugin, BukkitAudiences> audienceFactory,
      BiFunction<JavaPlugin, BukkitAudiences, BukkitBootstrapLifecycle> bootstrapFactory) {
    this.audienceFactory = Objects.requireNonNull(audienceFactory, "audienceFactory");
    this.bootstrapFactory = Objects.requireNonNull(bootstrapFactory, "bootstrapFactory");
  }

  @Override
  public void onEnable() {
    try {
      this.adventure = audienceFactory.apply(this);
      this.bootstrap = bootstrapFactory.apply(this, adventure);
      this.bootstrap.onEnable();
    } catch (Exception exception) {
      getLogger()
          .log(
              Level.SEVERE,
              "SpectraEvents was not enabled because Spigot startup failed. Shutting down plugin.",
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
                "Error during Spigot bootstrap teardown following startup failure",
                teardownException);
      } finally {
        this.bootstrap = null;
      }
    }
    if (this.adventure != null) {
      try {
        this.adventure.close();
      } catch (Exception closeException) {
        getLogger()
            .log(
                Level.WARNING,
                "Error closing Adventure audiences following startup failure",
                closeException);
      } finally {
        this.adventure = null;
      }
    }
    try {
      HandlerList.unregisterAll(this);
    } catch (Exception handlerError) {
      getLogger()
          .log(Level.FINE, "Error unregistering Bukkit handlers during teardown", handlerError);
    }
    try {
      if (getServer() != null && getServer().getScheduler() != null) {
        getServer().getScheduler().cancelTasks(this);
      }
    } catch (Exception schedulerError) {
      getLogger()
          .log(
              Level.FINE,
              "Error cancelling Bukkit scheduler tasks during teardown",
              schedulerError);
    }
  }

  BukkitAudiences adventure() {
    return adventure;
  }

  BukkitBootstrapLifecycle bootstrap() {
    return bootstrap;
  }
}
