package io.github.kizio806.spectraevents.platform.bukkit.integration.item;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CustomItemProvidersTest {

  @Test
  void nexoItemProviderHandlesUnavailableEnvironmentGracefully() {
    NexoItemProvider provider = new NexoItemProvider();
    assertFalse(provider.isAvailable());
    assertNull(provider.resolveItem("nexo:custom_sword", 1));
    assertNull(provider.resolveItem("custom_sword", 5));
  }

  @Test
  void oraxenItemProviderHandlesUnavailableEnvironmentGracefully() {
    OraxenItemProvider provider = new OraxenItemProvider();
    assertFalse(provider.isAvailable());
    assertNull(provider.resolveItem("oraxen:ruby_pickaxe", 1));
    assertNull(provider.resolveItem("ruby_pickaxe", 2));
  }

  @Test
  void itemsAdderItemProviderHandlesUnavailableEnvironmentGracefully() {
    ItemsAdderItemProvider provider = new ItemsAdderItemProvider();
    assertFalse(provider.isAvailable());
    assertNull(provider.resolveItem("itemsadder:custom_shield", 1));
    assertNull(provider.resolveItem("custom_shield", 1));
  }

  @Test
  void testItemProvidersWhenPluginManagerReportsPluginAvailable() {
    io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup.ensureInitialized();
    org.bukkit.plugin.PluginManager mockPm =
        (org.bukkit.plugin.PluginManager)
            java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.plugin.PluginManager.class.getClassLoader(),
                new Class<?>[] {org.bukkit.plugin.PluginManager.class},
                (proxy, method, args) -> {
                  if ("getPlugin".equals(method.getName())) {
                    String name = (String) args[0];
                    return (org.bukkit.plugin.Plugin)
                        java.lang.reflect.Proxy.newProxyInstance(
                            org.bukkit.plugin.Plugin.class.getClassLoader(),
                            new Class<?>[] {org.bukkit.plugin.Plugin.class},
                            (p, m, a) -> {
                              if ("getName".equals(m.getName())) return name;
                              if ("isEnabled".equals(m.getName())) return true;
                              return null;
                            });
                  }
                  return null;
                });

    io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
        .setCustomPluginManager(mockPm);
    try {
      NexoItemProvider nexo = new NexoItemProvider();
      org.junit.jupiter.api.Assertions.assertTrue(nexo.isAvailable());
      assertNull(nexo.resolveItem("nexo:sword", 1));
      assertNull(nexo.resolveItem("sword", 1));

      OraxenItemProvider oraxen = new OraxenItemProvider();
      org.junit.jupiter.api.Assertions.assertTrue(oraxen.isAvailable());
      assertNull(oraxen.resolveItem("oraxen:pickaxe", 1));
      assertNull(oraxen.resolveItem("pickaxe", 1));

      ItemsAdderItemProvider itemsAdder = new ItemsAdderItemProvider();
      org.junit.jupiter.api.Assertions.assertTrue(itemsAdder.isAvailable());
      assertNull(itemsAdder.resolveItem("itemsadder:shield", 1));
      assertNull(itemsAdder.resolveItem("shield", 1));
    } finally {
      io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup
          .setCustomPluginManager(null);
    }
  }
}
