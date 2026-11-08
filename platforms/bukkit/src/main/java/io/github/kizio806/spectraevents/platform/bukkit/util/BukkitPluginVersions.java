package io.github.kizio806.spectraevents.platform.bukkit.util;

import java.lang.reflect.Method;
import org.bukkit.plugin.Plugin;

/**
 * Utility for resolving plugin versions cleanly across Paper (with PluginMeta) and Spigot (with
 * PluginDescriptionFile).
 */
public final class BukkitPluginVersions {

  private BukkitPluginVersions() {}

  public static String resolveVersion(Plugin plugin) {
    if (plugin == null) {
      return "unknown";
    }
    String implVersion = plugin.getClass().getPackage().getImplementationVersion();
    if (implVersion != null && !implVersion.isBlank()) {
      return implVersion;
    }
    try {
      Method metaMethod = plugin.getClass().getMethod("getPluginMeta");
      Object meta = metaMethod.invoke(plugin);
      if (meta != null) {
        Method versionMethod = meta.getClass().getMethod("getVersion");
        Object version = versionMethod.invoke(meta);
        if (version != null) {
          return version.toString();
        }
      }
    } catch (ReflectiveOperationException ignored) {
      // Spigot does not declare getPluginMeta(); fallback to getDescription()
    }
    try {
      Method descMethod = plugin.getClass().getMethod("getDescription");
      Object desc = descMethod.invoke(plugin);
      if (desc != null) {
        Method versionMethod = desc.getClass().getMethod("getVersion");
        Object version = versionMethod.invoke(desc);
        if (version != null) {
          return version.toString();
        }
      }
    } catch (ReflectiveOperationException ignored) {
      // Fallback exhausted
    }
    return "unknown";
  }
}
