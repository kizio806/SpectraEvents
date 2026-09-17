package dev.spectraevents.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.spectraevents.application.config.registry.EventDefinitionRegistry;
import dev.spectraevents.application.repository.InMemoryEventInstanceRepository;
import dev.spectraevents.core.event.runtime.EventInstance;
import dev.spectraevents.core.event.runtime.EventLifecycleState;
import org.junit.jupiter.api.Test;

class EventOrchestrationServiceTest {
  private final EventOrchestrationService service =
      new EventOrchestrationService(
          new InMemoryEventInstanceRepository(), new EventDefinitionRegistry());

  @Test
  void startsWalkingSkeleton() {
    EventInstance instance = service.startWalkingSkeleton();
    assertNotNull(instance);
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals("phase_one", instance.currentPhase().orElseThrow().value());
  }

  @Test
  void transitionsPhase() {
    EventInstance instance = service.startWalkingSkeleton();

    EventInstance next = service.transitionPhase(instance.id().toString());
    assertEquals("phase_two", next.currentPhase().orElseThrow().value());

    EventInstance finalPhase = service.transitionPhase(instance.id().toString());
    assertEquals("phase_three", finalPhase.currentPhase().orElseThrow().value());
  }

  @Test
  void queriesInfo() {
    EventInstance instance = service.startWalkingSkeleton();
    EventInstance info = service.getEventInfo(instance.id().toString());
    assertEquals(instance.id(), info.id());
  }

  @Test
  void completesEvent() {
    EventInstance instance = service.startWalkingSkeleton();
    EventInstance completed = service.completeEvent(instance.id().toString());
    assertEquals(EventLifecycleState.COMPLETED, completed.state());
  }

  @Test
  void rejectsUnknownInstance() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.getEventInfo("00000000-0000-0000-0000-000000000000"));
  }

  @Test
  void rejectsInvalidUuid() {
    assertThrows(IllegalArgumentException.class, () -> service.getEventInfo("not-a-uuid"));
  }

  @Test
  void rejectsInvalidTransition() {
    EventInstance instance = service.startWalkingSkeleton();
    service.transitionPhase(instance.id().toString()); // to two
    service.transitionPhase(instance.id().toString()); // to three

    // No more transitions
    assertThrows(
        IllegalStateException.class, () -> service.transitionPhase(instance.id().toString()));
  }
}
