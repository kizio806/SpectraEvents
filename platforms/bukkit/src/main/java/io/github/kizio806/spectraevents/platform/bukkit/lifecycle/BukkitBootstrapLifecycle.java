package io.github.kizio806.spectraevents.platform.bukkit.lifecycle;

/** Represents a Bukkit/Spigot bootstrap lifecycle that can be enabled and disabled. */
public interface BukkitBootstrapLifecycle {

  /** Executes platform bootstrap initialization. */
  void onEnable();

  /** Releases all allocated platform resources and shuts down services cleanly. */
  void onDisable();
}
