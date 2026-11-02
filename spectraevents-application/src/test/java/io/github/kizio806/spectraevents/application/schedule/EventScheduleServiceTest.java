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
    assertEquals(1, service.schedules().size());

    // Starting again when already running updates schedules without re-scheduling
    service.start(List.of(matching));
    assertEquals(1, scheduler.globalTasks.size());

    service.replace(List.of());
    assertTrue(service.schedules().isEmpty());

    service.replace(List.of(matching));
    scheduler.globalTasks.removeFirst().run();

    assertTrue(diagnostics.getFirst().contains("nightly-metin skipped"));
    assertEquals(1, scheduler.globalTasks.size());

    service.stop();
    assertTrue(service.schedules().isEmpty());

    // Run callback when stopped
    scheduler.globalTasks.removeFirst().run();
  }

  @Test
  void dispatchesSuccessfullyWhenDefinitionIsRegistered() {
    FakeScheduler scheduler = new FakeScheduler();
    List<String> diagnostics = new ArrayList<>();
    EventSchedule matching =
        EventSchedule.parse(
            "daily-event",
            "sample",
            "* * * * *",
            "UTC",
            new EventLocation("world", 0, 80, 0, 0, 0));

    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    registry.register(sampleDefinition("sample"), "events/sample.yml");
    EventOrchestrationService orchestration =
        new EventOrchestrationService(new InMemoryEventInstanceRepository(), registry);

    EventScheduleService service =
        new EventScheduleService(
            scheduler,
            orchestration,
            Clock.fixed(Instant.parse("2026-09-28T18:00:00Z"), ZoneOffset.UTC),
            diagnostics::add);

    service.start(List.of(matching));
    scheduler.globalTasks.removeFirst().run();

    assertTrue(diagnostics.getFirst().contains("daily-event started sample"));
  }

  private io.github.kizio806.spectraevents.core.event.definition.EventDefinition sampleDefinition(
      String id) {
    io.github.kizio806.spectraevents.core.event.phase.PhaseId phaseId =
        new io.github.kizio806.spectraevents.core.event.phase.PhaseId("active");
    io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition phase =
        new io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition(
            phaseId, java.util.Set.of(), java.util.List.of(), java.util.List.of());
    return new io.github.kizio806.spectraevents.core.event.definition.EventDefinition(
        new io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId(id),
        phaseId,
        java.util.Map.of(phaseId, phase));
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
