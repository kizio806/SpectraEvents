package io.github.kizio806.spectraevents.platform.bukkit.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackState;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/** Base listener adapter managing player resource-pack delivery on Bukkit-based servers. */
public abstract class AbstractBukkitPlayerResourcePackAdapter implements Listener {

  protected final PlayerResourcePackService service;
  protected final String pluginVersion;
  protected final AssetTargetProfile profile;
  protected final String sourceConfigId;
  protected final LocaleCatalog locales;

  protected AbstractBukkitPlayerResourcePackAdapter(
      PlayerResourcePackService service,
      String pluginVersion,
      AssetTargetProfile profile,
      String sourceConfigId,
      LocaleCatalog locales) {
    this.service = Objects.requireNonNull(service, "service");
    this.pluginVersion = Objects.requireNonNull(pluginVersion, "pluginVersion");
    this.profile = Objects.requireNonNull(profile, "profile");
    this.sourceConfigId = Objects.requireNonNull(sourceConfigId, "sourceConfigId");
    this.locales = Objects.requireNonNull(locales, "locales");
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    Optional<ResourcePackDescriptor> packOpt =
        service.getAvailablePack(pluginVersion, profile, sourceConfigId);

    if (packOpt.isPresent()) {
      ResourcePackDescriptor pack = packOpt.get();
      service.updateState(playerId, PlayerResourcePackState.REQUESTED);
      sendResourcePack(event.getPlayer(), pack);
    }
  }

  @EventHandler
  public void onPackStatus(PlayerResourcePackStatusEvent event) {
    if (event == null || event.getStatus() == null) {
      return;
    }
    PlayerResourcePackState state = mapStatus(event.getStatus());
    if (state == null) {
      return;
    }
    service.updateState(event.getPlayer().getUniqueId(), state);
    if (service.isRequired()
        && (state == PlayerResourcePackState.DECLINED
            || state == PlayerResourcePackState.FAILED
            || state == PlayerResourcePackState.DISCARDED)) {
      kickPlayer(event.getPlayer(), locales.message("messages.resource-pack-required"));
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    if (event != null && event.getPlayer() != null) {
      service.removePlayer(event.getPlayer().getUniqueId());
    }
  }

  protected abstract void sendResourcePack(Player player, ResourcePackDescriptor pack);

  protected abstract void kickPlayer(Player player, String message);

  public PlayerResourcePackState mapStatus(PlayerResourcePackStatusEvent.Status status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case ACCEPTED -> PlayerResourcePackState.ACCEPTED;
      case DECLINED -> PlayerResourcePackState.DECLINED;
      case FAILED_DOWNLOAD -> PlayerResourcePackState.FAILED;
      case SUCCESSFULLY_LOADED -> PlayerResourcePackState.LOADED;
      case DOWNLOADED -> PlayerResourcePackState.DOWNLOADED;
      case INVALID_URL -> PlayerResourcePackState.FAILED;
      case FAILED_RELOAD -> PlayerResourcePackState.FAILED;
      case DISCARDED -> PlayerResourcePackState.DISCARDED;
      default -> null;
    };
  }

  protected static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }
}
