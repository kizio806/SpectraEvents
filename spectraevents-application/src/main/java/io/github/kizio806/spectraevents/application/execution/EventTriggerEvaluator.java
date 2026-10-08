package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.condition.CoreConditions;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;
import java.util.List;
import java.util.logging.Logger;

class EventTriggerEvaluator {
  private static final Logger LOGGER = Logger.getLogger(EventTriggerEvaluator.class.getName());

  private final List<IntegrationConditionResolver> conditionResolvers;

  EventTriggerEvaluator(List<IntegrationConditionResolver> conditionResolvers) {
    this.conditionResolvers = conditionResolvers;
  }

  boolean matchesTrigger(TriggerDefinition ruleTrigger, TriggerDefinition incomingTrigger) {
    if (!ruleTrigger.type().equalsIgnoreCase(incomingTrigger.type())) {
      return false;
    }
    if (ruleTrigger instanceof CoreTriggers.HealthThresholdCrossedTrigger ruleHealth) {
      if (incomingTrigger instanceof CoreTriggers.HealthThresholdCrossedTrigger incomingHealth) {
        if (ruleHealth.threshold() != -1
            && incomingHealth.threshold() != -1
            && ruleHealth.threshold() != incomingHealth.threshold()) {
          return false;
        }
      }
    }
    if (ruleTrigger instanceof CoreTriggers.HealthPercentThresholdCrossedTrigger ruleHealth) {
      if (incomingTrigger
              instanceof CoreTriggers.HealthPercentThresholdCrossedTrigger incomingHealth
          && ruleHealth.percent() != incomingHealth.percent()) {
        return false;
      }
    }
    if (ruleTrigger instanceof CoreTriggers.HitsPercentThresholdCrossedTrigger ruleHits) {
      if (incomingTrigger instanceof CoreTriggers.HitsPercentThresholdCrossedTrigger incomingHits
          && ruleHits.percent() != incomingHits.percent()) {
        return false;
      }
    }
    if (ruleTrigger instanceof CoreTriggers.WaveClearedTrigger ruleWave
        && incomingTrigger instanceof CoreTriggers.WaveClearedTrigger incomingWave
        && !ruleWave.waveId().equals(incomingWave.waveId())) {
      return false;
    }
    return true;
  }

  boolean evaluateConditions(
      List<ConditionDefinition> conditions, EventRuntimeState state, ExecutionContext context) {
    for (ConditionDefinition cond : conditions) {
      String type = cond.type().toLowerCase(java.util.Locale.ROOT);

      boolean resolvedByIntegration = false;
      for (IntegrationConditionResolver resolver : conditionResolvers) {
        if (resolver.supports(type)) {
          if (!resolver.resolve(cond, null, state, context)) {
            return false;
          }
          resolvedByIntegration = true;
          break;
        }
      }
      if (resolvedByIntegration) continue;

      if (cond instanceof CoreConditions.NotLockedCondition && state.isLocked()) {
        return false;
      }
      if (cond instanceof CoreConditions.IsLockedCondition && !state.isLocked()) {
        return false;
      }
      if (!(cond instanceof CoreConditions.NotLockedCondition)
          && !(cond instanceof CoreConditions.IsLockedCondition)) {
        LOGGER.warning("Unsupported condition type rejected: " + cond.type());
        return false;
      }
    }
    return true;
  }
}
