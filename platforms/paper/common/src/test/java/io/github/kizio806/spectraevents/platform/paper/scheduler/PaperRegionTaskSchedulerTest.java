package io.github.kizio806.spectraevents.platform.paper.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PaperRegionTaskSchedulerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @Test
  void testNullPluginThrows() {
    assertThrows(NullPointerException.class, () -> new PaperRegionTaskScheduler(null));
  }

  @Test
  void testExecuteGlobalExecutesTask() {
    Plugin mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (p, m, a) -> null);

    PaperRegionTaskScheduler scheduler = new PaperRegionTaskScheduler(mockPlugin);
    AtomicBoolean executed = new AtomicBoolean(false);

    assertDoesNotThrow(() -> scheduler.executeGlobal(() -> executed.set(true)));
    assertTrue(executed.get());
  }

  @Test
  void testExecuteAtExecutesTask() {
    Plugin mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (p, m, a) -> null);

    PaperRegionTaskScheduler scheduler = new PaperRegionTaskScheduler(mockPlugin);
    AtomicBoolean executed = new AtomicBoolean(false);
    World world = MockBukkitSetup.createMockWorld("world");
    Location loc = new Location(world, 10, 64, 10);

    assertDoesNotThrow(() -> scheduler.executeAt(loc, () -> executed.set(true)));
    assertTrue(executed.get());
  }

  @Test
  void testExecuteForEntityExecutesTask() {
    Plugin mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (p, m, a) -> null);

    PaperRegionTaskScheduler scheduler = new PaperRegionTaskScheduler(mockPlugin);
    AtomicBoolean executed = new AtomicBoolean(false);
    World world = MockBukkitSetup.createMockWorld("world");
    Location loc = new Location(world, 10, 64, 10);
    Entity entity = MockBukkitSetup.createMockEntity(Display.class, loc);

    assertDoesNotThrow(() -> scheduler.executeFor(entity, () -> executed.set(true)));
    assertTrue(executed.get());
  }
}
