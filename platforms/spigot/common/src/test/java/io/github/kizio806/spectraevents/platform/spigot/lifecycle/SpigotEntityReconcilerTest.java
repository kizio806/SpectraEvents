package io.github.kizio806.spectraevents.platform.spigot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort.DiscoveredEntity;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotEntityReconcilerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path temporaryDirectory;

  @Test
  void validatesConstructorArgumentsAndExposesPlugin() throws Exception {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("equals".equals(method.getName())) {
                    return args != null
                        && args.length > 0
                        && System.identityHashCode(proxy) == System.identityHashCode(args[0]);
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

    try {
      assertThrows(NullPointerException.class, () -> new SpigotEntityReconciler(null, adapter));
      assertThrows(NullPointerException.class, () -> new SpigotEntityReconciler(dummyPlugin, null));
    } finally {
      dummyAudiences.close();
    }

    SpigotEntityReconciler reconciler = new SpigotEntityReconciler(dummyPlugin, adapter);
    assertNotNull(reconciler.getPlugin());
    assertSame(dummyPlugin, reconciler.getPlugin());
  }

  @Test
  void testRemoveEntitySafely() throws Exception {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);

    try (BukkitAudiences dummyAudiences =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null)) {
      LocaleCatalog catalog = new FileSystemLocaleLoader(temporaryDirectory).loadCatalog("en-US");
      SpigotActionAdapter adapter = new SpigotActionAdapter(dummyPlugin, dummyAudiences, catalog);

      SpigotEntityReconciler reconciler = new SpigotEntityReconciler(dummyPlugin, adapter);

      // 1. null entity
      reconciler.removeEntitySafely(null);

      // 2. invalid entity
      AtomicBoolean removed = new AtomicBoolean(false);
      Entity invalidEntity =
          (Entity)
              Proxy.newProxyInstance(
                  Entity.class.getClassLoader(),
                  new Class<?>[] {Entity.class},
                  (proxy, method, args) -> {
                    if ("isValid".equals(method.getName())) {
                      return false;
                    }
                    if ("remove".equals(method.getName())) {
                      removed.set(true);
                      return null;
                    }
                    return null;
                  });
      reconciler.removeEntitySafely(invalidEntity);
      assertFalse(removed.get());

      // 3. valid entity
      Entity validEntity =
          (Entity)
              Proxy.newProxyInstance(
                  Entity.class.getClassLoader(),
                  new Class<?>[] {Entity.class},
                  (proxy, method, args) -> {
                    if ("isValid".equals(method.getName())) {
                      return true;
                    }
                    if ("remove".equals(method.getName())) {
                      removed.set(true);
                      return null;
                    }
                    return null;
                  });
      reconciler.removeEntitySafely(validEntity);
      assertTrue(removed.get());
    }
  }

  @Test
  void testRestoreInstance() throws Exception {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);

    try (BukkitAudiences dummyAudiences =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null)) {
      LocaleCatalog catalog = new FileSystemLocaleLoader(temporaryDirectory).loadCatalog("en-US");
      SpigotActionAdapter adapter = new SpigotActionAdapter(dummyPlugin, dummyAudiences, catalog);

      SpigotEntityReconciler reconciler = new SpigotEntityReconciler(dummyPlugin, adapter);

      EventInstanceId instanceId = EventInstanceId.generate();
      UUID entityUuid = UUID.randomUUID();

      DiscoveredEntity validDiscovered =
          new DiscoveredEntity(entityUuid, instanceId, "marker", "main");
      DiscoveredEntity nonUuidDiscovered =
          new DiscoveredEntity("non-uuid-reference", instanceId, "marker", "main");

      reconciler.restoreInstance(instanceId, List.of(validDiscovered, nonUuidDiscovered));

      // Cleanup the event in adapter
      adapter.cleanupEvent(instanceId);
    }
  }
}
