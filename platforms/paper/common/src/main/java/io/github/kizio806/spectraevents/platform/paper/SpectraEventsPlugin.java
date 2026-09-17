package io.github.kizio806.spectraevents.platform.paper;

import org.bukkit.plugin.java.JavaPlugin;

/** Paper family plugin entrypoint for SpectraEvents. */
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
