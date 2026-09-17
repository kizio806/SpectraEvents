package dev.spectraevents.platform.paper.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Port for dispatching tasks to region or entity-owned execution threads in Folia/Paper. */
public interface RegionTaskScheduler {
  void executeAt(Location location, Runnable task);

  void executeFor(Entity entity, Runnable task);

  void executeFor(Player player, Runnable task);
}
