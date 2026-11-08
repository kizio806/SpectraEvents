package io.github.kizio806.spectraevents.platform.paper.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PaperEventTaskSchedulerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @Test
  void testNullPluginThrows() {
    assertThrows(NullPointerException.class, () -> new PaperEventTaskScheduler(null));
  }

  @Test
  void testScheduleAndCancel() {
    Plugin mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (p, m, a) -> null);

    PaperEventTaskScheduler scheduler = new PaperEventTaskScheduler(mockPlugin);
    EventInstanceId eventId = EventInstanceId.generate();
    AtomicBoolean executed = new AtomicBoolean(false);

    scheduler.schedule(eventId, Duration.ofMillis(100), () -> executed.set(true));
    assertTrue(executed.get());

    scheduler.cancelAll(eventId);
    assertEquals(0, scheduler.pendingTaskCount(eventId));

    // Cancel non-existent event ID (instanceTasks == null branch)
    assertDoesNotThrow(() -> scheduler.cancelAll(EventInstanceId.generate()));
  }

  @Test
  void testScheduleGlobalAndCancelAll() {
    Plugin mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (p, m, a) -> null);

    PaperEventTaskScheduler scheduler = new PaperEventTaskScheduler(mockPlugin);
    AtomicBoolean executed = new AtomicBoolean(false);

    scheduler.scheduleGlobal(Duration.ofMillis(50), () -> executed.set(true));
    assertTrue(executed.get());

    assertDoesNotThrow(() -> scheduler.cancelAll());
  }

  @Test
  void testDelayedExecutionAndTaskCleanup() throws Exception {
    Plugin mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (p, m, a) -> null);

    List<Consumer<ScheduledTask>> capturedConsumers = new ArrayList<>();
    List<ScheduledTask> createdTasks = new ArrayList<>();

    GlobalRegionScheduler customGlobalScheduler =
        (GlobalRegionScheduler)
            Proxy.newProxyInstance(
                GlobalRegionScheduler.class.getClassLoader(),
                new Class<?>[] {GlobalRegionScheduler.class},
                (proxy, method, args) -> {
                  if ("runDelayed".equals(method.getName()) && args != null) {
                    AtomicBoolean cancelled = new AtomicBoolean(false);
                    ScheduledTask task =
                        (ScheduledTask)
                            Proxy.newProxyInstance(
                                ScheduledTask.class.getClassLoader(),
                                new Class<?>[] {ScheduledTask.class},
                                (tp, tm, ta) -> {
                                  if ("cancel".equals(tm.getName())) {
                                    cancelled.set(true);
                                    return null;
                                  }
                                  if ("isCancelled".equals(tm.getName())) {
                                    return cancelled.get();
                                  }
                                  if ("getExecutionState".equals(tm.getName())) {
                                    return ScheduledTask.ExecutionState.FINISHED;
                                  }
                                  return null;
                                });
                    for (Object arg : args) {
                      if (arg != null && Consumer.class.isAssignableFrom(arg.getClass())) {
                        java.lang.reflect.Method acceptMethod =
                            Consumer.class.getMethod("accept", Object.class);
                        capturedConsumers.add(
                            taskArg -> {
                              try {
                                acceptMethod.invoke(arg, taskArg);
                              } catch (ReflectiveOperationException e) {
                                if (e.getCause() instanceof RuntimeException re) {
                                  throw re;
                                }
                                throw new RuntimeException(e);
                              }
                            });
                      }
                    }
                    createdTasks.add(task);
                    return task;
                  }
                  return null;
                });

    Server baseServer = Bukkit.getServer();
    Server customServer =
        (Server)
            Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> {
                  if ("getGlobalRegionScheduler".equals(method.getName())) {
                    return customGlobalScheduler;
                  }
                  return method.invoke(baseServer, args);
                });

    Field serverField = Bukkit.class.getDeclaredField("server");
    serverField.setAccessible(true);
    serverField.set(null, customServer);

    try {
      PaperEventTaskScheduler scheduler = new PaperEventTaskScheduler(mockPlugin);
      EventInstanceId eventId = EventInstanceId.generate();

      // 1. Schedule event task
      AtomicBoolean eventTaskRun = new AtomicBoolean(false);
      scheduler.schedule(eventId, Duration.ofMillis(100), () -> eventTaskRun.set(true));
      assertEquals(1, scheduler.pendingTaskCount(eventId));

      // Execute captured task consumer (finished state)
      capturedConsumers.remove(0).accept(createdTasks.get(0));
      assertTrue(eventTaskRun.get());
      assertEquals(0, scheduler.pendingTaskCount(eventId));

      // 2. Schedule another task, cancel it, then run consumer (cancelled state)
      scheduler.schedule(
          eventId,
          Duration.ofMillis(100),
          () -> {
            throw new RuntimeException("Simulated task exception");
          });
      assertEquals(1, scheduler.pendingTaskCount(eventId));
      ScheduledTask task2 = createdTasks.get(1);
      task2.cancel();
      // Run consumer throwing exception
      assertThrows(RuntimeException.class, () -> capturedConsumers.remove(0).accept(task2));
      assertEquals(0, scheduler.pendingTaskCount(eventId));

      // 3. Schedule global task
      AtomicBoolean globalRun = new AtomicBoolean(false);
      scheduler.scheduleGlobal(Duration.ofMillis(100), () -> globalRun.set(true));
      capturedConsumers.remove(0).accept(createdTasks.get(2));
      assertTrue(globalRun.get());

      // 4. CancelAll with remaining tasks
      scheduler.schedule(eventId, Duration.ofMillis(100), () -> {});
      scheduler.scheduleGlobal(Duration.ofMillis(100), () -> {});
      scheduler.cancelAll();
      assertEquals(0, scheduler.pendingTaskCount(eventId));
    } finally {
      serverField.set(null, baseServer);
    }
  }
}
