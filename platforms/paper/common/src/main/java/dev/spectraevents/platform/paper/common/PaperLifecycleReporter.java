package dev.spectraevents.platform.paper.common;

import dev.spectraevents.application.port.LifecycleReporter;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/** Paper platform implementation of LifecycleReporter. */
public final class PaperLifecycleReporter implements LifecycleReporter {
  private final Logger logger;

  public PaperLifecycleReporter(Plugin plugin) {
    this.logger = Objects.requireNonNull(plugin, "plugin").getLogger();
  }

  @Override
  public void started() {
    logger.info("SpectraEvents engine started successfully.");
  }

  @Override
  public void stopped() {
    logger.info("SpectraEvents engine stopped.");
  }
}
