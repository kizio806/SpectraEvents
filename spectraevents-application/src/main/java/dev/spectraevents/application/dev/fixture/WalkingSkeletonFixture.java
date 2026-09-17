package dev.spectraevents.application.dev.fixture;

import dev.spectraevents.core.event.definition.EventDefinition;
import dev.spectraevents.core.event.definition.EventDefinitionId;
import dev.spectraevents.core.event.phase.PhaseDefinition;
import dev.spectraevents.core.event.phase.PhaseId;
import java.util.Map;
import java.util.Set;

/** Test fixture providing a walking skeleton event definition for the developer workflow. */
public final class WalkingSkeletonFixture {
  private WalkingSkeletonFixture() {}

  public static final EventDefinitionId DEFINITION_ID = new EventDefinitionId("walking_skeleton");

  public static final PhaseId PHASE_ONE = new PhaseId("phase_one");
  public static final PhaseId PHASE_TWO = new PhaseId("phase_two");
  public static final PhaseId PHASE_THREE = new PhaseId("phase_three");

  /**
   * Provides the walking skeleton event definition. phase_one -> phase_two -> phase_three
   *
   * @return the test definition
   */
  public static EventDefinition getDefinition() {
    return new EventDefinition(
        DEFINITION_ID,
        PHASE_ONE,
        Map.of(
            PHASE_ONE, new PhaseDefinition(PHASE_ONE, Set.of(PHASE_TWO)),
            PHASE_TWO, new PhaseDefinition(PHASE_TWO, Set.of(PHASE_THREE)),
            PHASE_THREE, new PhaseDefinition(PHASE_THREE, Set.of())));
  }
}
