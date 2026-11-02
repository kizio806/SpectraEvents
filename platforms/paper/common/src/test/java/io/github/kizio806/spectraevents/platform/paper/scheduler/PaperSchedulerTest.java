package io.github.kizio806.spectraevents.platform.paper.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperSchedulerTest {

  private Plugin dummyPlugin;

  @BeforeEach
  void setUp() {
    dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> null);
  }

  @Test
  void eventTaskSchedulerValidatesArgumentsAndTracksTasks() {
    assertThrows(NullPointerException.class, () -> new PaperEventTaskScheduler(null));

    PaperEventTaskScheduler scheduler = new PaperEventTaskScheduler(dummyPlugin);
    EventInstanceId instanceId = new EventInstanceId(UUID.randomUUID());

    assertEquals(0, scheduler.pendingTaskCount(instanceId));
    assertDoesNotThrow(() -> scheduler.cancelAll(instanceId));
    assertDoesNotThrow(() -> scheduler.cancelAll());
  }

  @Test
  void regionTaskSchedulerValidatesArguments() {
    assertThrows(NullPointerException.class, () -> new PaperRegionTaskScheduler(null));

    PaperRegionTaskScheduler scheduler = new PaperRegionTaskScheduler(dummyPlugin);
    assertNotNull(scheduler);
  }
}
