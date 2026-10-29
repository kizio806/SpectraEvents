package io.github.kizio806.spectraevents.platform.paper.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Proxy;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class SpectraPdcKeysTest {

  @Test
  void generatesNamespacedKeysUsingPlugin() {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("namespace".equals(method.getName()) || "getName".equals(method.getName())) {
                    return "spectraevents";
                  }
                  return null;
                });

    NamespacedKey instanceKey = SpectraPdcKeys.instanceId(dummyPlugin);
    assertNotNull(instanceKey);
    assertEquals("instance_id", instanceKey.getKey());

    NamespacedKey partKey = SpectraPdcKeys.partId(dummyPlugin);
    assertNotNull(partKey);
    assertEquals("model_part_id", partKey.getKey());

    NamespacedKey roleKey = SpectraPdcKeys.entityRole(dummyPlugin);
    assertNotNull(roleKey);
    assertEquals("resource_role", roleKey.getKey());
  }
}
