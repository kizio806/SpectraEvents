package dev.spectraevents.platform.paper.integration;

import dev.spectraevents.application.integration.IntegrationRegistry;
import dev.spectraevents.application.integration.IntegrationState;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Detects and manages optional paper plugin integrations safely. */
public final class PaperIntegrationManager {
  private static final Logger LOGGER = Logger.getLogger(PaperIntegrationManager.class.getName());

  private final IntegrationRegistry registry;

  public PaperIntegrationManager(IntegrationRegistry registry) {
    this.registry = registry;
  }

  public void detectAll() {
    // No optional integrations are implemented yet in Beta.
  }

  private void detectPlugin(String name, String description) {
    Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
    if (plugin != null && plugin.isEnabled()) {
      registry.register(
          name,
          IntegrationState.ENABLED,
          description + " (v" + plugin.getPluginMeta().getVersion() + ")");
      LOGGER.info("Integration detected and enabled: " + name);
    } else if (plugin != null) {
      registry.register(
          name, IntegrationState.DISABLED, description + " (Plugin present but disabled)");
    } else {
      registry.register(name, IntegrationState.MISSING, description + " (Not installed)");
    }
  }

  public IntegrationRegistry registry() {
    return registry;
  }
}
