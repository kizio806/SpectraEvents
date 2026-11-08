package io.github.kizio806.spectraevents.application.execution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.execution.condition.ConditionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.condition.CoreConditions;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventTriggerEvaluatorTest {

  @Test
  void matchesTriggerByExactTypeAndAttributes() {
    EventTriggerEvaluator evaluator = new EventTriggerEvaluator(List.of());

    // 1. Type mismatch
    assertFalse(
        evaluator.matchesTrigger(
            new CoreTriggers.LootItemTakenTrigger(),
            new CoreTriggers.LootContainerEmptiedTrigger()));

    // 2. HealthThresholdCrossedTrigger
    assertTrue(
        evaluator.matchesTrigger(
            new CoreTriggers.HealthThresholdCrossedTrigger(50),
            new CoreTriggers.HealthThresholdCrossedTrigger(50)));
    assertFalse(
        evaluator.matchesTrigger(
            new CoreTriggers.HealthThresholdCrossedTrigger(50),
            new CoreTriggers.HealthThresholdCrossedTrigger(25)));
    assertTrue(
        evaluator.matchesTrigger(
            new CoreTriggers.HealthThresholdCrossedTrigger(-1),
            new CoreTriggers.HealthThresholdCrossedTrigger(50)));
    assertTrue(
        evaluator.matchesTrigger(
            new CoreTriggers.HealthThresholdCrossedTrigger(50),
            new CoreTriggers.HealthThresholdCrossedTrigger(-1)));

    // 3. HealthPercentThresholdCrossedTrigger
    assertTrue(
        evaluator.matchesTrigger(
            new CoreTriggers.HealthPercentThresholdCrossedTrigger(50),
            new CoreTriggers.HealthPercentThresholdCrossedTrigger(50)));
    assertFalse(
        evaluator.matchesTrigger(
            new CoreTriggers.HealthPercentThresholdCrossedTrigger(50),
            new CoreTriggers.HealthPercentThresholdCrossedTrigger(25)));

    // 4. HitsPercentThresholdCrossedTrigger
    assertTrue(
        evaluator.matchesTrigger(
            new CoreTriggers.HitsPercentThresholdCrossedTrigger(75),
            new CoreTriggers.HitsPercentThresholdCrossedTrigger(75)));
    assertFalse(
        evaluator.matchesTrigger(
            new CoreTriggers.HitsPercentThresholdCrossedTrigger(75),
            new CoreTriggers.HitsPercentThresholdCrossedTrigger(50)));

    // 5. WaveClearedTrigger
    assertTrue(
        evaluator.matchesTrigger(
            new CoreTriggers.WaveClearedTrigger("wave1"),
            new CoreTriggers.WaveClearedTrigger("wave1")));
    assertFalse(
        evaluator.matchesTrigger(
            new CoreTriggers.WaveClearedTrigger("wave1"),
            new CoreTriggers.WaveClearedTrigger("wave2")));

    // 6. Generic triggers
    TriggerDefinition custom1 = () -> "custom_trigger";
    TriggerDefinition custom2 = () -> "custom_trigger";
    assertTrue(evaluator.matchesTrigger(custom1, custom2));
  }

  @Test
  void evaluatesCoreAndIntegrationConditions() {
    IntegrationConditionResolver mockResolver =
        new IntegrationConditionResolver() {
          @Override
          public boolean resolve(
              ConditionDefinition condition,
              EventInstance instance,
              EventRuntimeState state,
              ExecutionContext context) {
            return "integration_pass".equals(condition.type());
          }

          @Override
          public boolean supports(String conditionType) {
            return conditionType.startsWith("integration_");
          }
        };

    EventTriggerEvaluator evaluator = new EventTriggerEvaluator(List.of(mockResolver));
    EventRuntimeState state = new EventRuntimeState(EventInstanceId.generate());
    ExecutionContext context = ExecutionContext.EMPTY;

    // Empty conditions
    assertTrue(evaluator.evaluateConditions(List.of(), state, context));

    // Integration condition pass and fail
    assertTrue(evaluator.evaluateConditions(List.of(() -> "integration_pass"), state, context));
    assertFalse(evaluator.evaluateConditions(List.of(() -> "integration_fail"), state, context));

    // NotLockedCondition
    state.setLockedUntilMillis(0L);
    assertTrue(
        evaluator.evaluateConditions(
            List.of(new CoreConditions.NotLockedCondition()), state, context));
    state.setLockedUntilMillis(System.currentTimeMillis() + 100_000L);
    assertFalse(
        evaluator.evaluateConditions(
            List.of(new CoreConditions.NotLockedCondition()), state, context));

    // IsLockedCondition
    state.setLockedUntilMillis(System.currentTimeMillis() + 100_000L);
    assertTrue(
        evaluator.evaluateConditions(
            List.of(new CoreConditions.IsLockedCondition()), state, context));
    state.setLockedUntilMillis(0L);
    assertFalse(
        evaluator.evaluateConditions(
            List.of(new CoreConditions.IsLockedCondition()), state, context));

    // Unsupported condition type
    assertFalse(evaluator.evaluateConditions(List.of(() -> "unsupported_cond"), state, context));
  }
}
