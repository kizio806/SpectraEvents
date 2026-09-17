package dev.spectraevents.core.event.execution.action;

/**
 * Minimal platform-independent representation of an action. Represents an effect that occurs after
 * a trigger and its conditions evaluate to true. This can represent domain actions (e.g.,
 * transition phase) or side effects (e.g., spawn boss).
 */
public interface ActionDefinition {
  /** Gets the string identifier of the action type (e.g. "transition", "spawn_model") */
  String type();

  /** Gets configuration parameters associated with this action definition. */
  default java.util.Map<String, Object> parameters() {
    return java.util.Map.of();
  }
}
