package io.github.kizio806.spectraevents.platform.sponge.v26_2;

import io.github.kizio806.spectraevents.application.port.LifecycleReporter;

public class SpongeLifecycleReporter implements LifecycleReporter {
  private final SpongeBootstrap plugin;

  public SpongeLifecycleReporter(SpongeBootstrap plugin) {
    this.plugin = plugin;
  }

  @Override
  public void started() {
    plugin.getLogger().info("Engine started");
  }

  @Override
  public void stopped() {
    plugin.getLogger().info("Engine stopped");
  }
}
