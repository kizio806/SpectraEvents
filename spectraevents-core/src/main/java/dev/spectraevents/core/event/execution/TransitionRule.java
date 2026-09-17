package dev.spectraevents.core.event.execution;

import dev.spectraevents.core.event.execution.action.ActionDefinition;
import dev.spectraevents.core.event.execution.condition.ConditionDefinition;
import dev.spectraevents.core.event.execution.trigger.TriggerDefinition;
import dev.spectraevents.core.event.phase.PhaseId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines a rule for transitioning between phases or executing actions based on a trigger.
 *
 * @param trigger the trigger that initiates the rule evaluation
 * @param conditions conditions that must all be met for the rule to execute
 * @param targetPhase an optional phase to transition to if the rule fires
 * @param actions additional actions to execute when the rule fires
 */
public record TransitionRule(
    TriggerDefinition trigger,
    List<ConditionDefinition> conditions,
    Optional<PhaseId> targetPhase,
    List<ActionDefinition> actions) {

  public TransitionRule {
    Objects.requireNonNull(trigger, "trigger");
    Objects.requireNonNull(conditions, "conditions");
    Objects.requireNonNull(targetPhase, "targetPhase");
    Objects.requireNonNull(actions, "actions");

    conditions = List.copyOf(conditions);
    actions = List.copyOf(actions);
  }
}
