package io.github.kizio806.spectraevents.platform.bukkit.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Proxy;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class BukkitPdcKeysTest {

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

    NamespacedKey instanceKey = BukkitPdcKeys.instanceId(dummyPlugin);
    assertNotNull(instanceKey);
    assertEquals("instance_id", instanceKey.getKey());

    NamespacedKey partKey = BukkitPdcKeys.partId(dummyPlugin);
    assertNotNull(partKey);
    assertEquals("model_part_id", partKey.getKey());

    NamespacedKey roleKey = BukkitPdcKeys.entityRole(dummyPlugin);
    assertNotNull(roleKey);
    assertEquals("resource_role", roleKey.getKey());
  }

  @Test
  void staticKeysHaveCorrectNamespaceAndKey() {
    assertEquals("spectraevents:model_instance_id", BukkitPdcKeys.MODEL_INSTANCE_ID.toString());
    assertEquals("spectraevents:model_definition_id", BukkitPdcKeys.MODEL_DEFINITION_ID.toString());
    assertEquals("spectraevents:model_part_id", BukkitPdcKeys.MODEL_PART_ID.toString());
    assertEquals("spectraevents:instance_id", BukkitPdcKeys.EVENT_INSTANCE_ID.toString());
    assertEquals("spectraevents:resource_role", BukkitPdcKeys.RESOURCE_ROLE.toString());
    assertEquals("spectraevents:wave_id", BukkitPdcKeys.WAVE_ID.toString());
    assertEquals("spectraevents:ground_loot_slot", BukkitPdcKeys.GROUND_LOOT_SLOT.toString());
  }
}
