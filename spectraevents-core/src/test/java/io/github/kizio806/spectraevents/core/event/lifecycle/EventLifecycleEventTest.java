package io.github.kizio806.spectraevents.core.event.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventLifecycleEventTest {
  private static final EventInstanceId ID = new EventInstanceId(UUID.randomUUID());
  private static final EventDefinitionId DEF_ID = new EventDefinitionId("meteor");

  @Test
  void startedEventReportsRunningState() {
    EventLifecycleEvent event = new EventInstanceStarted(ID);
    assertEquals(ID, event.eventInstanceId());
    assertEquals(EventLifecycleState.RUNNING, event.state());
  }

  @Test
  void completedEventReportsCompletedState() {
    EventLifecycleEvent event = new EventInstanceCompleted(ID);
    assertEquals(ID, event.eventInstanceId());
    assertEquals(EventLifecycleState.COMPLETED, event.state());
  }

  @Test
  void cancelledEventReportsCancelledState() {
    EventLifecycleEvent event = new EventInstanceCancelled(ID);
    assertEquals(ID, event.eventInstanceId());
    assertEquals(EventLifecycleState.CANCELLED, event.state());
  }

  @Test
  void failedEventReportsFailedState() {
    EventLifecycleEvent event = new EventInstanceFailed(ID);
    assertEquals(ID, event.eventInstanceId());
    assertEquals(EventLifecycleState.FAILED, event.state());
  }

  @Test
  void phaseChangedEventReportsRunningStateAndPhases() {
    PhaseId p1 = new PhaseId("p1");
    PhaseId p2 = new PhaseId("p2");
    EventPhaseChanged event = new EventPhaseChanged(ID, DEF_ID, p1, p2);

    assertEquals(ID, event.eventInstanceId());
    assertEquals(DEF_ID, event.definitionId());
    assertEquals(p1, event.previousPhase());
    assertEquals(p2, event.currentPhase());
    assertEquals(EventLifecycleState.RUNNING, event.state());
  }
}
