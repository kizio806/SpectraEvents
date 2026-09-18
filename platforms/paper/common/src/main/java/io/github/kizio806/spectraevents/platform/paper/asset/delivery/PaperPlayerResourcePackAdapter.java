package io.github.kizio806.spectraevents.platform.paper.asset.delivery;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackService;
import io.github.kizio806.spectraevents.application.asset.delivery.PlayerResourcePackState;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDescriptor;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class PaperPlayerResourcePackAdapter implements Listener {

  private final PlayerResourcePackService service;
  private final String pluginVersion;
  private final AssetTargetProfile profile;
  private final String sourceConfigId;

  public PaperPlayerResourcePackAdapter(
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

      Component prompt = MiniMessage.miniMessage().deserialize(service.getPrompt());

      event.getPlayer().setResourcePack(pack.url(), pack.sha1(), service.isRequired(), prompt);
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
          case DOWNLOADED -> PlayerResourcePackState.DOWNLOADED;
          case INVALID_URL -> PlayerResourcePackState.FAILED;
          case FAILED_RELOAD -> PlayerResourcePackState.FAILED;
          case DISCARDED -> PlayerResourcePackState.DECLINED;
          default -> PlayerResourcePackState.FAILED;
        };
    service.updateState(event.getPlayer().getUniqueId(), state);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    service.removePlayer(event.getPlayer().getUniqueId());
  }
}
