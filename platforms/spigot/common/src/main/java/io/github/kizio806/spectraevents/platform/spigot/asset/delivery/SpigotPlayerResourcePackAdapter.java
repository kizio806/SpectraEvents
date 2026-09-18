package io.github.kizio806.spectraevents.platform.spigot.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackState;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class SpigotPlayerResourcePackAdapter implements Listener {

  private final PlayerResourcePackService service;
  private final String pluginVersion;
  private final AssetTargetProfile profile;
  private final String sourceConfigId;

  public SpigotPlayerResourcePackAdapter(
      PlayerResourcePackService service,
      String pluginVersion,
      AssetTargetProfile profile,
      String sourceConfigId) {
    this.service = service;
    this.pluginVersion = pluginVersion;
    this.profile = profile;
    this.sourceConfigId = sourceConfigId;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    Optional<ResourcePackDescriptor> packOpt =
        service.getAvailablePack(pluginVersion, profile, sourceConfigId);

    if (packOpt.isPresent()) {
      ResourcePackDescriptor pack = packOpt.get();
      service.updateState(playerId, PlayerResourcePackState.REQUESTED);

      byte[] hashBytes = hexStringToByteArray(pack.sha1());

      // Note: In real Spigot API prompt and force might be supported.
      // Using standard Bukkit API method that accepts hash and prompt.
      // Some older Spigot versions don't have prompt/force, but since we target 1.21.3 it does.
      try {
        event
            .getPlayer()
            .setResourcePack(pack.url(), hashBytes, service.getPrompt(), service.isRequired());
      } catch (NoSuchMethodError e) {
        // Fallback for older Spigot
        event.getPlayer().setResourcePack(pack.url(), hashBytes);
      }
    }
  }

  @EventHandler
  public void onPackStatus(PlayerResourcePackStatusEvent event) {
    PlayerResourcePackState state =
        switch (event.getStatus()) {
          case ACCEPTED -> PlayerResourcePackState.ACCEPTED;
          case DECLINED -> PlayerResourcePackState.DECLINED;
          case FAILED_DOWNLOAD -> PlayerResourcePackState.FAILED;
          case SUCCESSFULLY_LOADED -> PlayerResourcePackState.LOADED;
          default -> PlayerResourcePackState.FAILED;
        };
    service.updateState(event.getPlayer().getUniqueId(), state);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    service.removePlayer(event.getPlayer().getUniqueId());
  }

  private byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }
}
