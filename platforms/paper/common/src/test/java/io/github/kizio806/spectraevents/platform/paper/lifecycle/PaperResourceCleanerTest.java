package io.github.kizio806.spectraevents.platform.paper.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperResourceCleanerTest {

  private Plugin dummyPlugin;
  private PaperModelRenderer renderer;
  private TrackingTaskScheduler scheduler;
  private PaperResourceCleaner cleaner;

  private static final class TrackingTaskScheduler implements EventTaskScheduler {
    final AtomicInteger cancelAllCalls = new AtomicInteger();
    final AtomicInteger cancelInstanceCalls = new AtomicInteger();

    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

    @Override
    public void scheduleGlobal(Duration delay, Runnable task) {}

    @Override
    public void cancelAll(EventInstanceId eventId) {
      cancelInstanceCalls.incrementAndGet();
    }

    @Override
    public void cancelAll() {
      cancelAllCalls.incrementAndGet();
    }

    @Override
    public int pendingTaskCount(EventInstanceId eventId) {
      return 0;
    }
  }

  @BeforeEach
  void setUp() {
    dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);
    renderer = new PaperModelRenderer(dummyPlugin);
    scheduler = new TrackingTaskScheduler();
    cleaner = new PaperResourceCleaner(renderer, scheduler);
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(NullPointerException.class, () -> new PaperResourceCleaner(null, scheduler));
    assertThrows(NullPointerException.class, () -> new PaperResourceCleaner(renderer, null));
  }

  @Test
  void tracksAndExecutesCustomCleanupOnInstanceCleanup() {
    EventInstanceId instanceId = new EventInstanceId(UUID.randomUUID());
    AtomicBoolean executed = new AtomicBoolean(false);

    cleaner.registerCustomCleanup(instanceId, () -> executed.set(true));
    assertEquals(1, cleaner.resourceCount(instanceId));

    cleaner.cleanup(instanceId);

    assertTrue(executed.get());
    assertEquals(0, cleaner.resourceCount(instanceId));
    assertEquals(1, scheduler.cancelInstanceCalls.get());
  }

  @Test
  void tracksAndExecutesCustomCleanupsOnCleanupAll() {
    EventInstanceId id1 = new EventInstanceId(UUID.randomUUID());
    EventInstanceId id2 = new EventInstanceId(UUID.randomUUID());
    AtomicInteger count = new AtomicInteger(0);

    cleaner.registerCustomCleanup(id1, count::incrementAndGet);
    cleaner.registerCustomCleanup(id2, count::incrementAndGet);
    assertEquals(1, cleaner.resourceCount(id1));
    assertEquals(1, cleaner.resourceCount(id2));

    cleaner.cleanupAll();

    assertEquals(2, count.get());
    assertEquals(0, cleaner.resourceCount(id1));
    assertEquals(0, cleaner.resourceCount(id2));
    assertEquals(1, scheduler.cancelAllCalls.get());
  }
}
