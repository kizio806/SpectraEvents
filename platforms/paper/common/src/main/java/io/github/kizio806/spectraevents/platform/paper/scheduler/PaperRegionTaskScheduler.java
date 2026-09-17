package io.github.kizio806.spectraevents.platform.paper.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Real implementation of RegionTaskScheduler using Bukkit / Folia region and entity schedulers. */
public final class PaperRegionTaskScheduler implements RegionTaskScheduler {
  private final Plugin plugin;

  public PaperRegionTaskScheduler(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void executeAt(Location location, Runnable task) {
    Bukkit.getRegionScheduler().execute(plugin, location, task);
  }

  @Override
  public void executeFor(Entity entity, Runnable task) {
    entity.getScheduler().execute(plugin, task, null, 1);
  }

  @Override
  public void executeFor(Player player, Runnable task) {
    player.getScheduler().execute(plugin, task, null, 1);
  }
}
