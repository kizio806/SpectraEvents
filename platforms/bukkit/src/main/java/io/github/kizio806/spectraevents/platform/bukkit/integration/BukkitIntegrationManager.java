package io.github.kizio806.spectraevents.platform.bukkit.integration;

import io.github.kizio806.spectraevents.application.integration.IntegrationInitializationContext;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import io.github.kizio806.spectraevents.application.integration.PlatformIntegrationModule;
import io.github.kizio806.spectraevents.platform.bukkit.util.BukkitPluginVersions;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Detects and manages optional Bukkit and Paper plugin integrations safely. */
public final class BukkitIntegrationManager {
  private static final Logger LOGGER = Logger.getLogger(BukkitIntegrationManager.class.getName());
  private static final Set<String> REQUIRES_MODULE_INIT =
      Set.of("LuckPerms", "WorldGuard", "Vault", "PlaceholderAPI");

  private final IntegrationRegistry registry;

  public BukkitIntegrationManager(IntegrationRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  public void detectAll() {
    detectPlugin("LuckPerms", "Condition resolution for luckperms_group");
    detectPlugin("WorldGuard", "Condition resolution for worldguard_region");
    detectPlugin("Vault", "Action resolution for give_money");
    detectPlugin("PlaceholderAPI", "Spectra placeholder expansion");
    detectPlugin("MiniPlaceholders", "Global audience parser support");
    detectPlugin("Nexo", "Custom item provider for give_item");
    detectPlugin("Oraxen", "Custom item provider for give_item");
    detectPlugin("ItemsAdder", "Custom item provider for give_item");
  }

  private void detectPlugin(String name, String description) {
    if (Bukkit.getServer() == null || Bukkit.getPluginManager() == null) {
      registry.register(name, IntegrationState.ABSENT, description + " (Server unavailable)");
      return;
    }
    Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
    if (plugin != null && plugin.isEnabled()) {
      IntegrationState state =
          REQUIRES_MODULE_INIT.contains(name)
              ? IntegrationState.AVAILABLE
              : IntegrationState.ACTIVE;
      String version = BukkitPluginVersions.resolveVersion(plugin);
      registry.register(name, state, description + " (v" + version + ")");
      LOGGER.fine("Integration detected (" + state + "): " + name);
    } else if (plugin != null) {
      registry.register(
          name, IntegrationState.DEGRADED, description + " (Plugin present but disabled)");
    } else {
      registry.register(name, IntegrationState.ABSENT, description + " (Not installed)");
    }
  }

  public void initializeModules(Plugin plugin, IntegrationInitializationContext context) {
    Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(context, "context");

    if (Bukkit.getServer() == null || Bukkit.getPluginManager() == null) {
      return;
    }

    for (PlatformIntegrationModule module :
        ServiceLoader.load(PlatformIntegrationModule.class, plugin.getClass().getClassLoader())) {
      Plugin requiredPlugin = Bukkit.getPluginManager().getPlugin(module.requiredPluginName());
      if (requiredPlugin != null && requiredPlugin.isEnabled()) {
        String pluginName = module.requiredPluginName();
        try {
          module.initialize(context);
          String existingDetails =
              registry
                  .get(pluginName)
                  .map(IntegrationRegistry.IntegrationInfo::details)
                  .orElse("Active");
          registry.register(pluginName, IntegrationState.ACTIVE, existingDetails);
        } catch (Exception e) {
          String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
          registry.register(
              pluginName, IntegrationState.FAILED, "Initialization failed: " + reason);
          plugin
              .getLogger()
              .warning("Failed to initialize integration " + pluginName + ": " + reason);
        }
      }
    }
  }

  public IntegrationRegistry registry() {
    return registry;
  }
}
