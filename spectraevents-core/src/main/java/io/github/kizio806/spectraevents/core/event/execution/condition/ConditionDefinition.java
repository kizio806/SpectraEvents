package io.github.kizio806.spectraevents.core.event.execution.condition;

/**
 * Minimal platform-independent representation of a condition. A condition determines *whether* a
 * transition or action should proceed. Evaluation logic must be deterministic and side-effect free.
 */
public interface ConditionDefinition {
  /** Gets the string identifier of the condition type (e.g. "phase_equals", "players_in_range") */
  String type();

  /** Gets configuration parameters associated with this condition definition. */
  default java.util.Map<String, Object> parameters() {
    return java.util.Map.of();
  }
}
