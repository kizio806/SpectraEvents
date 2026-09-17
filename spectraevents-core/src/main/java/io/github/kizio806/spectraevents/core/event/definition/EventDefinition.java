package io.github.kizio806.spectraevents.core.event.definition;

import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable definition of an event and its phase lifecycle. */
public final class EventDefinition {
  private final EventDefinitionId id;
  private final PhaseId initialPhase;
  private final Map<PhaseId, PhaseDefinition> phases;

  /**
   * Creates and validates an event definition.
   *
   * @param id stable event definition identity
   * @param initialPhase the phase where the event starts
   * @param phases all phases defined for this event
   * @throws IllegalArgumentException if invariants are broken
   */
  public EventDefinition(
      EventDefinitionId id, PhaseId initialPhase, Map<PhaseId, PhaseDefinition> phases) {
    this.id = Objects.requireNonNull(id, "id");
    this.initialPhase = Objects.requireNonNull(initialPhase, "initialPhase");
    Objects.requireNonNull(phases, "phases");

    if (phases.isEmpty()) {
      throw new IllegalArgumentException("An event definition must have at least one phase");
    }

    if (!phases.containsKey(initialPhase)) {
      throw new IllegalArgumentException("Initial phase must exist in the phases map");
    }

    this.phases = Map.copyOf(phases);

    for (Map.Entry<PhaseId, PhaseDefinition> entry : this.phases.entrySet()) {
      PhaseId phaseId = entry.getKey();
      PhaseDefinition phaseDef = entry.getValue();

      if (!phaseId.equals(phaseDef.id())) {
        throw new IllegalArgumentException(
            "Phase map key (%s) does not match definition id (%s)"
                .formatted(phaseId, phaseDef.id()));
      }

      for (PhaseId target : phaseDef.allowedTransitions()) {
        if (!this.phases.containsKey(target)) {
          throw new IllegalArgumentException(
              "Phase %s allows transition to unknown phase: %s".formatted(phaseId, target));
        }
      }
    }
  }

  public EventDefinitionId id() {
    return id;
  }

  public PhaseId initialPhase() {
    return initialPhase;
  }

  public Optional<PhaseDefinition> phase(PhaseId phaseId) {
    return Optional.ofNullable(phases.get(phaseId));
  }

  /** Returns the stable identifiers of all phases in this definition. */
  public Set<PhaseId> phaseIds() {
    return phases.keySet();
  }
}
