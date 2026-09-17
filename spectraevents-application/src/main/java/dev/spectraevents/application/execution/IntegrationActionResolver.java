package dev.spectraevents.application.execution;

import dev.spectraevents.core.event.execution.action.ActionDefinition;
import dev.spectraevents.core.event.runtime.EventInstance;

public interface IntegrationActionResolver {
  /**
   * Executes an integration-provided action (e.g., give_money).
   *
   * @return true if successfully executed, false if it failed.
   */
  boolean execute(
      ActionDefinition action,
      EventInstance instance,
      EventRuntimeState state,
      ExecutionContext context)
      throws FatalActionException;

  /**
   * @return true if this resolver supports the given action type.
   */
  boolean supports(String actionType);
}
