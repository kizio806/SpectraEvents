package dev.spectraevents.core.event.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.spectraevents.core.event.definition.EventDefinition;
import dev.spectraevents.core.event.definition.EventDefinitionId;
import dev.spectraevents.core.event.lifecycle.EventPhaseChanged;
import dev.spectraevents.core.event.phase.PhaseDefinition;
import dev.spectraevents.core.event.phase.PhaseId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventInstancePhaseTest {
  private static final EventInstanceId ID = new EventInstanceId(UUID.randomUUID());
  private static final EventDefinitionId DEF_ID = new EventDefinitionId("meteor");
  private static final EventDefinitionId WRONG_DEF_ID = new EventDefinitionId("airdrop");

  private static final PhaseId FALLING = new PhaseId("falling");
  private static final PhaseId IMPACT = new PhaseId("impact");
  private static final PhaseId LOCKED = new PhaseId("locked");

  private static final EventDefinition DEFINITION =
      new EventDefinition(
          DEF_ID,
          FALLING,
          Map.of(
              FALLING, new PhaseDefinition(FALLING, Set.of(IMPACT, FALLING)),
              IMPACT, new PhaseDefinition(IMPACT, Set.of(LOCKED)),
              LOCKED, new PhaseDefinition(LOCKED, Set.of())));

  @Test
  void createdInstanceHasNoActivePhase() {
    EventInstance instance = EventInstance.create(ID, DEF_ID);
    assertTrue(instance.currentPhase().isEmpty());
  }

  @Test
  void startingEventEntersInitialPhase() {
    EventInstance instance = EventInstance.create(ID, DEF_ID);
    EventLifecycleTransition transition = instance.start(DEFINITION);

    assertEquals(EventLifecycleState.RUNNING, transition.eventInstance().state());
    assertEquals(FALLING, transition.eventInstance().currentPhase().orElseThrow());
  }

  @Test
  void legalPhaseTransitionSucceeds() {
    EventInstance running = EventInstance.create(ID, DEF_ID).start(DEFINITION).eventInstance();

    EventLifecycleTransition transition = running.transitionPhase(DEFINITION, IMPACT);

    assertNotSame(running, transition.eventInstance());
    assertEquals(IMPACT, transition.eventInstance().currentPhase().orElseThrow());
    assertEquals(FALLING, running.currentPhase().orElseThrow(), "Original instance is unchanged");

    EventPhaseChanged event =
        assertInstanceOf(EventPhaseChanged.class, transition.lifecycleEvent());
    assertEquals(ID, event.eventInstanceId());
    assertEquals(FALLING, event.previousPhase());
    assertEquals(IMPACT, event.currentPhase());
  }

  @Test
  void legalSelfTransitionSucceeds() {
    EventInstance running = EventInstance.create(ID, DEF_ID).start(DEFINITION).eventInstance();

    EventLifecycleTransition transition = running.transitionPhase(DEFINITION, FALLING);

    assertEquals(FALLING, transition.eventInstance().currentPhase().orElseThrow());
  }

  @Test
  void forbiddenTransitionIsRejected() {
    EventInstance running = EventInstance.create(ID, DEF_ID).start(DEFINITION).eventInstance();

    InvalidPhaseTransitionException ex =
        assertThrows(
            InvalidPhaseTransitionException.class,
            () -> running.transitionPhase(DEFINITION, LOCKED));

    assertEquals("Transition is not allowed by phase definition", ex.reason());
    assertEquals(
        FALLING, running.currentPhase().orElseThrow(), "Original instance remains unchanged");
  }

  @Test
  void unknownPhaseIsRejected() {
    EventInstance running = EventInstance.create(ID, DEF_ID).start(DEFINITION).eventInstance();
    PhaseId unknown = new PhaseId("unknown");

    InvalidPhaseTransitionException ex =
        assertThrows(
            InvalidPhaseTransitionException.class,
            () -> running.transitionPhase(DEFINITION, unknown));

    assertEquals("Target phase is unknown to definition", ex.reason());
  }

  @Test
  void definitionMismatchIsRejected() {
    EventInstance running = EventInstance.create(ID, DEF_ID).start(DEFINITION).eventInstance();

    EventDefinition wrongDefinition =
        new EventDefinition(
            WRONG_DEF_ID, FALLING, Map.of(FALLING, new PhaseDefinition(FALLING, Set.of())));

    InvalidPhaseTransitionException ex =
        assertThrows(
            InvalidPhaseTransitionException.class,
            () -> running.transitionPhase(wrongDefinition, FALLING));

    assertEquals("Definition ID mismatch", ex.reason());
  }

  @Test
  void terminalStatesCannotTransitionPhases() {
    EventInstance running = EventInstance.create(ID, DEF_ID).start(DEFINITION).eventInstance();
    EventInstance completed = running.complete().eventInstance();
    EventInstance cancelled = running.cancel().eventInstance();
    EventInstance failed = running.fail().eventInstance();

    assertThrows(
        InvalidPhaseTransitionException.class, () -> completed.transitionPhase(DEFINITION, IMPACT));
    assertThrows(
        InvalidPhaseTransitionException.class, () -> cancelled.transitionPhase(DEFINITION, IMPACT));
    assertThrows(
        InvalidPhaseTransitionException.class, () -> failed.transitionPhase(DEFINITION, IMPACT));
  }

  @Test
  void createdEventCannotTransitionPhaseBeforeStart() {
    EventInstance created = EventInstance.create(ID, DEF_ID);

    assertThrows(
        InvalidPhaseTransitionException.class, () -> created.transitionPhase(DEFINITION, IMPACT));
  }
}
