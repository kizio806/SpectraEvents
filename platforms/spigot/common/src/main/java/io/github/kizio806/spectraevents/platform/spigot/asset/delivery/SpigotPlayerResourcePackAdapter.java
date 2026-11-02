package io.github.kizio806.spectraevents.platform.spigot.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.platform.bukkit.asset.delivery.AbstractBukkitPlayerResourcePackAdapter;
import org.bukkit.entity.Player;

public class SpigotPlayerResourcePackAdapter extends AbstractBukkitPlayerResourcePackAdapter {

  public SpigotPlayerResourcePackAdapter(
      PlayerResourcePackService service,
      String pluginVersion,
      AssetTargetProfile profile,
      String sourceConfigId,
      LocaleCatalog locales) {
    super(service, pluginVersion, profile, sourceConfigId, locales);
  }

  @Override
  protected void sendResourcePack(Player player, ResourcePackDescriptor pack) {
    byte[] hashBytes = hexStringToByteArray(pack.sha1());
    try {
      player.setResourcePack(pack.url(), hashBytes, service.getPrompt(), service.isRequired());
    } catch (NoSuchMethodError e) {
      player.setResourcePack(pack.url(), hashBytes);
    }
  }

  @Override
  protected void kickPlayer(Player player, String message) {
    player.kickPlayer(message);
  }
}
