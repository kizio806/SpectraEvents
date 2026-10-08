package io.github.kizio806.spectraevents.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventOrchestrationServiceTest {
  private final EventDefinitionRegistry definitions = new EventDefinitionRegistry();
  private final EventOrchestrationService service =
      new EventOrchestrationService(new InMemoryEventInstanceRepository(), definitions);

  EventOrchestrationServiceTest() {
    definitions.register(threePhaseDefinition(), "test-event.yml");
  }

  @Test
  void startsRegisteredDefinition() {
    EventInstance instance = service.startDefinition("test_event");
    assertNotNull(instance);
    assertEquals(EventLifecycleState.RUNNING, instance.state());
    assertEquals("phase_one", instance.currentPhase().orElseThrow().value());
  }

  @Test
  void transitionsPhase() {
    EventInstance instance = service.startDefinition("test_event");

    EventInstance next = service.transitionPhase(instance.id().toString());
    assertEquals("phase_two", next.currentPhase().orElseThrow().value());

    EventInstance finalPhase = service.transitionPhase(instance.id().toString());
    assertEquals("phase_three", finalPhase.currentPhase().orElseThrow().value());
  }

  @Test
  void queriesInfo() {
    EventInstance instance = service.startDefinition("test_event");
    EventInstance info = service.getEventInfo(instance.id().toString());
    assertEquals(instance.id(), info.id());
  }

  @Test
  void completesEvent() {
    EventInstance instance = service.startDefinition("test_event");
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
    EventInstance instance = service.startDefinition("test_event");
    service.transitionPhase(instance.id().toString()); // to two
    service.transitionPhase(instance.id().toString()); // to three

    // No more transitions
    assertThrows(
        IllegalStateException.class, () -> service.transitionPhase(instance.id().toString()));
  }

  private EventDefinition threePhaseDefinition() {
    PhaseId first = new PhaseId("phase_one");
    PhaseId second = new PhaseId("phase_two");
    PhaseId third = new PhaseId("phase_three");
    return new EventDefinition(
        new EventDefinitionId("test_event"),
        first,
        Map.of(
            first, new PhaseDefinition(first, Set.of(second)),
            second, new PhaseDefinition(second, Set.of(third)),
            third, new PhaseDefinition(third, Set.of())));
  }
}
