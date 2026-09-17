package dev.spectraevents.application.port;

import dev.spectraevents.application.execution.EventRuntimeState;
import dev.spectraevents.application.execution.ExecutionContext;
import dev.spectraevents.core.event.execution.action.ActionDefinition;
import dev.spectraevents.core.event.runtime.EventInstance;

/**
 * Port implemented by platform adapters to execute domain actions that require platform
 * side-effects. The core domain emits Actions (e.g., spawn_model, play_sound), and the Application
 * orchestrates them through this port.
 */
public interface PlatformActionPort {

  /**
   * Executes a platform action.
   *
   * @param instance the runtime instance of the event triggering the action
   * @param state the per-instance runtime state
   * @param action the action to execute
   */
  void executeAction(EventInstance instance, EventRuntimeState state, ActionDefinition action);

  /** Executes a platform action with execution context. */
  default void executeAction(
      EventInstance instance,
      EventRuntimeState state,
      ActionDefinition action,
      ExecutionContext context) {
    executeAction(instance, state, action);
  }
}
