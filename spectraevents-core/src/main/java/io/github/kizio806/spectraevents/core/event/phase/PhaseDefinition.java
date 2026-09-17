package io.github.kizio806.spectraevents.core.event.phase;

import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Minimal domain representation of a phase configuration.
 *
 * @param id stable phase identity
 * @param allowedTransitions phase identifiers that this phase can transition into
 * @param rules transition rules evaluated during this phase
 * @param onEnterActions actions executed when entering this phase
 */
public record PhaseDefinition(
    PhaseId id,
    Set<PhaseId> allowedTransitions,
    List<TransitionRule> rules,
    List<ActionDefinition> onEnterActions) {
  /**
   * Creates an immutable phase definition.
   *
   * @param id stable phase identity
   * @param allowedTransitions allowed target phases
   * @param rules evaluated transition rules
   * @param onEnterActions actions on enter
   */
  public PhaseDefinition {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(allowedTransitions, "allowedTransitions");
    Objects.requireNonNull(rules, "rules");
    Objects.requireNonNull(onEnterActions, "onEnterActions");

    allowedTransitions = Set.copyOf(allowedTransitions); // defensive copy
    rules = List.copyOf(rules);
    onEnterActions = List.copyOf(onEnterActions);
  }

  /**
   * Compatibility constructor for phase definitions without triggers or enter actions.
   *
   * @param id stable phase identity
   * @param allowedTransitions phase identifiers that this phase can transition into
   */
  public PhaseDefinition(PhaseId id, Set<PhaseId> allowedTransitions) {
    this(id, allowedTransitions, List.of(), List.of());
  }

  /**
   * Checks whether this phase allows a transition to the target phase.
   *
   * @param targetPhase phase to transition to
   * @return {@code true} if allowed
   */
  public boolean allowsTransitionTo(PhaseId targetPhase) {
    return allowedTransitions.contains(targetPhase);
  }
}
