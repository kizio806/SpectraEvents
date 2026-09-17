package io.github.kizio806.spectraevents.application.dev.fixture;

import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.util.Map;
import java.util.Set;

/** Test fixture providing a dev_meteor event definition for the developer workflow. */
public final class MeteorFixture {
  private MeteorFixture() {}

  public static final EventDefinitionId DEFINITION_ID = new EventDefinitionId("dev_meteor");

  public static final PhaseId PHASE_FALLING = new PhaseId("falling");
  public static final PhaseId PHASE_IMPACT = new PhaseId("impact");
  public static final PhaseId PHASE_LOCKED = new PhaseId("locked");
  public static final PhaseId PHASE_ACTIVE = new PhaseId("active");
  public static final PhaseId PHASE_DESTROYED = new PhaseId("destroyed");

  /**
   * Provides the dev_meteor event definition. falling -> impact -> locked -> active -> destroyed
   *
   * @return the test definition
   */
  public static EventDefinition getDefinition() {
    return new EventDefinition(
        DEFINITION_ID,
        PHASE_FALLING,
        Map.of(
            PHASE_FALLING, new PhaseDefinition(PHASE_FALLING, Set.of(PHASE_IMPACT)),
            PHASE_IMPACT, new PhaseDefinition(PHASE_IMPACT, Set.of(PHASE_LOCKED)),
            PHASE_LOCKED, new PhaseDefinition(PHASE_LOCKED, Set.of(PHASE_ACTIVE)),
            PHASE_ACTIVE, new PhaseDefinition(PHASE_ACTIVE, Set.of(PHASE_DESTROYED)),
            PHASE_DESTROYED, new PhaseDefinition(PHASE_DESTROYED, Set.of())));
  }
}
