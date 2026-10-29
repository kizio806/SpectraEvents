package io.github.kizio806.spectraevents.platform.paper.asset.delivery;

import io.github.kizio806.spectraevents.adapter.delivery.manual.ManualUrlResourcePackSource;
import io.github.kizio806.spectraevents.adapter.delivery.modrinth.ModrinthApiClient;
import io.github.kizio806.spectraevents.adapter.delivery.modrinth.ModrinthResourcePackSource;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliveryCoordinator;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptorCache;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackSourcePort;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Loads administrator-owned Modrinth or manual pack delivery settings for the Paper family. */
public final class PaperResourcePackDeliveryBootstrap {
  private static final String CONFIG_FILE = "resource-pack.yml";
  private static final String DEFAULT_CONFIGURATION =
      """
      # =============================================================================
      # SpectraEvents — Player Resource-Pack Delivery (Paper family)
      # =============================================================================
      # Recommended: set modrinthProjectId after release CI publishes the separate
      # resource-pack project. The server then selects <plugin-version>+<Minecraft line>.
      # Keep enabled/required false until a real client has accepted every target pack.
      enabled: false
      required: false
      # Manual mode: HTTPS ZIP URL and lowercase SHA-1 checksum.
      url: ""
      sha1: ""
      # Modrinth mode: leave modrinthVersionId empty for automatic version selection.
      # Set it only to pin a reviewed rollback version for this server.
      modrinthProjectId: ""
      modrinthVersionId: ""
      # MiniMessage text shown before the client downloads the pack. An i18n:key
      # is resolved from the selected server locale; plain custom text is kept unchanged.
      prompt: "i18n:messages.resource-pack-prompt"
      """;

  private PaperResourcePackDeliveryBootstrap() {}

  public static CompletableFuture<Void> configure(
      JavaPlugin plugin, String pluginVersion, AssetTargetProfile profile, LocaleCatalog locales) {
    try {
      ResourcePackDeliverySettings settings = readSettings(plugin.getDataFolder().toPath());
      if (!settings.enabled()) {
        plugin.getLogger().info("Resource-pack delivery is disabled by configuration.");
        return CompletableFuture.completedFuture(null);
      }

      ResourcePackDescriptorCache cache = new ResourcePackDescriptorCache();
      PlayerResourcePackService service =
          new PlayerResourcePackService(
              cache, settings.required(), locales.resolveTemplate(settings.prompt()));
      ResourcePackSourcePort source;
      if (!settings.modrinthProjectId().isBlank()) {
        source =
            new ModrinthResourcePackSource(
                new ModrinthApiClient(pluginVersion),
                settings.modrinthProjectId(),
                plugin.getServer().getMinecraftVersion(),
                settings.modrinthVersionId());
      } else {
        source = new ManualUrlResourcePackSource(settings.url(), settings.sha1());
      }
      ResourcePackDeliveryCoordinator coordinator =
          new ResourcePackDeliveryCoordinator(
              source, cache, pluginVersion, profile, settings.sourceConfigId());
      Bukkit.getPluginManager()
          .registerEvents(
              new PaperPlayerResourcePackAdapter(
                  service, pluginVersion, profile, settings.sourceConfigId(), locales),
              plugin);
      return coordinator
          .refresh()
          .thenAccept(
              descriptor ->
                  plugin
                      .getLogger()
                      .info(
                          "Resource-pack delivery ready: "
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
      return readSettings(plugin.getDataFolder().toPath()).enabled();
    } catch (RuntimeException | IOException exception) {
      return false;
    }
  }

  /** Reads and validates the operator-owned delivery settings for commands and bootstrapping. */
  public static ResourcePackDeliverySettings readSettings(Path dataDirectory) throws IOException {
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
}
