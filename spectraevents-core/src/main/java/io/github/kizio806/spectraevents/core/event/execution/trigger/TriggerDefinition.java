package io.github.kizio806.spectraevents.core.event.execution.trigger;

/**
 * Minimal platform-independent representation of a trigger. A trigger indicates *why* an evaluation
 * or action should happen.
 */
public interface TriggerDefinition {
  /** Gets the string identifier of the trigger type (e.g. "timer_elapsed", "health_crossed") */
  String type();
}
