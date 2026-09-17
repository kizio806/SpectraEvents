package dev.spectraevents.application.execution;

import dev.spectraevents.core.event.execution.condition.ConditionDefinition;
import dev.spectraevents.core.event.runtime.EventInstance;

public interface IntegrationConditionResolver {
  /**
   * Resolves an integration-provided condition (e.g., luckperms_group).
   *
   * @return true if the condition is met, false otherwise.
   */
  boolean resolve(
      ConditionDefinition condition,
      EventInstance instance,
      EventRuntimeState state,
      ExecutionContext context);

  /**
   * @return true if this resolver supports the given condition type.
   */
  boolean supports(String conditionType);
}
