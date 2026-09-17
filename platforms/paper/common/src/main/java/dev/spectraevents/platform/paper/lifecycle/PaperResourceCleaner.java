package dev.spectraevents.platform.paper.lifecycle;

import dev.spectraevents.application.port.EventTaskScheduler;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.platform.paper.render.PaperModelRenderer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Explicitly owns the cleanup of Paper platform resources for an event instance. */
public final class PaperResourceCleaner {
  private final PaperModelRenderer renderer;
  private final EventTaskScheduler scheduler;
  private final Map<EventInstanceId, Runnable> customCleanups = new ConcurrentHashMap<>();

  public PaperResourceCleaner(PaperModelRenderer renderer, EventTaskScheduler scheduler) {
    this.renderer = renderer;
    this.scheduler = scheduler;
  }

  /** Registers a custom cleanup action for a specific instance. */
  public void registerCustomCleanup(EventInstanceId instanceId, Runnable cleanupAction) {
    customCleanups.put(instanceId, cleanupAction);
  }

  /** Cleans up all platform resources and custom states associated with the instance. */
  public void cleanup(EventInstanceId instanceId) {
    renderer.remove(instanceId);
    scheduler.cancelAll(instanceId);

    Runnable customCleanup = customCleanups.remove(instanceId);
    if (customCleanup != null) {
      customCleanup.run();
    }
  }

  /** Cleans up all tracked active events. Safe for plugin disable. */
  public void cleanupAll() {
    scheduler.cancelAll();
    renderer.removeAll();
    for (Runnable customCleanup : customCleanups.values()) {
      customCleanup.run();
    }
    customCleanups.clear();
  }
}
