package io.github.kizio806.spectraevents.platform.spigot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotEventTaskSchedulerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private Plugin plugin;
  private List<Runnable> scheduledTasks;
  private List<BukkitTask> mockTasks;

  @BeforeEach
  void setUp() {
    scheduledTasks = new ArrayList<>();
    mockTasks = new ArrayList<>();

    plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);
  }

  @AfterEach
  void tearDown() {
    MockBukkitSetup.resetCustomDelegates();
  }

  private static final java.util.IdentityHashMap<Object, Boolean> IDENTITY_HOLDER =
      new java.util.IdentityHashMap<>();
  private static final Object IDENTITY_LOCK = new Object();

  private static boolean isSameInstance(Object a, Object b) {
    if (a == null || b == null) {
      return false;
    }
    synchronized (IDENTITY_LOCK) {
      IDENTITY_HOLDER.put(a, Boolean.TRUE);
      boolean same = IDENTITY_HOLDER.containsKey(b);
      IDENTITY_HOLDER.clear();
      return same;
    }
  }

  private BukkitTask createMockTask() {
    AtomicBoolean cancelled = new AtomicBoolean(false);
    BukkitTask task =
        (BukkitTask)
            Proxy.newProxyInstance(
                BukkitTask.class.getClassLoader(),
                new Class<?>[] {BukkitTask.class},
                (proxy, method, args) -> {
                  if ("cancel".equals(method.getName())) {
                    cancelled.set(true);
                    return null;
                  }
                  if ("isCancelled".equals(method.getName())) {
                    return cancelled.get();
                  }
                  if ("equals".equals(method.getName())) {
                    return isSameInstance(proxy, args != null && args.length > 0 ? args[0] : null);
                  }
                  if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                  }
                  return null;
                });
    mockTasks.add(task);
    return task;
  }

  @Test
  void shouldScheduleAndExecuteTasks() {
    BukkitScheduler customScheduler =
        (BukkitScheduler)
            Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(),
                new Class<?>[] {BukkitScheduler.class},
                (proxy, method, args) -> {
                  if ("runTaskLater".equals(method.getName()) && args != null && args.length >= 2) {
                    Runnable r = (Runnable) args[1];
                    scheduledTasks.add(r);
                    return createMockTask();
                  }
                  return null;
                });

    MockBukkitSetup.setCustomScheduler(customScheduler);

    SpigotEventTaskScheduler scheduler = new SpigotEventTaskScheduler(plugin);
    EventInstanceId id = EventInstanceId.generate();

    AtomicBoolean taskRan = new AtomicBoolean(false);
    scheduler.schedule(id, Duration.ofMillis(100), () -> taskRan.set(true));

    assertEquals(1, scheduler.pendingTaskCount(id));

    // Execute scheduled task
    assertFalse(scheduledTasks.isEmpty());
    scheduledTasks.getFirst().run();
    assertTrue(taskRan.get());

    // Task should be removed after execution
    assertEquals(0, scheduler.pendingTaskCount(id));
  }

  @Test
  void shouldScheduleGlobalTask() {
    BukkitScheduler customScheduler =
        (BukkitScheduler)
            Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(),
                new Class<?>[] {BukkitScheduler.class},
                (proxy, method, args) -> {
                  if ("runTaskLater".equals(method.getName()) && args != null && args.length >= 2) {
                    Runnable r = (Runnable) args[1];
                    scheduledTasks.add(r);
                    return createMockTask();
                  }
                  return null;
                });

    MockBukkitSetup.setCustomScheduler(customScheduler);

    SpigotEventTaskScheduler scheduler = new SpigotEventTaskScheduler(plugin);
    AtomicBoolean globalRan = new AtomicBoolean(false);
    scheduler.scheduleGlobal(Duration.ofSeconds(1), () -> globalRan.set(true));

    assertFalse(scheduledTasks.isEmpty());
    scheduledTasks.getFirst().run();
    assertTrue(globalRan.get());
  }

  @Test
  void shouldCancelAllForEventAndGlobal() {
    BukkitScheduler customScheduler =
        (BukkitScheduler)
            Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(),
                new Class<?>[] {BukkitScheduler.class},
                (proxy, method, args) -> {
                  if ("runTaskLater".equals(method.getName())) {
                    return createMockTask();
                  }
                  return null;
                });

    MockBukkitSetup.setCustomScheduler(customScheduler);

    SpigotEventTaskScheduler scheduler = new SpigotEventTaskScheduler(plugin);
    EventInstanceId id1 = EventInstanceId.generate();
    EventInstanceId id2 = EventInstanceId.generate();

    scheduler.schedule(id1, Duration.ofSeconds(5), () -> {});
    scheduler.schedule(id2, Duration.ofSeconds(5), () -> {});
    scheduler.scheduleGlobal(Duration.ofSeconds(5), () -> {});

    assertEquals(1, scheduler.pendingTaskCount(id1));
    assertEquals(1, scheduler.pendingTaskCount(id2));

    scheduler.cancelAll(id1);
    assertEquals(0, scheduler.pendingTaskCount(id1));
    assertEquals(1, scheduler.pendingTaskCount(id2));

    scheduler.cancelAll();
    assertEquals(0, scheduler.pendingTaskCount(id2));
  }
}
