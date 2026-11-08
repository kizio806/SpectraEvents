package io.github.kizio806.spectraevents.platform.bukkit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

class BukkitPluginVersionsTest {

  @Test
  void returnsUnknownForNullPlugin() {
    assertEquals("unknown", BukkitPluginVersions.resolveVersion(null));
  }

  @Test
  void resolvesVersionFromPluginDescriptionFileOnSpigot() {
    PluginDescriptionFile description =
        new PluginDescriptionFile("TestPlugin", "1.2.3", "io.github.kizio806.spectraevents.Main");

    Plugin plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getDescription".equals(method.getName())) {
                    return description;
                  }
                  return null;
                });

    assertEquals("1.2.3", BukkitPluginVersions.resolveVersion(plugin));
  }

  interface HasPluginMeta {
    Object getPluginMeta();
  }

  interface MetaWithVersion {
    String getVersion();
  }

  @Test
  void resolvesVersionFromPluginMetaOnPaper() {
    MetaWithVersion meta = () -> "2.3.4";
    Plugin plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class, HasPluginMeta.class},
                (proxy, method, args) -> {
                  if ("getPluginMeta".equals(method.getName())) {
                    return meta;
                  }
                  return null;
                });

    assertEquals("2.3.4", BukkitPluginVersions.resolveVersion(plugin));

    // When meta returns null
    Plugin nullMetaPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class, HasPluginMeta.class},
                (proxy, method, args) -> null);
    assertEquals("unknown", BukkitPluginVersions.resolveVersion(nullMetaPlugin));

    // When meta version is null
    MetaWithVersion nullVerMeta = () -> null;
    Plugin nullVerPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class, HasPluginMeta.class},
                (proxy, method, args) -> {
                  if ("getPluginMeta".equals(method.getName())) {
                    return nullVerMeta;
                  }
                  return null;
                });
    assertEquals("unknown", BukkitPluginVersions.resolveVersion(nullVerPlugin));
  }

  @Test
  void returnsUnknownWhenVersionResolutionFails() {
    Plugin plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);

    assertEquals("unknown", BukkitPluginVersions.resolveVersion(plugin));
  }
}
