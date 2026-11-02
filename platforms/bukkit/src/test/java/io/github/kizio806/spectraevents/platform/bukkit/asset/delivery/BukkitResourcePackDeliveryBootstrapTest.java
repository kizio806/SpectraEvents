package io.github.kizio806.spectraevents.platform.bukkit.asset.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BukkitResourcePackDeliveryBootstrapTest {

  @TempDir Path tempDir;

  @Test
  void readsDefaultSettingsWhenFileDoesNotExist() throws IOException {
    ResourcePackDeliverySettings settings =
        BukkitResourcePackDeliveryBootstrap.readSettings(tempDir);

    assertNotNull(settings);
    assertFalse(settings.enabled());
    assertFalse(settings.required());
    assertTrue(Files.exists(tempDir.resolve(BukkitResourcePackDeliveryBootstrap.CONFIG_FILE)));
  }

  @Test
  void parsesMinecraftVersionFromBukkitVersion() {
    Server dummyServer =
        (Server)
            Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> {
                  if ("getBukkitVersion".equals(method.getName())) {
                    return "1.21.4-R0.1-SNAPSHOT";
                  }
                  return null;
                });

    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getServer".equals(method.getName())) {
                    return dummyServer;
                  }
                  return null;
                });

    assertEquals("1.21.4", BukkitResourcePackDeliveryBootstrap.minecraftVersion(dummyPlugin));
  }

  @Test
  void parsesMinecraftVersionWithoutSeparator() {
    Server dummyServer =
        (Server)
            Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> {
                  if ("getBukkitVersion".equals(method.getName())) {
                    return "1.21.4";
                  }
                  return null;
                });

    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getServer".equals(method.getName())) {
                    return dummyServer;
                  }
                  return null;
                });

    assertEquals("1.21.4", BukkitResourcePackDeliveryBootstrap.minecraftVersion(dummyPlugin));
  }

  @Test
  void testIsEnabled() throws IOException {
    Plugin dummyPlugin = createMockPlugin(tempDir, "1.21.4");
    assertFalse(BukkitResourcePackDeliveryBootstrap.isEnabled(dummyPlugin));

    Path configFile = tempDir.resolve(BukkitResourcePackDeliveryBootstrap.CONFIG_FILE);
    Files.writeString(
        configFile,
        """
        enabled: true
        modrinthProjectId: test-project
        """);
    assertTrue(BukkitResourcePackDeliveryBootstrap.isEnabled(dummyPlugin));
  }

  @Test
  void testConfigureWhenDisabled() throws IOException {
    Plugin dummyPlugin = createMockPlugin(tempDir, "1.21.4");
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");

    CompletableFuture<Void> future =
        BukkitResourcePackDeliveryBootstrap.configure(
            dummyPlugin,
            "1.0.0",
            AssetTargetProfile.PROFILE_26_1,
            locales,
            (service, settings) -> new Listener() {});

    assertNotNull(future);
    assertNull(future.join());
  }

  @Test
  void rejectsNullArgumentsInConfigure() {
    assertThrows(
        NullPointerException.class,
        () -> BukkitResourcePackDeliveryBootstrap.configure(null, "1.0.0", null, null, null));
  }

  private static Plugin createMockPlugin(Path dataDir, String bukkitVersion) {
    Server dummyServer =
        (Server)
            Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> {
                  if ("getBukkitVersion".equals(method.getName())) {
                    return bukkitVersion;
                  }
                  if ("getPluginManager".equals(method.getName())) {
                    return Proxy.newProxyInstance(
                        PluginManager.class.getClassLoader(),
                        new Class<?>[] {PluginManager.class},
                        (p, m, a) -> null);
                  }
                  return null;
                });

    Logger logger = Logger.getLogger("TestPluginLogger");

    return (Plugin)
        Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[] {Plugin.class},
            (proxy, method, args) -> {
              if ("getDataFolder".equals(method.getName())) {
                return dataDir.toFile();
              }
              if ("getServer".equals(method.getName())) {
                return dummyServer;
              }
              if ("getLogger".equals(method.getName())) {
                return logger;
              }
              return null;
            });
  }
}
