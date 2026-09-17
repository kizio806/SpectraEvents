package dev.spectraevents.platform.paper.v26_2;

import dev.spectraevents.platform.paper.PaperBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper 26.2 plugin entrypoint for SpectraEvents. */
public final class SpectraEventsPlugin extends JavaPlugin {
  private PaperBootstrap bootstrap;

  @Override
  public void onEnable() {
    bootstrap = new PaperBootstrap(this);
    bootstrap.enable();
  }

  @Override
  public void onDisable() {
    if (bootstrap != null) {
      bootstrap.disable();
      bootstrap = null;
    }
  }
}
