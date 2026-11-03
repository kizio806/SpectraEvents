package io.github.kizio806.spectraevents.platform.bukkit.lifecycle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class AbstractBukkitEntityReconcilerTest {

  private static final class TestReconciler extends AbstractBukkitEntityReconciler {
    final List<Entity> removedEntities = new ArrayList<>();
    final List<Entity> corruptedEntities = new ArrayList<>();

    TestReconciler(Plugin plugin) {
      super(plugin);
    }

    @Override
    public void restoreInstance(EventInstanceId instanceId, List<DiscoveredEntity> entities) {}

    @Override
    public void removeEntitySafely(Entity entity) {
      removedEntities.add(entity);
    }

    @Override
    protected void onCorruptedPdcEntity(Entity entity) {
      corruptedEntities.add(entity);
    }
  }

  @Test
  void rejectsNullPlugin() {
    assertThrows(NullPointerException.class, () -> new TestReconciler(null));
  }

  @Test
  void exposesPlugin() {
    Plugin dummyPlugin = createDummyPlugin();
    TestReconciler reconciler = new TestReconciler(dummyPlugin);

    assertSame(dummyPlugin, reconciler.plugin());
  }

  @Test
  void handlesMissingBukkitServerGracefully() {
    Plugin dummyPlugin = createDummyPlugin();
    TestReconciler reconciler = new TestReconciler(dummyPlugin);

    assertTrue(reconciler.scanLoadedEntities().isEmpty());
    assertDoesNotThrow(() -> reconciler.removeEntity(UUID.randomUUID()));
    assertDoesNotThrow(() -> reconciler.removeEntity("invalid-type"));
    assertDoesNotThrow(() -> reconciler.removeEntity(null));
  }

  @Test
  void ignoresNonUuidPlatformEntityReference() {
    Plugin dummyPlugin = createDummyPlugin();
    TestReconciler reconciler = new TestReconciler(dummyPlugin);

    assertDoesNotThrow(() -> reconciler.removeEntity(12345));
    assertEquals(0, reconciler.removedEntities.size());
  }

  private Plugin createDummyPlugin() {
    return (Plugin)
        Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[] {Plugin.class},
            (proxy, method, args) -> null);
  }
}
