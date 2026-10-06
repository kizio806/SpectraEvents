package io.github.kizio806.spectraevents.application.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.util.Map;
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
