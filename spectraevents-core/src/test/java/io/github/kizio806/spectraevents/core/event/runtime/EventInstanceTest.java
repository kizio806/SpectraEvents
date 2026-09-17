package io.github.kizio806.spectraevents.core.event.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.lifecycle.EventInstanceCancelled;
import io.github.kizio806.spectraevents.core.event.lifecycle.EventInstanceCompleted;
import io.github.kizio806.spectraevents.core.event.lifecycle.EventInstanceFailed;
import io.github.kizio806.spectraevents.core.event.lifecycle.EventInstanceStarted;
import io.github.kizio806.spectraevents.core.event.lifecycle.EventLifecycleEvent;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EventInstanceTest {
  private static final EventInstanceId ID =
      new EventInstanceId(UUID.fromString("c56a4180-65aa-42ec-a945-5fd21dec0538"));
  private static final EventDefinitionId DEF_ID = new EventDefinitionId("meteor");

  private static final PhaseId INITIAL_PHASE = new PhaseId("falling");
  private static final EventDefinition DEFINITION =
      new EventDefinition(
          DEF_ID,
          INITIAL_PHASE,
          Map.of(INITIAL_PHASE, new PhaseDefinition(INITIAL_PHASE, Set.of())));

  @Test
  void createsAnInstanceInCreatedState() {
    EventInstance eventInstance = EventInstance.create(ID, DEF_ID);

    assertEquals(ID, eventInstance.id());
    assertEquals(DEF_ID, eventInstance.definitionId());
    assertEquals(EventLifecycleState.CREATED, eventInstance.state());
    assertTrue(eventInstance.currentPhase().isEmpty());
  }

  @Test
  void rejectsAFactoryCallWithoutAnIdentity() {
    assertThrows(NullPointerException.class, () -> EventInstance.create(null, DEF_ID));
    assertThrows(NullPointerException.class, () -> EventInstance.create(ID, null));
  }

  @ParameterizedTest(name = "{0} can {1} and enter {2}")
  @MethodSource("allowedTransitions")
  void performsEveryAllowedTransition(
      EventLifecycleState initialState,
      Operation operation,
      EventLifecycleState expectedState,
      Class<? extends EventLifecycleEvent> expectedEventType) {
    EventInstance initial = instanceIn(initialState);

    EventLifecycleTransition transition = operation.apply(initial);

    assertNotSame(initial, transition.eventInstance());
    assertEquals(initialState, initial.state(), "the original aggregate must remain unchanged");
    assertEquals(ID, transition.eventInstance().id());
    assertEquals(expectedState, transition.eventInstance().state());
    assertInstanceOf(expectedEventType, transition.lifecycleEvent());
    assertEquals(ID, transition.lifecycleEvent().eventInstanceId());
    assertEquals(expectedState, transition.lifecycleEvent().state());
  }

  @ParameterizedTest(name = "{0} cannot {1}")
  @MethodSource("rejectedTransitions")
  void rejectsEveryDisallowedTransition(EventLifecycleState initialState, Operation operation) {
    EventInstance initial = instanceIn(initialState);

    InvalidEventLifecycleTransitionException exception =
        assertThrows(
            InvalidEventLifecycleTransitionException.class, () -> operation.apply(initial));

    assertEquals(ID, exception.eventInstanceId());
    assertEquals(initialState, exception.currentState());
    assertEquals(operation.destination(), exception.requestedState());
    assertEquals(initialState, initial.state(), "a rejected transition must not change state");
  }

  private static Stream<Arguments> allowedTransitions() {
    return Stream.of(
        Arguments.of(
            EventLifecycleState.CREATED,
            Operation.START,
            EventLifecycleState.RUNNING,
            EventInstanceStarted.class),
        Arguments.of(
            EventLifecycleState.RUNNING,
            Operation.COMPLETE,
            EventLifecycleState.COMPLETED,
            EventInstanceCompleted.class),
        Arguments.of(
            EventLifecycleState.RUNNING,
            Operation.CANCEL,
            EventLifecycleState.CANCELLED,
            EventInstanceCancelled.class),
        Arguments.of(
            EventLifecycleState.RUNNING,
            Operation.FAIL,
            EventLifecycleState.FAILED,
            EventInstanceFailed.class));
  }

  private static Stream<Arguments> rejectedTransitions() {
    return Arrays.stream(EventLifecycleState.values())
        .flatMap(
            state ->
                Arrays.stream(Operation.values())
                    .filter(operation -> !operation.isAllowedFrom(state))
                    .map(operation -> Arguments.of(state, operation)));
  }

  private static EventInstance instanceIn(EventLifecycleState state) {
    EventInstance created = EventInstance.create(ID, DEF_ID);
    if (state == EventLifecycleState.CREATED) {
      return created;
    }

    EventInstance running = created.start(DEFINITION).eventInstance();
    return switch (state) {
      case RUNNING -> running;
      case COMPLETED -> running.complete().eventInstance();
      case CANCELLED -> running.cancel().eventInstance();
      case FAILED -> running.fail().eventInstance();
      case CREATED -> throw new AssertionError("CREATED handled before switch");
    };
  }

  private enum Operation {
    START(EventLifecycleState.CREATED, EventLifecycleState.RUNNING, i -> i.start(DEFINITION)),
    COMPLETE(EventLifecycleState.RUNNING, EventLifecycleState.COMPLETED, EventInstance::complete),
    CANCEL(EventLifecycleState.RUNNING, EventLifecycleState.CANCELLED, EventInstance::cancel),
    FAIL(EventLifecycleState.RUNNING, EventLifecycleState.FAILED, EventInstance::fail);

    private final EventLifecycleState source;
    private final EventLifecycleState destination;
    private final Function<EventInstance, EventLifecycleTransition> transition;

    Operation(
        EventLifecycleState source,
        EventLifecycleState destination,
        Function<EventInstance, EventLifecycleTransition> transition) {
      this.source = source;
      this.destination = destination;
      this.transition = transition;
    }

    EventLifecycleTransition apply(EventInstance eventInstance) {
      return transition.apply(eventInstance);
    }

    boolean isAllowedFrom(EventLifecycleState state) {
      return source == state;
    }

    EventLifecycleState destination() {
      return destination;
    }
  }
}
