package io.github.kizio806.spectraevents.platform.paper.config;

import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper adapter extending BukkitEventSettingsStore for backward compatibility and paper
 * configuration bindings.
 */
public final class PaperEventSettingsStore extends BukkitEventSettingsStore {

  public PaperEventSettingsStore(JavaPlugin plugin) {
    super(plugin);
  }

  public PaperEventSettingsStore(Path dataDirectory) {
    super(dataDirectory);
  }
}
