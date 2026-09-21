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
              "[SpectraEvents] REFUSING TO ENABLE: Folia for Minecraft "
                  + buildInfo.minecraftVersionId()
                  + " is not supported. Verified Folia versions: 26.1 and 26.2. "
                  + "Install a supported Folia server, or use Paper/Purpur for Minecraft 26.3."
                  + " No event data was changed.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
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
