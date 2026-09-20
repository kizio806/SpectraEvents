package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;

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
   * Checks if this resolver supports the given condition type.
   *
   * @param conditionType the condition type to check
   * @return true if this resolver supports the given condition type
   */
  boolean supports(String conditionType);
}
