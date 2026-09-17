package dev.spectraevents.application.config.compiler;

import dev.spectraevents.application.config.compiled.ConfiguredActionDefinition;
import dev.spectraevents.application.config.compiled.ConfiguredConditionDefinition;
import dev.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import dev.spectraevents.application.config.spec.EventSpec;
import dev.spectraevents.application.config.spec.PhaseSpec;
import dev.spectraevents.application.config.validation.ValidationDiagnostic;
import dev.spectraevents.core.event.definition.EventDefinition;
import dev.spectraevents.core.event.definition.EventDefinitionId;
import dev.spectraevents.core.event.execution.TransitionRule;
import dev.spectraevents.core.event.execution.action.ActionDefinition;
import dev.spectraevents.core.event.execution.condition.ConditionDefinition;
import dev.spectraevents.core.event.execution.trigger.TriggerDefinition;
import dev.spectraevents.core.event.phase.PhaseDefinition;
import dev.spectraevents.core.event.phase.PhaseId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compiles a raw {@link EventSpec} into a validated, immutable {@link EventDefinition}. Rejects
 * compilation entirely if any errors are encountered.
 */
public class EventDefinitionCompiler {

  /**
   * Compiles an EventSpec.
   *
   * @param spec raw authoring model
   * @return a compiled EventDefinition
   * @throws EventDefinitionCompilerException if validation errors occur
   */
  public EventDefinition compile(EventSpec spec) {
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();

    if (spec.id() == null || spec.id().isBlank()) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR, "SE-DEF-001", "id", "Event ID cannot be blank"));
    }

    if (spec.initialPhase() == null || spec.initialPhase().isBlank()) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-DEF-002",
              "initialPhase",
              "Initial phase cannot be blank"));
    } else if (spec.phases() != null && !spec.phases().containsKey(spec.initialPhase())) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-DEF-003",
              "initialPhase",
              "Initial phase must exist in phases map: " + spec.initialPhase()));
    }

    if (spec.phases() == null || spec.phases().isEmpty()) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-DEF-004",
              "phases",
              "At least one phase must be defined"));
    }

    if (!diagnostics.isEmpty()) {
      throw new EventDefinitionCompilerException("Failed to compile EventDefinition", diagnostics);
    }

    Map<PhaseId, PhaseDefinition> compiledPhases = new HashMap<>();

    for (Map.Entry<String, PhaseSpec> entry : spec.phases().entrySet()) {
      String phaseName = entry.getKey();
      PhaseSpec phaseSpec = entry.getValue();
      PhaseId phaseId = new PhaseId(phaseName);

      Set<PhaseId> allowedTransitions = Set.of();
      if (phaseSpec.allowedTransitions() != null) {
        allowedTransitions =
            phaseSpec.allowedTransitions().stream().map(PhaseId::new).collect(Collectors.toSet());
        // Validate target exists
        for (String target : phaseSpec.allowedTransitions()) {
          if (!spec.phases().containsKey(target)) {
            diagnostics.add(
                new ValidationDiagnostic(
                    ValidationDiagnostic.Severity.ERROR,
                    "SE-DEF-005",
                    "phases." + phaseName + ".allowedTransitions",
                    "Transition target phase does not exist: " + target));
          }
        }
      }

      List<ActionDefinition> onEnter = new ArrayList<>();
      if (phaseSpec.onEnter() != null) {
        onEnter =
            phaseSpec.onEnter().stream()
                .map(a -> new ConfiguredActionDefinition(a.type(), a.parameters()))
                .collect(Collectors.toList());
      }

      List<TransitionRule> rules = new ArrayList<>();
      if (phaseSpec.transitions() != null) {
        for (int i = 0; i < phaseSpec.transitions().size(); i++) {
          var tSpec = phaseSpec.transitions().get(i);
          String path = "phases." + phaseName + ".transitions[" + i + "]";

          if (tSpec.trigger() == null) {
            diagnostics.add(
                new ValidationDiagnostic(
                    ValidationDiagnostic.Severity.ERROR,
                    "SE-DEF-006",
                    path + ".trigger",
                    "Trigger is required"));
            continue;
          }

          TriggerDefinition triggerDef =
              new ConfiguredTriggerDefinition(tSpec.trigger().type(), tSpec.trigger().parameters());

          List<ConditionDefinition> conditions = new ArrayList<>();
          if (tSpec.conditions() != null) {
            conditions =
                tSpec.conditions().stream()
                    .map(c -> new ConfiguredConditionDefinition(c.type(), c.parameters()))
                    .collect(Collectors.toList());
          }

          List<ActionDefinition> actions = new ArrayList<>();
          if (tSpec.actions() != null) {
            actions =
                tSpec.actions().stream()
                    .map(a -> new ConfiguredActionDefinition(a.type(), a.parameters()))
                    .collect(Collectors.toList());
          }

          Optional<PhaseId> targetPhase = Optional.empty();
          if (tSpec.targetPhase() != null && !tSpec.targetPhase().isBlank()) {
            if (!spec.phases().containsKey(tSpec.targetPhase())) {
              diagnostics.add(
                  new ValidationDiagnostic(
                      ValidationDiagnostic.Severity.ERROR,
                      "SE-DEF-007",
                      path + ".targetPhase",
                      "Transition target phase does not exist: " + tSpec.targetPhase()));
            }
            targetPhase = Optional.of(new PhaseId(tSpec.targetPhase()));
          }

          rules.add(new TransitionRule(triggerDef, conditions, targetPhase, actions));
        }
      }

      compiledPhases.put(phaseId, new PhaseDefinition(phaseId, allowedTransitions, rules, onEnter));
    }

    if (!diagnostics.isEmpty()) {
      throw new EventDefinitionCompilerException("Failed to compile EventDefinition", diagnostics);
    }

    return new EventDefinition(
        new EventDefinitionId(spec.id()), new PhaseId(spec.initialPhase()), compiledPhases);
  }
}
