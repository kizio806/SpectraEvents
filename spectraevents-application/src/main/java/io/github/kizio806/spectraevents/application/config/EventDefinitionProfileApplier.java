package io.github.kizio806.spectraevents.application.config;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredActionDefinition;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Creates a per-instance definition snapshot with validated operator overrides applied. */
public final class EventDefinitionProfileApplier {
  private EventDefinitionProfileApplier() {}

  /** Supported settings are health, damage, hits, duration, and lock-duration. */
  public static EventDefinition apply(EventDefinition definition, Map<String, Object> settings) {
    Objects.requireNonNull(definition, "definition");
    Objects.requireNonNull(settings, "settings");
    if (settings.isEmpty()) {
      return definition;
    }

    Map<PhaseId, PhaseDefinition> phases = new LinkedHashMap<>();
    for (PhaseId phaseId : definition.phaseIds()) {
      PhaseDefinition phase = definition.phase(phaseId).orElseThrow();
      phases.put(
          phaseId,
          new PhaseDefinition(
              phase.id(),
              phase.allowedTransitions(),
              phase.rules().stream().map(rule -> rewriteRule(rule, settings)).toList(),
              rewriteActions(phase.onEnterActions(), settings)));
    }
    return new EventDefinition(definition.id(), definition.initialPhase(), phases);
  }

  private static TransitionRule rewriteRule(TransitionRule rule, Map<String, Object> settings) {
    if (!(rule.trigger() instanceof ConfiguredTriggerDefinition trigger)) {
      return rule;
    }
    Map<String, Object> parameters = new LinkedHashMap<>(trigger.parameters());
    if ("timer_elapsed".equals(trigger.type()) && settings.containsKey("duration")) {
      parameters.put("duration", settings.get("duration"));
    }
    return new TransitionRule(
        new ConfiguredTriggerDefinition(trigger.type(), parameters),
        rule.conditions(),
        rule.targetPhase(),
        rewriteActions(rule.actions(), settings));
  }

  private static List<ActionDefinition> rewriteActions(
      List<ActionDefinition> actions, Map<String, Object> settings) {
    return actions.stream().map(action -> rewriteAction(action, settings)).toList();
  }

  private static ActionDefinition rewriteAction(
      ActionDefinition action, Map<String, Object> settings) {
    Map<String, Object> parameters = new LinkedHashMap<>(action.parameters());
    switch (action.type()) {
      case "initialize_health" -> replace(parameters, "max", settings, "health");
      case "apply_damage" -> replace(parameters, "amount", settings, "damage");
      case "initialize_hit_counter" -> replace(parameters, "max", settings, "hits");
      case "set_locked" -> replace(parameters, "duration", settings, "lock-duration");
      default -> {
        return action;
      }
    }
    return new ConfiguredActionDefinition(action.type(), parameters);
  }

  private static void replace(
      Map<String, Object> parameters, String target, Map<String, Object> settings, String setting) {
    if (settings.containsKey(setting)) {
      parameters.put(target, settings.get(setting));
    }
  }
}
