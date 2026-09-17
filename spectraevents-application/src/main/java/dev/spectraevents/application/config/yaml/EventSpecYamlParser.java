package dev.spectraevents.application.config.yaml;

import dev.spectraevents.application.config.compiler.EventDefinitionCompilerException;
import dev.spectraevents.application.config.spec.ActionSpec;
import dev.spectraevents.application.config.spec.ConditionSpec;
import dev.spectraevents.application.config.spec.EventSpec;
import dev.spectraevents.application.config.spec.PhaseSpec;
import dev.spectraevents.application.config.spec.TransitionSpec;
import dev.spectraevents.application.config.spec.TriggerSpec;
import dev.spectraevents.application.config.validation.ValidationDiagnostic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/** Safely parses YAML into an EventSpec. Does not allow polymorphic deserialization. */
public class EventSpecYamlParser {

  private final Yaml yaml;

  public EventSpecYamlParser() {
    LoaderOptions options = new LoaderOptions();
    // Prevent generic object instantiation for security
    this.yaml = new Yaml(new SafeConstructor(options));
  }

  public EventSpec parse(String yamlContent, String sourceFile) {
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();
    Object loaded;

    try {
      loaded = yaml.load(yamlContent);
    } catch (YAMLException e) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-001",
              "root",
              "YAML Syntax Error: " + e.getMessage()));
      throw new EventDefinitionCompilerException("Failed to parse YAML", diagnostics);
    }

    if (!(loaded instanceof Map<?, ?> rootMap)) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-002",
              "root",
              "YAML root must be an object/map"));
      throw new EventDefinitionCompilerException("Failed to parse YAML", diagnostics);
    }

    String schemaVersion = getString(rootMap, "schema-version");
    if (schemaVersion == null) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-003",
              "schema-version",
              "schema-version is required"));
    } else if (!"1".equals(String.valueOf(schemaVersion))) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-004",
              "schema-version",
              "Unsupported schema-version: " + schemaVersion));
    }

    if (!diagnostics.isEmpty()) {
      throw new EventDefinitionCompilerException("YAML validation failed", diagnostics);
    }

    String id = getString(rootMap, "id");
    String initialPhase = getString(rootMap, "initial-phase");

    Map<String, PhaseSpec> phases = new HashMap<>();
    Object phasesObj = rootMap.get("phases");
    if (phasesObj instanceof Map<?, ?> phasesMap) {
      for (Map.Entry<?, ?> entry : phasesMap.entrySet()) {
        String phaseName = String.valueOf(entry.getKey());
        if (entry.getValue() instanceof Map<?, ?> phaseMap) {
          phases.put(phaseName, parsePhase(phaseMap, "phases." + phaseName, diagnostics));
        } else if (entry.getValue() == null) {
          phases.put(phaseName, new PhaseSpec(Set.of(), List.of(), List.of()));
        } else {
          diagnostics.add(
              new ValidationDiagnostic(
                  ValidationDiagnostic.Severity.ERROR,
                  "SE-YAML-005",
                  "phases." + phaseName,
                  "Phase must be an object"));
        }
      }
    }

    if (!diagnostics.isEmpty()) {
      throw new EventDefinitionCompilerException("YAML validation failed", diagnostics);
    }

    return new EventSpec(id, schemaVersion, initialPhase, phases);
  }

  private PhaseSpec parsePhase(
      Map<?, ?> phaseMap, String path, List<ValidationDiagnostic> diagnostics) {
    Set<String> allowedTransitions = new LinkedHashSet<>();
    List<TransitionSpec> transitions = new ArrayList<>();
    List<ActionSpec> onEnter = new ArrayList<>();

    Object transitionsObj = phaseMap.get("transitions");
    if (transitionsObj instanceof List<?> transitionsList) {
      for (int i = 0; i < transitionsList.size(); i++) {
        Object tObj = transitionsList.get(i);
        if (tObj instanceof Map<?, ?> tMap) {
          String tPath = path + ".transitions[" + i + "]";
          TransitionSpec spec = parseTransition(tMap, tPath, diagnostics);
          transitions.add(spec);
          if (spec.targetPhase() != null) {
            allowedTransitions.add(spec.targetPhase());
          }
        }
      }
    }

    Object onEnterObj = phaseMap.get("onEnter");
    if (onEnterObj == null) {
      onEnterObj = phaseMap.get("on-enter");
    }
    if (onEnterObj == null) {
      onEnterObj = phaseMap.get("on_enter");
    }
    if (onEnterObj instanceof List<?> onEnterList) {
      for (int i = 0; i < onEnterList.size(); i++) {
        Object aObj = onEnterList.get(i);
        if (aObj instanceof Map<?, ?> aMap) {
          onEnter.add(parseAction(aMap, path + ".onEnter[" + i + "]", diagnostics));
        }
      }
    }

    return new PhaseSpec(allowedTransitions, transitions, onEnter);
  }

  private TransitionSpec parseTransition(
      Map<?, ?> tMap, String path, List<ValidationDiagnostic> diagnostics) {
    TriggerSpec trigger = null;
    List<ConditionSpec> conditions = new ArrayList<>();
    List<ActionSpec> actions = new ArrayList<>();
    String targetPhase = getString(tMap, "target");
    if (targetPhase == null) {
      targetPhase = getString(tMap, "target-phase");
    }
    if (targetPhase == null) {
      targetPhase = getString(tMap, "targetPhase");
    }
    if (targetPhase == null) {
      targetPhase = getString(tMap, "target_phase");
    }

    Object triggerObj = tMap.get("trigger");
    if (triggerObj instanceof Map<?, ?> trMap) {
      trigger = parseTrigger(trMap, path + ".trigger", diagnostics);
    } else {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-006",
              path + ".trigger",
              "Transition must have a trigger object"));
    }

    Object condObj = tMap.get("conditions");
    if (condObj instanceof List<?> condList) {
      for (int i = 0; i < condList.size(); i++) {
        Object cObj = condList.get(i);
        if (cObj instanceof Map<?, ?> cMap) {
          conditions.add(parseCondition(cMap, path + ".conditions[" + i + "]", diagnostics));
        }
      }
    }

    Object actionsObj = tMap.get("actions");
    if (actionsObj instanceof List<?> actionsList) {
      for (int i = 0; i < actionsList.size(); i++) {
        Object aObj = actionsList.get(i);
        if (aObj instanceof Map<?, ?> aMap) {
          actions.add(parseAction(aMap, path + ".actions[" + i + "]", diagnostics));
        }
      }
    }

    return new TransitionSpec(trigger, conditions, targetPhase, actions);
  }

  private TriggerSpec parseTrigger(
      Map<?, ?> map, String path, List<ValidationDiagnostic> diagnostics) {
    String type = getString(map, "type");
    if (type == null) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-007",
              path + ".type",
              "Trigger must have a type"));
    }
    return new TriggerSpec(type, getParameters(map));
  }

  private ConditionSpec parseCondition(
      Map<?, ?> map, String path, List<ValidationDiagnostic> diagnostics) {
    String type = getString(map, "type");
    if (type == null) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-008",
              path + ".type",
              "Condition must have a type"));
    }
    return new ConditionSpec(type, getParameters(map));
  }

  private ActionSpec parseAction(
      Map<?, ?> map, String path, List<ValidationDiagnostic> diagnostics) {
    String type = getString(map, "type");
    if (type == null) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-009",
              path + ".type",
              "Action must have a type"));
    }
    return new ActionSpec(type, getParameters(map));
  }

  private String getString(Map<?, ?> map, String key) {
    Object val = map.get(key);
    return val != null ? String.valueOf(val) : null;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getParameters(Map<?, ?> map) {
    Map<String, Object> result = new HashMap<>();
    Object params = map.get("parameters");
    if (params instanceof Map<?, ?> pMap) {
      for (Map.Entry<?, ?> entry : pMap.entrySet()) {
        result.put(String.valueOf(entry.getKey()), entry.getValue());
      }
    }
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String key = String.valueOf(entry.getKey());
      if (!"type".equals(key) && !"parameters".equals(key)) {
        result.put(key, entry.getValue());
      }
    }
    return result;
  }
}
