package io.github.kizio806.spectraevents.platform.spigot.v26_2;

import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class SpigotEventTaskScheduler implements EventTaskScheduler {
  private final Plugin plugin;
  private final Map<EventInstanceId, List<BukkitTask>> tasks = new ConcurrentHashMap<>();

  public SpigotEventTaskScheduler(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
    long ticks = Math.max(1L, delay.toMillis() / 50L);

    BukkitTask bukkitTask =
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  task.run();
                  List<BukkitTask> eventTasks = tasks.get(eventId);
                  if (eventTasks != null) {
                    // We can't easily remove 'this' task since we're inside the lambda and
                    // bukkitTask is effectively final,
                    // but we can clean up periodically or let cancelAll handle it.
                    // Actually, let's keep it simple. It's safe to keep completed tasks in the list
                    // until cancelAll.
                  }
                },
                ticks);

    tasks.computeIfAbsent(eventId, k -> new CopyOnWriteArrayList<>()).add(bukkitTask);
  }

  @Override
  public void cancelAll(EventInstanceId eventId) {
    List<BukkitTask> eventTasks = tasks.remove(eventId);
    if (eventTasks != null) {
      for (BukkitTask task : eventTasks) {
        task.cancel();
      }
    }
  }

  @Override
  public void cancelAll() {
    for (List<BukkitTask> eventTasks : tasks.values()) {
      for (BukkitTask task : eventTasks) {
        task.cancel();
      }
    }
    tasks.clear();
  }
}
