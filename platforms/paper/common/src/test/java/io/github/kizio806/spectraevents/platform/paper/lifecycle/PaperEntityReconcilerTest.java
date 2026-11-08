package io.github.kizio806.spectraevents.platform.paper.lifecycle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort.DiscoveredEntity;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperEntityReconcilerTest {

  private Plugin dummyPlugin;
  private PaperResourceCleaner cleaner;
  private PaperEntityReconciler reconciler;

  private static final class NoopScheduler implements EventTaskScheduler {
    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

    @Override
    public void scheduleGlobal(Duration delay, Runnable task) {}

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}

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
    PaperModelRenderer renderer = new PaperModelRenderer(dummyPlugin);
    cleaner = new PaperResourceCleaner(renderer, new NoopScheduler());
    reconciler = new PaperEntityReconciler(dummyPlugin, cleaner);
  }

  @Test
  void rejectsNullConstructorArguments() {
    assertThrows(NullPointerException.class, () -> new PaperEntityReconciler(null, cleaner));
    assertThrows(NullPointerException.class, () -> new PaperEntityReconciler(dummyPlugin, null));
  }

  @Test
  void handlesInvalidPlatformReferenceOnRemove() {
    assertDoesNotThrow(() -> reconciler.removeEntity("not-a-uuid"));
    assertDoesNotThrow(() -> reconciler.removeEntity(null));
  }

  @Test
  void restoreInstanceRegistersCleanupWithCleaner() {
    EventInstanceId instanceId = new EventInstanceId(UUID.randomUUID());
    UUID entityUuid = UUID.randomUUID();
    DiscoveredEntity discovered =
        new DiscoveredEntity(entityUuid, instanceId, "ITEM_DISPLAY", "part-1");

    reconciler.restoreInstance(instanceId, List.of(discovered));

    assertEquals(1, cleaner.resourceCount(instanceId));
  }

  @Test
  void removeEntitySafelyHandlesNullAndValidEntity() {
    assertDoesNotThrow(() -> reconciler.removeEntitySafely(null));

    org.bukkit.entity.Display entity =
        (org.bukkit.entity.Display)
            Proxy.newProxyInstance(
                org.bukkit.entity.Display.class.getClassLoader(),
                new Class<?>[] {org.bukkit.entity.Display.class},
                (proxy, method, args) -> {
                  if ("isValid".equals(method.getName())) return true;
                  if ("getScheduler".equals(method.getName())) {
                    return Proxy.newProxyInstance(
                        io.papermc.paper.threadedregions.scheduler.EntityScheduler.class
                            .getClassLoader(),
                        new Class<?>[] {
                          io.papermc.paper.threadedregions.scheduler.EntityScheduler.class
                        },
                        (sp, sm, sa) -> {
                          if ("execute".equals(sm.getName())
                              && sa != null
                              && sa.length > 1
                              && sa[1] instanceof Runnable r) {
                            r.run();
                            return true;
                          }
                          return null;
                        });
                  }
                  if ("remove".equals(method.getName())) return null;
                  return null;
                });

    assertDoesNotThrow(() -> reconciler.removeEntitySafely(entity));
  }
}
