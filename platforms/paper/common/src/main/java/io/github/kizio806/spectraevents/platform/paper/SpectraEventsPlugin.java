package io.github.kizio806.spectraevents.platform.paper;

import io.papermc.paper.ServerBuildInfo;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper family plugin entrypoint for SpectraEvents. */
public final class SpectraEventsPlugin extends JavaPlugin {
  private PaperBootstrap bootstrap;

  @Override
  public void onEnable() {
    ServerBuildInfo buildInfo = ServerBuildInfo.buildInfo();
    if (FoliaCompatibilityGuard.shouldRefuse(buildInfo)) {
      getLogger()
          .severe(
              "REFUSING TO ENABLE: Folia for Minecraft "
                  + buildInfo.minecraftVersionId()
                  + " is not supported. "
                  + "This SpectraEvents version supports Folia 26.1 and 26.2 only. "
                  + "Download the newest SpectraEvents release when Folia support for this Minecraft "
                  + "version is restored. No event data was changed.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    bootstrap = new PaperBootstrap(this);
    try {
      bootstrap.enable();
    } catch (RuntimeException exception) {
      getLogger()
          .log(
              java.util.logging.Level.SEVERE,
              "SpectraEvents was not enabled because startup validation failed. No success banner was emitted.",
              exception);
      bootstrap.disable();
      bootstrap = null;
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (bootstrap != null) {
      bootstrap.disable();
      bootstrap = null;
    }
  }
}
