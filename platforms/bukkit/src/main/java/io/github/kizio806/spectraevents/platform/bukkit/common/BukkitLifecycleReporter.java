package io.github.kizio806.spectraevents.platform.bukkit.common;

import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/** Shared Bukkit platform lifecycle reporter. */
public class BukkitLifecycleReporter implements LifecycleReporter {

  private final Logger logger;

  public BukkitLifecycleReporter(Plugin plugin) {
    this.logger = Objects.requireNonNull(plugin, "plugin").getLogger();
  }

  @Override
  public void started() {
    logger.info("Engine started.");
  }

  @Override
  public void stopped() {
    logger.info("SpectraEvents stopped.");
  }
}
