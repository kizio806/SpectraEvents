package dev.spectraevents.platform.paper.scheduler;

import dev.spectraevents.application.port.EventTaskScheduler;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Paper implementation of EventTaskScheduler using Bukkit's global region scheduler. */
public final class PaperEventTaskScheduler implements EventTaskScheduler {
  private final Plugin plugin;
  private final Map<EventInstanceId, List<ScheduledTask>> tasks = new ConcurrentHashMap<>();

  public PaperEventTaskScheduler(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
    long ticks = Math.max(1L, delay.toMillis() / 50L);

    ScheduledTask scheduledTask =
        Bukkit.getGlobalRegionScheduler()
            .runDelayed(
                plugin,
                (t) -> {
                  try {
                    task.run();
                  } finally {
                    List<ScheduledTask> instanceTasks = tasks.get(eventId);
                    if (instanceTasks != null) {
                      instanceTasks.removeIf(
                          st ->
                              st.isCancelled()
                                  || st.getExecutionState()
                                      == ScheduledTask.ExecutionState.FINISHED);
                    }
                  }
                },
                ticks);

    tasks.computeIfAbsent(eventId, k -> new CopyOnWriteArrayList<>()).add(scheduledTask);
  }

  @Override
  public void cancelAll(EventInstanceId eventId) {
    List<ScheduledTask> instanceTasks = tasks.remove(eventId);
    if (instanceTasks != null) {
      for (ScheduledTask task : instanceTasks) {
        task.cancel();
      }
    }
  }

  @Override
  public void cancelAll() {
    for (EventInstanceId id : tasks.keySet()) {
      cancelAll(id);
    }
  }
}
