package io.github.kizio806.spectraevents.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventScheduleServiceTest {
  @Test
  void dispatchesOnlyObservedMatchingMinutesAndIsolatesAConflictingStart() {
    FakeScheduler scheduler = new FakeScheduler();
    List<String> diagnostics = new ArrayList<>();
    EventSchedule matching =
        EventSchedule.parse(
            "nightly-metin",
            "metin",
            "0 20 * * *",
            "Europe/Warsaw",
            new EventLocation("world", 0, 80, 0, 0, 0));
    EventOrchestrationService orchestration =
        new EventOrchestrationService(
            new InMemoryEventInstanceRepository(), new EventDefinitionRegistry());
    EventScheduleService service =
        new EventScheduleService(
            scheduler,
            orchestration,
            Clock.fixed(Instant.parse("2026-09-28T18:00:00Z"), ZoneOffset.UTC),
            diagnostics::add);

    service.start(List.of(matching));
    assertEquals(1, scheduler.globalTasks.size());
    scheduler.globalTasks.removeFirst().run();

    assertTrue(diagnostics.getFirst().contains("nightly-metin skipped"));
    assertEquals(1, scheduler.globalTasks.size());
  }

  private static final class FakeScheduler implements EventTaskScheduler {
    private final List<Runnable> globalTasks = new ArrayList<>();

    @Override
    public void schedule(EventInstanceId eventId, Duration delay, Runnable task) {}

    @Override
    public void scheduleGlobal(Duration delay, Runnable task) {
      globalTasks.add(task);
    }

    @Override
    public void cancelAll(EventInstanceId eventId) {}

    @Override
    public void cancelAll() {}
  }
}
