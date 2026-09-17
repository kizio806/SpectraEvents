package io.github.kizio806.spectraevents.platform.paper.update;

import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Listens for admin player joins to notify about available plugin updates. */
public final class UpdateNotificationListener implements Listener {
  private final UpdateService updateService;
  private final Set<UUID> notifiedPlayers = ConcurrentHashMap.newKeySet();

  public UpdateNotificationListener(UpdateService updateService) {
    this.updateService = updateService;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (!player.hasPermission("spectraevents.update.notify")
        && !player.hasPermission("spectraevents.admin")
        && !player.isOp()) {
      return;
    }

    if (notifiedPlayers.contains(player.getUniqueId())) {
      return;
    }

    UpdateInfo info = updateService.currentInfo();
    if (info.updateAvailable()) {
      notifiedPlayers.add(player.getUniqueId());
      player.sendMessage(
          Component.text("[SpectraEvents] ", NamedTextColor.DARK_PURPLE)
              .append(Component.text("A new version is available: ", NamedTextColor.YELLOW))
              .append(
                  Component.text(
                      info.currentVersion() + " -> " + info.latestVersion(), NamedTextColor.GREEN))
              .append(Component.text(". Run /spectra update info", NamedTextColor.GRAY)));
    }
  }
}
