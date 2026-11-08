package io.github.kizio806.spectraevents.platform.bukkit.lifecycle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    if (org.bukkit.Bukkit.getServer() == null) {
      assertTrue(reconciler.scanLoadedEntities().isEmpty());
    }
    assertDoesNotThrow(() -> reconciler.removeEntity(UUID.randomUUID()));
    assertDoesNotThrow(() -> reconciler.removeEntity("invalid-type"));
    assertDoesNotThrow(() -> reconciler.removeEntity(null));
  }

  @Test
  void testScanLoadedEntitiesAndRemoval() {
    io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup.ensureInitialized();
    Plugin dummyPlugin = createDummyPlugin();
    TestReconciler reconciler = new TestReconciler(dummyPlugin);

    org.bukkit.World world = Objects.requireNonNull(org.bukkit.Bukkit.getWorld("world"));
    org.bukkit.Location loc = new org.bukkit.Location(world, 0, 64, 0);

    // 1. Entity without PDC
    world.spawnEntity(loc, org.bukkit.entity.EntityType.ZOMBIE);

    // 2. Entity with valid PDC and explicit role
    org.bukkit.entity.Entity e2 = world.spawnEntity(loc, org.bukkit.entity.EntityType.ARMOR_STAND);
    UUID instId1 = UUID.randomUUID();
    e2.getPersistentDataContainer()
        .set(
            io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys
                .EVENT_INSTANCE_ID,
            org.bukkit.persistence.PersistentDataType.STRING,
            instId1.toString());
    e2.getPersistentDataContainer()
        .set(
            io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys.MODEL_PART_ID,
            org.bukkit.persistence.PersistentDataType.STRING,
            "part_head");
    e2.getPersistentDataContainer()
        .set(
            io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys.RESOURCE_ROLE,
            org.bukkit.persistence.PersistentDataType.STRING,
            "custom_role");

    // 3. Entity with valid PDC and null role (should fall back to entity type name)
    org.bukkit.entity.Entity e3 = world.spawnEntity(loc, org.bukkit.entity.EntityType.SKELETON);
    UUID instId2 = UUID.randomUUID();
    e3.getPersistentDataContainer()
        .set(
            io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys
                .EVENT_INSTANCE_ID,
            org.bukkit.persistence.PersistentDataType.STRING,
            instId2.toString());

    // 4. Entity with corrupt non-UUID PDC tag
    org.bukkit.entity.Entity e4 = world.spawnEntity(loc, org.bukkit.entity.EntityType.CREEPER);
    e4.getPersistentDataContainer()
        .set(
            io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys
                .EVENT_INSTANCE_ID,
            org.bukkit.persistence.PersistentDataType.STRING,
            "not-a-valid-uuid");

    List<
            io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort
                .DiscoveredEntity>
        discovered = reconciler.scanLoadedEntities();

    assertNotNull(discovered);
    assertFalse(discovered.isEmpty());
    assertEquals(1, reconciler.corruptedEntities.size());

    // Remove valid entity
    reconciler.removeEntity(e2.getUniqueId());
    assertEquals(1, reconciler.removedEntities.size());

    // Remove non-existent entity UUID
    reconciler.removeEntity(UUID.randomUUID());
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
