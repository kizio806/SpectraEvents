package io.github.kizio806.spectraevents.platform.spigot.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Proxy;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class SpigotPdcKeysTest {

  @Test
  void generatesNamespacedKeysUsingPlugin() {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getName".equals(method.getName())) {
                    return "spectraevents";
                  }
                  return null;
                });

    NamespacedKey instanceKey = SpigotPdcKeys.instanceId(dummyPlugin);
    assertNotNull(instanceKey);
    assertEquals("instance_id", instanceKey.getKey());

    NamespacedKey partKey = SpigotPdcKeys.partId(dummyPlugin);
    assertNotNull(partKey);
    assertEquals("model_part_id", partKey.getKey());

    NamespacedKey roleKey = SpigotPdcKeys.entityRole(dummyPlugin);
    assertNotNull(roleKey);
    assertEquals("resource_role", roleKey.getKey());
  }
}
