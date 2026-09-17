package io.github.kizio806.spectraevents.platform.spigot.v26_2;

import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

public class SpigotLifecycleReporter implements LifecycleReporter {
  private final Logger logger;

  public SpigotLifecycleReporter(Plugin plugin) {
    this.logger = plugin.getLogger();
  }

  @Override
  public void started() {
    logger.info("Engine started");
  }

  @Override
  public void stopped() {
    logger.info("Engine stopped");
  }
}
