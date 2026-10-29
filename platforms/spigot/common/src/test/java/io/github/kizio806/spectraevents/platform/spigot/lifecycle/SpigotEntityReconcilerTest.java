package io.github.kizio806.spectraevents.platform.spigot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotEntityReconcilerTest {

  @TempDir Path temporaryDirectory;

  @Test
  @SuppressWarnings("PMD.CompareObjectsWithEquals")
  void validatesConstructorArgumentsAndExposesPlugin() throws Exception {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                  }
                  if ("hashCode".equals(method.getName())) {
                    return 42;
                  }
                  if ("getName".equals(method.getName())) {
                    return "TestPlugin";
                  }
                  return null;
                });

    BukkitAudiences dummyAudiences =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null);

    LocaleCatalog catalog = new FileSystemLocaleLoader(temporaryDirectory).loadCatalog("en-US");
    SpigotActionAdapter adapter = new SpigotActionAdapter(dummyPlugin, dummyAudiences, catalog);

    assertThrows(NullPointerException.class, () -> new SpigotEntityReconciler(null, adapter));
    assertThrows(NullPointerException.class, () -> new SpigotEntityReconciler(dummyPlugin, null));

    SpigotEntityReconciler reconciler = new SpigotEntityReconciler(dummyPlugin, adapter);
    assertNotNull(reconciler.getPlugin());
    assertSame(dummyPlugin, reconciler.getPlugin());
  }
}
