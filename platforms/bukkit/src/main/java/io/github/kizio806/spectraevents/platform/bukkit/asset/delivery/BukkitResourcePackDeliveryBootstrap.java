package io.github.kizio806.spectraevents.platform.bukkit.asset.delivery;

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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/** Shared bootstrap configuring player resource-pack delivery for Bukkit-based distributions. */
public final class BukkitResourcePackDeliveryBootstrap {

  public static final String CONFIG_FILE = "resource-pack.yml";

  public static final String DEFAULT_CONFIGURATION =
      """
      # =============================================================================
      # SpectraEvents — Player Resource-Pack Delivery
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
      # Prompt shown before the client downloads the pack. An i18n:key is resolved
      # from the selected server locale; plain custom text is kept unchanged.
      prompt: "i18n:messages.resource-pack-prompt"
      """;

  private BukkitResourcePackDeliveryBootstrap() {}

  public static CompletableFuture<Void> configure(
      Plugin plugin,
      String pluginVersion,
      AssetTargetProfile profile,
      LocaleCatalog locales,
      BiFunction<PlayerResourcePackService, ResourcePackDeliverySettings, Listener>
          listenerFactory) {
    Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(pluginVersion, "pluginVersion");
    Objects.requireNonNull(profile, "profile");
    Objects.requireNonNull(locales, "locales");
    Objects.requireNonNull(listenerFactory, "listenerFactory");

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
                minecraftVersion(plugin),
                settings.modrinthVersionId());
      } else {
        source = new ManualUrlResourcePackSource(settings.url(), settings.sha1());
      }

      ResourcePackDeliveryCoordinator coordinator =
          new ResourcePackDeliveryCoordinator(
              source, cache, pluginVersion, profile, settings.sourceConfigId());

      plugin
          .getServer()
          .getPluginManager()
          .registerEvents(listenerFactory.apply(service, settings), plugin);

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

  public static boolean isEnabled(Plugin plugin) {
    try {
      return readSettings(plugin.getDataFolder().toPath()).enabled();
    } catch (RuntimeException | IOException exception) {
      return false;
    }
  }

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

  public static String minecraftVersion(Plugin plugin) {
    String bukkitVersion = plugin.getServer().getBukkitVersion();
    int separator = bukkitVersion.indexOf('-');
    return separator < 0 ? bukkitVersion : bukkitVersion.substring(0, separator);
  }
}
