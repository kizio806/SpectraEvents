package io.github.kizio806.spectraevents.application.config.yaml;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompilerException;
import io.github.kizio806.spectraevents.application.config.spec.ActionSpec;
import io.github.kizio806.spectraevents.application.config.spec.ConditionSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import io.github.kizio806.spectraevents.application.config.spec.PhaseSpec;
import io.github.kizio806.spectraevents.application.config.spec.TransitionSpec;
import io.github.kizio806.spectraevents.application.config.spec.TriggerSpec;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
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
  private static final int MAX_PHASES = 128;
  private static final int MAX_TRANSITIONS_PER_PHASE = 128;
  private static final int MAX_ACTIONS_PER_LIST = 128;
  private static final int MAX_CONDITIONS_PER_TRANSITION = 64;

  private static final Set<String> ROOT_KEYS =
      Set.of("schema-version", "id", "initial-phase", "phases");
  private static final Set<String> PHASE_KEYS =
      Set.of("transitions", "on-enter", "onEnter", "on_enter");
  private static final Set<String> TRANSITION_KEYS =
      Set.of(
          "trigger",
          "conditions",
          "actions",
          "target",
          "target-phase",
          "targetPhase",
          "target_phase");

  private final Yaml yaml;

  public EventSpecYamlParser() {
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(false);
    options.setMaxAliasesForCollections(50);
    options.setNestingDepthLimit(64);
    options.setCodePointLimit(1_000_000);
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
      throw failure("Failed to parse YAML", sourceFile, diagnostics);
    }

    if (!(loaded instanceof Map<?, ?> rootMap)) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-002",
              "root",
              "YAML root must be an object/map"));
      throw failure("Failed to parse YAML", sourceFile, diagnostics);
    }

    rejectUnknownKeys(rootMap, ROOT_KEYS, "root", diagnostics);

    String schemaVersion = getString(rootMap, "schema-version", "schema-version", diagnostics);
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

    String id = getString(rootMap, "id", "id", diagnostics);
    String initialPhase = getString(rootMap, "initial-phase", "initial-phase", diagnostics);

    Map<String, PhaseSpec> phases = new HashMap<>();
    Object phasesObj = rootMap.get("phases");
    if (phasesObj instanceof Map<?, ?> phasesMap) {
      rejectOversized(phasesMap.size(), MAX_PHASES, "phases", "phase definitions", diagnostics);
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
    } else if (phasesObj != null) {
      addTypeError("phases", "phases must be an object/map", diagnostics);
    }

    if (!diagnostics.isEmpty()) {
      throw failure("YAML validation failed", sourceFile, diagnostics);
    }

    return new EventSpec(id, schemaVersion, initialPhase, phases);
  }

  private PhaseSpec parsePhase(
      Map<?, ?> phaseMap, String path, List<ValidationDiagnostic> diagnostics) {
    rejectUnknownKeys(phaseMap, PHASE_KEYS, path, diagnostics);
    Set<String> allowedTransitions = new LinkedHashSet<>();
    List<TransitionSpec> transitions = new ArrayList<>();
    List<ActionSpec> onEnter = new ArrayList<>();

    Object transitionsObj = phaseMap.get("transitions");
    if (transitionsObj instanceof List<?> transitionsList) {
      rejectOversized(
          transitionsList.size(),
          MAX_TRANSITIONS_PER_PHASE,
          path + ".transitions",
          "transitions",
          diagnostics);
      for (int i = 0; i < transitionsList.size(); i++) {
        Object tObj = transitionsList.get(i);
        if (tObj instanceof Map<?, ?> tMap) {
          String tPath = path + ".transitions[" + i + "]";
          TransitionSpec spec = parseTransition(tMap, tPath, diagnostics);
          transitions.add(spec);
          if (spec.targetPhase() != null) {
            allowedTransitions.add(spec.targetPhase());
          }
        } else {
          addTypeError(
              path + ".transitions[" + i + "]", "Transition must be an object", diagnostics);
        }
      }
    } else if (transitionsObj != null) {
      addTypeError(path + ".transitions", "transitions must be a list", diagnostics);
    }

    Object onEnterObj = phaseMap.get("onEnter");
    if (onEnterObj == null) {
      onEnterObj = phaseMap.get("on-enter");
    }
    if (onEnterObj == null) {
      onEnterObj = phaseMap.get("on_enter");
    }
    if (onEnterObj instanceof List<?> onEnterList) {
      rejectOversized(
          onEnterList.size(),
          MAX_ACTIONS_PER_LIST,
          path + ".onEnter",
          "on-enter actions",
          diagnostics);
      for (int i = 0; i < onEnterList.size(); i++) {
        Object aObj = onEnterList.get(i);
        if (aObj instanceof Map<?, ?> aMap) {
          onEnter.add(parseAction(aMap, path + ".onEnter[" + i + "]", diagnostics));
        } else {
          diagnostics.add(
              new ValidationDiagnostic(
                  ValidationDiagnostic.Severity.ERROR,
                  "SE-YAML-012",
                  path + ".onEnter[" + i + "]",
                  "Action must be an object"));
        }
      }
    } else if (onEnterObj != null) {
      addTypeError(path + ".onEnter", "on-enter must be a list", diagnostics);
    }

    return new PhaseSpec(allowedTransitions, transitions, onEnter);
  }

  private TransitionSpec parseTransition(
      Map<?, ?> tMap, String path, List<ValidationDiagnostic> diagnostics) {
    rejectUnknownKeys(tMap, TRANSITION_KEYS, path, diagnostics);
    TriggerSpec trigger = null;
    List<ConditionSpec> conditions = new ArrayList<>();
    List<ActionSpec> actions = new ArrayList<>();
    String targetPhase = getString(tMap, "target", path + ".target", diagnostics);
    if (targetPhase == null) {
      targetPhase = getString(tMap, "target-phase", path + ".target-phase", diagnostics);
    }
    if (targetPhase == null) {
      targetPhase = getString(tMap, "targetPhase", path + ".targetPhase", diagnostics);
    }
    if (targetPhase == null) {
      targetPhase = getString(tMap, "target_phase", path + ".target_phase", diagnostics);
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
      rejectOversized(
          condList.size(),
          MAX_CONDITIONS_PER_TRANSITION,
          path + ".conditions",
          "conditions",
          diagnostics);
      for (int i = 0; i < condList.size(); i++) {
        Object cObj = condList.get(i);
        if (cObj instanceof Map<?, ?> cMap) {
          conditions.add(parseCondition(cMap, path + ".conditions[" + i + "]", diagnostics));
        } else {
          addTypeError(path + ".conditions[" + i + "]", "Condition must be an object", diagnostics);
        }
      }
    } else if (condObj != null) {
      addTypeError(path + ".conditions", "conditions must be a list", diagnostics);
    }

    Object actionsObj = tMap.get("actions");
    if (actionsObj instanceof List<?> actionsList) {
      rejectOversized(
          actionsList.size(), MAX_ACTIONS_PER_LIST, path + ".actions", "actions", diagnostics);
      for (int i = 0; i < actionsList.size(); i++) {
        Object aObj = actionsList.get(i);
        if (aObj instanceof Map<?, ?> aMap) {
          actions.add(parseAction(aMap, path + ".actions[" + i + "]", diagnostics));
        } else {
          addTypeError(path + ".actions[" + i + "]", "Action must be an object", diagnostics);
        }
      }
    } else if (actionsObj != null) {
      addTypeError(path + ".actions", "actions must be a list", diagnostics);
    }

    return new TransitionSpec(trigger, conditions, targetPhase, actions);
  }

  private void rejectUnknownKeys(
      Map<?, ?> map, Set<String> allowedKeys, String path, List<ValidationDiagnostic> diagnostics) {
    for (Object rawKey : map.keySet()) {
      String key = String.valueOf(rawKey);
      if (!allowedKeys.contains(key)) {
        diagnostics.add(
            new ValidationDiagnostic(
                ValidationDiagnostic.Severity.ERROR,
                "SE-YAML-010",
                path + "." + key,
                "Unknown field '" + key + "'"));
      }
    }
  }

  private void addTypeError(String path, String message, List<ValidationDiagnostic> diagnostics) {
    diagnostics.add(
        new ValidationDiagnostic(
            ValidationDiagnostic.Severity.ERROR, "SE-YAML-011", path, message));
  }

  private void rejectOversized(
      int actual,
      int maximum,
      String path,
      String description,
      List<ValidationDiagnostic> diagnostics) {
    if (actual > maximum) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-013",
              path,
              description + " exceeds limit " + maximum + " (got " + actual + ")"));
    }
  }

  private EventDefinitionCompilerException failure(
      String message, String sourceFile, List<ValidationDiagnostic> diagnostics) {
    List<ValidationDiagnostic> sourced =
        diagnostics.stream()
            .map(
                diagnostic ->
                    new ValidationDiagnostic(
                        diagnostic.severity(),
                        diagnostic.code(),
                        sourceFile + ":" + diagnostic.path(),
                        diagnostic.message()))
            .toList();
    return new EventDefinitionCompilerException(message, sourced);
  }

  private TriggerSpec parseTrigger(
      Map<?, ?> map, String path, List<ValidationDiagnostic> diagnostics) {
    String type = getString(map, "type", path + ".type", diagnostics);
    if (type == null) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-007",
              path + ".type",
              "Trigger must have a type"));
    }
    return new TriggerSpec(type, getParameters(map, path, diagnostics));
  }

  private ConditionSpec parseCondition(
      Map<?, ?> map, String path, List<ValidationDiagnostic> diagnostics) {
    String type = getString(map, "type", path + ".type", diagnostics);
    if (type == null) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-008",
              path + ".type",
              "Condition must have a type"));
    }
    return new ConditionSpec(type, getParameters(map, path, diagnostics));
  }

  private ActionSpec parseAction(
      Map<?, ?> map, String path, List<ValidationDiagnostic> diagnostics) {
    String type = getString(map, "type", path + ".type", diagnostics);
    if (type == null) {
      diagnostics.add(
          new ValidationDiagnostic(
              ValidationDiagnostic.Severity.ERROR,
              "SE-YAML-009",
              path + ".type",
              "Action must have a type"));
    }
    return new ActionSpec(type, getParameters(map, path, diagnostics));
  }

  private String getString(
      Map<?, ?> map, String key, String path, List<ValidationDiagnostic> diagnostics) {
    if (!map.containsKey(key)) {
      return null;
    }
    Object val = map.get(key);
    if (val == null) {
      return null;
    }
    if (val instanceof Map<?, ?> || val instanceof List<?>) {
      addTypeError(path, "Value must be a scalar", diagnostics);
      return null;
    }
    return String.valueOf(val);
  }

  private Map<String, Object> getParameters(
      Map<?, ?> map, String path, List<ValidationDiagnostic> diagnostics) {
    Map<String, Object> result = new HashMap<>();
    Object params = map.get("parameters");
    if (params instanceof Map<?, ?> pMap) {
      for (Map.Entry<?, ?> entry : pMap.entrySet()) {
        result.put(String.valueOf(entry.getKey()), entry.getValue());
      }
    } else if (params != null) {
      addTypeError(path + ".parameters", "parameters must be an object/map", diagnostics);
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
