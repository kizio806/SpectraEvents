package io.github.kizio806.spectraevents.platform.spigot.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SpigotModelRendererTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @Test
  void shouldTeleportEntitiesOnTransform() {
    Plugin dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);

    SpigotModelRenderer renderer = new SpigotModelRenderer(dummyPlugin);
    assertNotNull(renderer);

    World world = org.bukkit.Bukkit.getWorld("world");
    Location originalLoc = new Location(world, 10.0, 64.0, 10.0);
    Location targetLoc = new Location(world, 20.0, 70.0, 30.0);

    List<Location> teleportedLocations = new ArrayList<>();
    Entity entity1 =
        (Entity)
            Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[] {Entity.class},
                (proxy, method, args) -> {
                  if ("teleport".equals(method.getName()) && args != null && args.length > 0) {
                    teleportedLocations.add((Location) args[0]);
                    return true;
                  }
                  if ("getLocation".equals(method.getName())) {
                    return originalLoc;
                  }
                  return null;
                });

    renderer.applyTransformTeleports(null, List.of(entity1), targetLoc);
    assertEquals(1, teleportedLocations.size());
    assertEquals(targetLoc, teleportedLocations.getFirst());
  }
}
