package io.github.kizio806.spectraevents.core.event.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventDefinitionTest {
  private static final EventDefinitionId DEF_ID = new EventDefinitionId("meteor");
  private static final PhaseId INITIAL = new PhaseId("falling");
  private static final PhaseId NEXT = new PhaseId("impact");

  @Test
  void createsValidDefinition() {
    Map<PhaseId, PhaseDefinition> phases =
        Map.of(
            INITIAL, new PhaseDefinition(INITIAL, Set.of(NEXT)),
            NEXT, new PhaseDefinition(NEXT, Set.of()));

    EventDefinition definition = new EventDefinition(DEF_ID, INITIAL, phases);

    assertEquals(DEF_ID, definition.id());
    assertEquals(INITIAL, definition.initialPhase());
    assertTrue(definition.phase(INITIAL).isPresent());
    assertTrue(definition.phase(NEXT).isPresent());
  }

  @Test
  void rejectsEmptyPhases() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new EventDefinition(DEF_ID, INITIAL, Collections.emptyMap()));
  }

  @Test
  void rejectsMissingInitialPhase() {
    Map<PhaseId, PhaseDefinition> phases = Map.of(NEXT, new PhaseDefinition(NEXT, Set.of()));

    assertThrows(
        IllegalArgumentException.class, () -> new EventDefinition(DEF_ID, INITIAL, phases));
  }

  @Test
  void rejectsPhaseIdMismatch() {
    PhaseId badId = new PhaseId("wrong_key");
    Map<PhaseId, PhaseDefinition> phases =
        Map.of(
            INITIAL, new PhaseDefinition(INITIAL, Set.of()),
            badId, new PhaseDefinition(NEXT, Set.of()));

    assertThrows(
        IllegalArgumentException.class, () -> new EventDefinition(DEF_ID, INITIAL, phases));
  }

  @Test
  void rejectsTransitionToUnknownPhase() {
    Map<PhaseId, PhaseDefinition> phases =
        Map.of(
            INITIAL, new PhaseDefinition(INITIAL, Set.of(NEXT))
            // NEXT is missing
            );

    assertThrows(
        IllegalArgumentException.class, () -> new EventDefinition(DEF_ID, INITIAL, phases));
  }

  @Test
  void defendsAgainstMapMutation() {
    Map<PhaseId, PhaseDefinition> phases = new HashMap<>();
    phases.put(INITIAL, new PhaseDefinition(INITIAL, Set.of()));

    EventDefinition definition = new EventDefinition(DEF_ID, INITIAL, phases);

    // Mutate the original map
    phases.clear();

    // Definition should still be valid and contain the initial phase
    assertTrue(definition.phase(INITIAL).isPresent());
  }
}
