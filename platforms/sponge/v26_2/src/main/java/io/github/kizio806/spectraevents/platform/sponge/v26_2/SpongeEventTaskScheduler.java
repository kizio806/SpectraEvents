package io.github.kizio806.spectraevents.platform.sponge.v26_2;

import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.time.Duration;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

public class SpongeEventTaskScheduler implements EventTaskScheduler {
  private final SpongeBootstrap plugin;

  public SpongeEventTaskScheduler(SpongeBootstrap plugin) {
    this.plugin = plugin;
  }

  @Override
  public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {
    Sponge.server()
        .scheduler()
        .submit(Task.builder().plugin(plugin.getContainer()).delay(delay).execute(task).build());
  }

  @Override
  public void cancelAll(EventInstanceId eventId) {
    // Not fully implemented task tracking per event
  }

  @Override
  public void cancelAll() {
    // Not fully implemented global cancellation
  }
}
