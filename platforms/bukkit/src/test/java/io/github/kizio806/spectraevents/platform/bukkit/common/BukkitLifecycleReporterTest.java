package io.github.kizio806.spectraevents.platform.bukkit.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class BukkitLifecycleReporterTest {

  @Test
  void reportsLifecycleTransitions() {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("BukkitLifecycleReporterTest");
                  }
                  return null;
                });

    BukkitLifecycleReporter reporter = new BukkitLifecycleReporter(dummyPlugin);
    assertDoesNotThrow(reporter::started);
    assertDoesNotThrow(reporter::stopped);
  }

  @Test
  void rejectsNullPlugin() {
    assertThrows(NullPointerException.class, () -> new BukkitLifecycleReporter(null));
  }
}
