package io.github.kizio806.spectraevents.platform.paper.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.platform.bukkit.asset.delivery.BukkitResourcePackDeliveryBootstrap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.bukkit.plugin.java.JavaPlugin;

/** Configures player resource-pack delivery for Paper distributions. */
public final class PaperResourcePackDeliveryBootstrap {

  private PaperResourcePackDeliveryBootstrap() {}

  public static CompletableFuture<Void> configure(
      JavaPlugin plugin, String pluginVersion, AssetTargetProfile profile, LocaleCatalog locales) {
    return BukkitResourcePackDeliveryBootstrap.configure(
        plugin,
        pluginVersion,
        profile,
        locales,
        (service, settings) ->
            new PaperPlayerResourcePackAdapter(
                service, pluginVersion, profile, settings.sourceConfigId(), locales));
  }

  public static boolean isEnabled(JavaPlugin plugin) {
    return BukkitResourcePackDeliveryBootstrap.isEnabled(plugin);
  }

  public static ResourcePackDeliverySettings readSettings(Path dataDirectory) throws IOException {
    return BukkitResourcePackDeliveryBootstrap.readSettings(dataDirectory);
  }
}
