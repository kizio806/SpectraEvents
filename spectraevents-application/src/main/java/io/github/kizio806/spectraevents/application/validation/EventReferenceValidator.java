package io.github.kizio806.spectraevents.application.validation;

import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.spec.ActionSpec;
import io.github.kizio806.spectraevents.application.config.spec.PhaseSpec;
import io.github.kizio806.spectraevents.application.config.spec.TransitionSpec;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Validates model and animation references after the asset pipeline has registered them. */
public final class EventReferenceValidator {
  private final ModelDefinitionRegistry models;
  private final AnimationDefinitionRegistry animations;

  public EventReferenceValidator(
      ModelDefinitionRegistry models, AnimationDefinitionRegistry animations) {
    this.models = Objects.requireNonNull(models, "models");
    this.animations = Objects.requireNonNull(animations, "animations");
  }

  public List<ValidationDiagnostic> validate(DefinitionLoadResult definitions) {
    Objects.requireNonNull(definitions, "definitions");
    List<ValidationDiagnostic> diagnostics = new ArrayList<>();
    for (var definition : definitions.loaded()) {
      List<ActionAtPath> actions = actions(definition.sourceSpec().phases());
      Set<String> referencedModels = new LinkedHashSet<>();
      for (ActionAtPath action : actions) {
        if ("spawn_model".equals(action.action().type())) {
          String model = string(action.action().parameters(), "model");
          if (model == null) {
            diagnostics.add(error(action.path(), "SE-REF-MODEL-001", "spawn_model requires model"));
          } else {
            referencedModels.add(model);
            validateModel(model, action.path(), diagnostics);
          }
        }
      }
      for (ActionAtPath action : actions) {
        if ("play_animation".equals(action.action().type())) {
          validateAnimation(action, referencedModels, diagnostics);
        }
      }
    }
    return List.copyOf(diagnostics);
  }

  private void validateAnimation(
      ActionAtPath action, Set<String> referencedModels, List<ValidationDiagnostic> diagnostics) {
    String animation = string(action.action().parameters(), "animation");
    if (animation == null) {
      diagnostics.add(
          error(action.path(), "SE-REF-ANIMATION-001", "play_animation requires animation"));
      return;
    }
    String model = string(action.action().parameters(), "model");
    if (model == null && referencedModels.size() == 1) {
      model = referencedModels.iterator().next();
    }
    if (model == null) {
      diagnostics.add(
          error(
              action.path(),
              "SE-REF-ANIMATION-002",
              "play_animation requires model when the definition spawns zero or multiple models"));
      return;
    }
    try {
      ModelId modelId = new ModelId(model);
      if (!models.contains(modelId)) {
        diagnostics.add(
            error(action.path(), "SE-REF-ANIMATION-003", "Unknown model \"" + model + "\""));
        return;
      }
      if (animations.find(modelId, new AnimationId(animation)).isEmpty()) {
        String available =
            animations.forModel(modelId).stream()
                .map(value -> value.definition().id().value())
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        diagnostics.add(
            error(
                action.path(),
                "SE-REF-ANIMATION-004",
                "Unknown animation \""
                    + animation
                    + "\" for model \""
                    + model
                    + "\". Available: "
                    + available));
      }
    } catch (IllegalArgumentException exception) {
      diagnostics.add(error(action.path(), "SE-REF-ANIMATION-005", exception.getMessage()));
    }
  }

  private void validateModel(String model, String path, List<ValidationDiagnostic> diagnostics) {
    try {
      if (!models.contains(new ModelId(model))) {
        diagnostics.add(error(path, "SE-REF-MODEL-002", "Unknown model \"" + model + "\""));
      }
    } catch (IllegalArgumentException exception) {
      diagnostics.add(error(path, "SE-REF-MODEL-003", exception.getMessage()));
    }
  }

  private static List<ActionAtPath> actions(Map<String, PhaseSpec> phases) {
    List<ActionAtPath> result = new ArrayList<>();
    for (Map.Entry<String, PhaseSpec> phase : phases.entrySet()) {
      String prefix = "phases." + phase.getKey();
      add(result, phase.getValue().onEnter(), prefix + ".on-enter");
      List<TransitionSpec> transitions = phase.getValue().transitions();
      for (int index = 0; index < transitions.size(); index++) {
        add(
            result,
            transitions.get(index).actions(),
            prefix + ".transitions[" + index + "].actions");
      }
    }
    return result;
  }

  private static void add(List<ActionAtPath> target, List<ActionSpec> actions, String prefix) {
    for (int index = 0; index < actions.size(); index++) {
      target.add(new ActionAtPath(actions.get(index), prefix + "[" + index + "]"));
    }
  }

  private static String string(Map<String, Object> values, String key) {
    Object value = values.get(key);
    return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
  }

  private static ValidationDiagnostic error(String path, String code, String message) {
    return new ValidationDiagnostic(ValidationDiagnostic.Severity.ERROR, code, path, message);
  }

  private record ActionAtPath(ActionSpec action, String path) {}
}
