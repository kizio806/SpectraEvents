package dev.spectraevents.core.event.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.spectraevents.core.event.definition.EventDefinitionId;
import dev.spectraevents.core.event.lifecycle.EventInstanceCompleted;
import dev.spectraevents.core.event.lifecycle.EventInstanceStarted;
import dev.spectraevents.core.event.lifecycle.EventLifecycleEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventLifecycleTransitionTest {
  private static final EventInstanceId FIRST_ID = new EventInstanceId(UUID.randomUUID());
  private static final EventInstanceId SECOND_ID = new EventInstanceId(UUID.randomUUID());
  private static final EventDefinitionId DEF_ID = new EventDefinitionId("meteor");

  @Test
  void holdsTargetStateAndEvent() {
    EventInstance instance =
        EventInstance.reconstitute(FIRST_ID, DEF_ID, EventLifecycleState.RUNNING, null);
    EventLifecycleEvent event = new EventInstanceStarted(FIRST_ID);

    EventLifecycleTransition transition = new EventLifecycleTransition(instance, event);

    assertEquals(instance, transition.eventInstance());
    assertEquals(event, transition.lifecycleEvent());
  }

  @Test
  void rejectsEventFromDifferentInstance() {
    EventInstance instance =
        EventInstance.reconstitute(FIRST_ID, DEF_ID, EventLifecycleState.RUNNING, null);
    EventLifecycleEvent wrongEvent = new EventInstanceStarted(SECOND_ID);

    assertThrows(
        IllegalArgumentException.class, () -> new EventLifecycleTransition(instance, wrongEvent));
  }

  @Test
  void rejectsEventDescribingDifferentState() {
    EventInstance instance =
        EventInstance.reconstitute(FIRST_ID, DEF_ID, EventLifecycleState.RUNNING, null);
    EventLifecycleEvent wrongEvent = new EventInstanceCompleted(FIRST_ID);

    assertThrows(
        IllegalArgumentException.class, () -> new EventLifecycleTransition(instance, wrongEvent));
  }
}
