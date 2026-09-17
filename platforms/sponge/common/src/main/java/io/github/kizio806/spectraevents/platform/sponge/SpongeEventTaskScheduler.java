package io.github.kizio806.spectraevents.platform.sponge;

import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;

public class SpongeEventTaskScheduler implements EventTaskScheduler {
  private final SpongeBootstrap plugin;
  private final Map<EventInstanceId, List<ScheduledTask>> tasks = new ConcurrentHashMap<>();

  public SpongeEventTaskScheduler(SpongeBootstrap plugin) {
    this.plugin = plugin;
  }

  @Override
  public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
    if (!Sponge.isServerAvailable()) return;

    ScheduledTask scheduledTask =
        Sponge.server()
            .scheduler()
            .submit(
                Task.builder().plugin(plugin.getContainer()).delay(delay).execute(task).build());

    tasks.computeIfAbsent(eventId, k -> new ArrayList<>()).add(scheduledTask);
  }

  @Override
  public void cancelAll(EventInstanceId eventId) {
    List<ScheduledTask> eventTasks = tasks.remove(eventId);
    if (eventTasks != null) {
      for (ScheduledTask task : eventTasks) {
        task.cancel();
      }
    }
  }

  @Override
  public void cancelAll() {
    for (List<ScheduledTask> eventTasks : tasks.values()) {
      for (ScheduledTask task : eventTasks) {
        task.cancel();
      }
    }
    tasks.clear();
  }
}
