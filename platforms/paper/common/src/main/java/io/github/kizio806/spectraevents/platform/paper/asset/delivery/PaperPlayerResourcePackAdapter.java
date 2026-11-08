package io.github.kizio806.spectraevents.platform.paper.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.platform.bukkit.asset.delivery.AbstractBukkitPlayerResourcePackAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class PaperPlayerResourcePackAdapter extends AbstractBukkitPlayerResourcePackAdapter {

  public PaperPlayerResourcePackAdapter(
      PlayerResourcePackService service,
      String pluginVersion,
      AssetTargetProfile profile,
      String sourceConfigId,
      LocaleCatalog locales) {
    super(service, pluginVersion, profile, sourceConfigId, locales);
  }

  @Override
  protected void sendResourcePack(Player player, ResourcePackDescriptor pack) {
    Component prompt = MiniMessage.miniMessage().deserialize(service.getPrompt());
    player.setResourcePack(pack.url(), pack.sha1(), service.isRequired(), prompt);
  }

  @Override
  protected void kickPlayer(Player player, String message) {
    player.kick(Component.text(message));
  }
}
