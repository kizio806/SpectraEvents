package io.github.kizio806.spectraevents.platform.spigot;

import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class SpigotEventTaskScheduler implements EventTaskScheduler {
  private final Plugin plugin;
  private final Map<EventInstanceId, List<BukkitTask>> tasks = new ConcurrentHashMap<>();
  private final List<BukkitTask> globalTasks = new CopyOnWriteArrayList<>();

  public SpigotEventTaskScheduler(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
    long ticks = Math.max(1L, delay.toMillis() / 50L);
    AtomicReference<BukkitTask> taskReference = new AtomicReference<>();
    BukkitTask bukkitTask =
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  try {
                    task.run();
                  } finally {
                    BukkitTask completedTask = taskReference.get();
                    List<BukkitTask> eventTasks = tasks.get(eventId);
                    if (completedTask != null && eventTasks != null) {
                      eventTasks.remove(completedTask);
                      if (eventTasks.isEmpty()) {
                        tasks.remove(eventId, eventTasks);
                      }
                    }
                  }
                },
                ticks);
    taskReference.set(bukkitTask);
    tasks.computeIfAbsent(eventId, k -> new CopyOnWriteArrayList<>()).add(bukkitTask);
  }

  @Override
  public void scheduleGlobal(Duration delay, Runnable task) {
    long ticks = Math.max(1L, delay.toMillis() / 50L);
    AtomicReference<BukkitTask> taskReference = new AtomicReference<>();
    BukkitTask bukkitTask =
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  try {
                    task.run();
                  } finally {
                    BukkitTask completedTask = taskReference.get();
                    if (completedTask != null) {
                      globalTasks.remove(completedTask);
                    }
                  }
                },
                ticks);
    taskReference.set(bukkitTask);
    globalTasks.add(bukkitTask);
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
    for (BukkitTask task : globalTasks) {
      task.cancel();
    }
    globalTasks.clear();
  }

  @Override
  public int pendingTaskCount(EventInstanceId eventId) {
    return tasks.getOrDefault(eventId, List.of()).size();
  }
}
