package io.github.kizio806.spectraevents.platform.paper.lifecycle;

import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Explicitly owns the cleanup of Paper platform resources for an event instance. */
public final class PaperResourceCleaner {
  private final PaperModelRenderer renderer;
  private final EventTaskScheduler scheduler;
  private final Map<EventInstanceId, List<Runnable>> customCleanups = new ConcurrentHashMap<>();

  public PaperResourceCleaner(PaperModelRenderer renderer, EventTaskScheduler scheduler) {
    this.renderer = renderer;
    this.scheduler = scheduler;
  }

  /** Registers a custom cleanup action for a specific instance. */
  public void registerCustomCleanup(EventInstanceId instanceId, Runnable cleanupAction) {
    customCleanups
        .computeIfAbsent(instanceId, ignored -> new CopyOnWriteArrayList<>())
        .add(cleanupAction);
  }

  /** Cleans up all platform resources and custom states associated with the instance. */
  public void cleanup(EventInstanceId instanceId) {
    renderer.cleanupInstance(instanceId);
    scheduler.cancelAll(instanceId);

    List<Runnable> cleanups = customCleanups.remove(instanceId);
    if (cleanups != null) {
      cleanups.forEach(Runnable::run);
    }
  }

  /** Cleans up all tracked active events. Safe for plugin disable. */
  public void cleanupAll() {
    scheduler.cancelAll();
    renderer.removeAll();
    customCleanups.values().forEach(cleanups -> cleanups.forEach(Runnable::run));
    customCleanups.clear();
  }

  public int resourceCount(EventInstanceId instanceId) {
    return renderer.resourceCount(instanceId)
        + customCleanups.getOrDefault(instanceId, List.of()).size();
  }
}
