package io.github.kizio806.spectraevents.application.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.application.config.spec.ActionSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import io.github.kizio806.spectraevents.application.config.spec.PhaseSpec;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDefinition;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDuration;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.animation.RecoveryPolicy;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventReferenceValidatorTest {
  @Test
  void reportsTheMissingModelWithoutThrowingAnImplementationException() {
    var definitions =
        new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry())
            .validate(Map.of("events/example.yml", definition("missing_model", "idle")));

    var diagnostics =
        new EventReferenceValidator(
                new ModelDefinitionRegistry(), new AnimationDefinitionRegistry())
            .validate(definitions);

    assertTrue(definitions.failures().isEmpty(), definitions.failures()::toString);

    assertTrue(
        diagnostics.stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code().equals("SE-REF-MODEL-002")
                        && diagnostic.message().contains("missing_model")));
  }

  @Test
  void reportsTheMissingAnimationAndItsOwningModel() {
    ModelDefinitionRegistry models =
        new ModelDefinitionRegistry() {
          @Override
          public boolean contains(ModelId modelId) {
            return modelId.value().equals("metin_stone");
          }
        };
    var definitions =
        new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry())
            .validate(Map.of("events/metin.yml", definition("metin_stone", "attack2")));

    var diagnostics =
        new EventReferenceValidator(models, new AnimationDefinitionRegistry())
            .validate(definitions);

    assertTrue(definitions.failures().isEmpty(), definitions.failures()::toString);

    assertTrue(
        diagnostics.stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code().equals("SE-REF-ANIMATION-004")
                        && diagnostic.message().contains("attack2")
                        && diagnostic.message().contains("metin_stone")));
  }

  @Test
  void reportsMissingModelAndMissingAnimationParameters() {
    ActionSpec spawnNoModel = new ActionSpec("spawn_model", Map.of());
    ActionSpec playNoAnim = new ActionSpec("play_animation", Map.of());
    PhaseSpec phase = new PhaseSpec(Set.of(), List.of(), List.of(spawnNoModel, playNoAnim));
    EventSpec spec = new EventSpec("missing", "1", "start", Map.of("start", phase));
    io.github.kizio806.spectraevents.core.event.phase.PhaseId phaseId =
        new io.github.kizio806.spectraevents.core.event.phase.PhaseId("start");
    io.github.kizio806.spectraevents.core.event.definition.EventDefinition dummyDef =
        new io.github.kizio806.spectraevents.core.event.definition.EventDefinition(
            new io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId("missing"),
            phaseId,
            Map.of(
                phaseId,
                new io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition(
                    phaseId, Set.of())));
    RegisteredEventDefinition reg =
        new RegisteredEventDefinition(dummyDef, "events/missing.yml", spec);
    var definitions = new DefinitionLoadResult(List.of(reg), List.of());

    var diagnostics =
        new EventReferenceValidator(
                new ModelDefinitionRegistry(), new AnimationDefinitionRegistry())
            .validate(definitions);

    assertTrue(
        diagnostics.stream().anyMatch(d -> d.code().equals("SE-REF-MODEL-001")),
        diagnostics::toString);
    assertTrue(
        diagnostics.stream().anyMatch(d -> d.code().equals("SE-REF-ANIMATION-001")),
        diagnostics::toString);
  }

  @Test
  void reportsInvalidModelAndAnimationIds() {
    ActionSpec spawnInvalid = new ActionSpec("spawn_model", Map.of("model", "throws_model"));
    ActionSpec playInvalid =
        new ActionSpec(
            "play_animation", Map.of("model", "metin_stone", "animation", "throws_anim"));
    PhaseSpec phase = new PhaseSpec(Set.of(), List.of(), List.of(spawnInvalid, playInvalid));
    EventSpec spec = new EventSpec("invalid_ids", "1", "start", Map.of("start", phase));
    io.github.kizio806.spectraevents.core.event.phase.PhaseId phaseId =
        new io.github.kizio806.spectraevents.core.event.phase.PhaseId("start");
    io.github.kizio806.spectraevents.core.event.definition.EventDefinition dummyDef =
        new io.github.kizio806.spectraevents.core.event.definition.EventDefinition(
            new io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId(
                "invalid_ids"),
            phaseId,
            Map.of(
                phaseId,
                new io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition(
                    phaseId, Set.of())));
    RegisteredEventDefinition reg =
        new RegisteredEventDefinition(dummyDef, "events/invalid.yml", spec);
    var definitions = new DefinitionLoadResult(List.of(reg), List.of());

    ModelDefinitionRegistry models =
        new ModelDefinitionRegistry() {
          @Override
          public boolean contains(ModelId modelId) {
            if ("throws_model".equals(modelId.value())) {
              throw new IllegalArgumentException("Invalid model identifier");
            }
            return modelId.value().equals("metin_stone");
          }
        };

    AnimationDefinitionRegistry animations =
        new AnimationDefinitionRegistry() {
          @Override
          public java.util.Optional<CompiledAnimation> find(
              ModelId modelId, AnimationId animationId) {
            if ("throws_anim".equals(animationId.value())) {
              throw new IllegalArgumentException("Invalid animation identifier");
            }
            return java.util.Optional.empty();
          }
        };

    var diagnostics = new EventReferenceValidator(models, animations).validate(definitions);

    assertTrue(
        diagnostics.stream().anyMatch(d -> d.code().equals("SE-REF-MODEL-003")),
        diagnostics::toString);
    assertTrue(
        diagnostics.stream().anyMatch(d -> d.code().equals("SE-REF-ANIMATION-005")),
        diagnostics::toString);
  }

  @Test
  void reportsPlayAnimationWithoutModelWhenMultipleOrZeroModels() {
    String yaml =
        """
        schema-version: 1
        id: multi_models
        initial-phase: start
        phases:
          start:
            on-enter:
              - type: spawn_model
                model: model_a
              - type: spawn_model
                model: model_b
              - type: play_animation
                animation: attack
        """;
    var definitions =
        new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry())
            .validate(Map.of("events/multi.yml", yaml));

    var diagnostics =
        new EventReferenceValidator(
                new ModelDefinitionRegistry(), new AnimationDefinitionRegistry())
            .validate(definitions);

    assertTrue(
        diagnostics.stream().anyMatch(d -> d.code().equals("SE-REF-ANIMATION-002")),
        diagnostics::toString);
  }

  @Test
  void reportsUnknownModelForPlayAnimation() {
    String yaml =
        """
        schema-version: 1
        id: unknown_anim_model
        initial-phase: start
        phases:
          start:
            on-enter:
              - type: play_animation
                model: unknown_model
                animation: attack
        """;
    var definitions =
        new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry())
            .validate(Map.of("events/unknown.yml", yaml));

    var diagnostics =
        new EventReferenceValidator(
                new ModelDefinitionRegistry(), new AnimationDefinitionRegistry())
            .validate(definitions);

    assertTrue(
        diagnostics.stream().anyMatch(d -> d.code().equals("SE-REF-ANIMATION-003")),
        diagnostics::toString);
  }

  @Test
  void reportsAvailableAnimationsWhenFound() {
    ModelDefinitionRegistry models =
        new ModelDefinitionRegistry() {
          @Override
          public boolean contains(ModelId modelId) {
            return modelId.value().equals("metin_stone");
          }
        };

    AnimationDefinitionRegistry animations = new AnimationDefinitionRegistry();
    ModelId modelId = new ModelId("metin_stone");
    animations.register(
        modelId,
        new CompiledAnimation(
            new AnimationDefinition(
                new AnimationId("idle"),
                AnimationDuration.fromSeconds(1.0),
                LoopMode.ONCE,
                1,
                Map.of(),
                List.of(),
                RecoveryPolicy.STOP),
            List.of()));
    animations.register(
        modelId,
        new CompiledAnimation(
            new AnimationDefinition(
                new AnimationId("walk"),
                AnimationDuration.fromSeconds(1.0),
                LoopMode.ONCE,
                1,
                Map.of(),
                List.of(),
                RecoveryPolicy.STOP),
            List.of()));

    var definitions =
        new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry())
            .validate(Map.of("events/metin.yml", definition("metin_stone", "attack_unknown")));

    var diagnostics = new EventReferenceValidator(models, animations).validate(definitions);

    assertTrue(
        diagnostics.stream()
            .anyMatch(
                d -> d.code().equals("SE-REF-ANIMATION-004") && d.message().contains("idle, walk")),
        diagnostics::toString);
  }

  @Test
  void validatesActionsInTransitions() {
    String yaml =
        """
        schema-version: 1
        id: trans_test
        initial-phase: start
        phases:
          start:
            transitions:
              - trigger:
                  type: timer
                  duration: 5s
                actions:
                  - type: spawn_model
                    model: trans_model
                  - type: play_animation
                    model: trans_model
                    animation: trans_anim
        """;
    var definitions =
        new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry())
            .validate(Map.of("events/trans.yml", yaml));

    var diagnostics =
        new EventReferenceValidator(
                new ModelDefinitionRegistry(), new AnimationDefinitionRegistry())
            .validate(definitions);

    assertTrue(
        diagnostics.stream()
            .anyMatch(
                d ->
                    d.path().contains("transitions[0].actions")
                        && d.code().equals("SE-REF-MODEL-002")),
        diagnostics::toString);
  }

  private static String definition(String model, String animation) {
    String definition =
        """
        schema-version: 1
        id: reference_test
        initial-phase: start
        phases:
          start:
            on-enter:
              - type: spawn_model
                model: {model}
              - type: play_animation
                animation: {animation}
        """;
    return definition.replace("{model}", model).replace("{animation}", animation);
  }
}
