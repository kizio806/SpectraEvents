package io.github.kizio806.spectraevents.platform.bukkit.asset.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.delivery.ResourcePackDeliverySettings;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
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
  void rejectsNullArgumentsInConfigure() {
    assertThrows(
        NullPointerException.class,
        () -> BukkitResourcePackDeliveryBootstrap.configure(null, "1.0.0", null, null, null));
  }
}
