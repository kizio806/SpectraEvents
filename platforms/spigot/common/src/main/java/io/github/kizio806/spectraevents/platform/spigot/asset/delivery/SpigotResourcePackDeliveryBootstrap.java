package io.github.kizio806.spectraevents.platform.spigot.asset.delivery;

import io.github.kizio806.spectraevents.adapter.delivery.manual.ManualUrlResourcePackSource;
import io.github.kizio806.spectraevents.adapter.delivery.modrinth.ModrinthApiClient;
import io.github.kizio806.spectraevents.adapter.delivery.modrinth.ModrinthResourcePackSource;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliveryCoordinator;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptorCache;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Loads explicit administrator-owned manual pack delivery settings for the Spigot family. */
public final class SpigotResourcePackDeliveryBootstrap {
  private static final String CONFIG_FILE = "resource-pack.yml";
  private static final String DEFAULT_CONFIGURATION =
      """
      # Host the locally generated ZIP on HTTPS, then opt in explicitly.
      enabled: false
      required: false
      url: ""
      sha1: ""
      modrinthProjectId: ""
      modrinthVersionId: ""
      prompt: "Server resources are required for SpectraEvents."
      """;

  private SpigotResourcePackDeliveryBootstrap() {}

  public static CompletableFuture<Void> configure(
      JavaPlugin plugin, String pluginVersion, AssetTargetProfile profile) {
    try {
      ResourcePackDeliverySettings settings = loadSettings(plugin.getDataFolder().toPath());
      if (!settings.enabled()) {
        plugin
            .getLogger()
            .info("[SpectraEvents] Resource-pack delivery is disabled by configuration.");
        return CompletableFuture.completedFuture(null);
      }

      ResourcePackDescriptorCache cache = new ResourcePackDescriptorCache();
      PlayerResourcePackService service =
          new PlayerResourcePackService(cache, settings.required(), settings.prompt());
      ResourcePackDeliveryCoordinator coordinator =
          new ResourcePackDeliveryCoordinator(
              settings.modrinthProjectId().isBlank()
                  ? new ManualUrlResourcePackSource(settings.url(), settings.sha1())
                  : new ModrinthResourcePackSource(
                      new ModrinthApiClient(pluginVersion),
                      settings.modrinthProjectId(),
                      minecraftVersion(plugin),
                      settings.modrinthVersionId()),
              cache,
              pluginVersion,
              profile,
              settings.sourceConfigId());
      plugin
          .getServer()
          .getPluginManager()
          .registerEvents(
              new SpigotPlayerResourcePackAdapter(
                  service, pluginVersion, profile, settings.sourceConfigId()),
              plugin);
      return coordinator
          .refresh()
          .thenAccept(
              descriptor ->
                  plugin
                      .getLogger()
                      .info(
                          "[SpectraEvents] Resource-pack delivery ready: "
                              + descriptor.url()
                              + " sha1="
                              + descriptor.sha1()))
          .exceptionally(
              exception -> {
                plugin
                    .getLogger()
                    .log(Level.WARNING, "Resource-pack delivery remains unavailable", exception);
                return null;
              });
    } catch (RuntimeException | IOException exception) {
      plugin
          .getLogger()
          .log(
              Level.SEVERE,
              "Resource-pack delivery configuration is invalid and has been disabled",
              exception);
      return CompletableFuture.completedFuture(null);
    }
  }

  /** Returns whether validated delivery is configured, without resolving or publishing a pack. */
  public static boolean isEnabled(JavaPlugin plugin) {
    try {
      return loadSettings(plugin.getDataFolder().toPath()).enabled();
    } catch (RuntimeException | IOException exception) {
      return false;
    }
  }

  private static ResourcePackDeliverySettings loadSettings(Path dataDirectory) throws IOException {
    Path configurationFile = dataDirectory.resolve(CONFIG_FILE);
    if (!Files.exists(configurationFile)) {
      Files.createDirectories(dataDirectory);
      Files.writeString(configurationFile, DEFAULT_CONFIGURATION, StandardCharsets.UTF_8);
    }
    YamlConfiguration configuration =
        YamlConfiguration.loadConfiguration(configurationFile.toFile());
    return new ResourcePackDeliverySettings(
        configuration.getBoolean("enabled", false),
        configuration.getBoolean("required", false),
        configuration.getString("url", ""),
        configuration.getString("sha1", ""),
        configuration.getString("prompt", ResourcePackDeliverySettings.DEFAULT_PROMPT),
        configuration.getString("modrinthProjectId", ""),
        configuration.getString("modrinthVersionId", ""));
  }

  private static String minecraftVersion(JavaPlugin plugin) {
    String bukkitVersion = plugin.getServer().getBukkitVersion();
    int separator = bukkitVersion.indexOf('-');
    return separator < 0 ? bukkitVersion : bukkitVersion.substring(0, separator);
  }
}
