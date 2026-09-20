package io.github.kizio806.spectraevents.application.model.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelResourceExamplesTest {
  private static final List<ModelExample> EXAMPLES =
      List.of(
          new ModelExample("meteor", "dev_meteor_model", "fall"),
          new ModelExample("airdrop", "dev_airdrop_model", "descent"),
          new ModelExample("metin", "dev_metin_model", "pulse"));

  @Test
  void shippedModelsExposeTheAnimationsUsedByEventExamples() throws IOException {
    ModelDefinitionRegistry modelRegistry = new ModelDefinitionRegistry();
    AnimationDefinitionRegistry animationRegistry = new AnimationDefinitionRegistry();
    ModelLoader loader = new ModelLoader(new ModelCompiler(), modelRegistry, animationRegistry);

    for (ModelExample example : EXAMPLES) {
      try (InputStream input = resource("/models/" + example.fileName() + ".yml")) {
        var compiled = loader.parseAndCompileWithAnimations(input, example.fileName() + ".yml");
        modelRegistry.register(compiled.definition());
        compiled
            .animations()
            .forEach(
                (animationId, animation) ->
                    animationRegistry.register(compiled.definition().id(), animation));
      }
    }

    assertEquals(3, modelRegistry.all().size());
    for (ModelExample example : EXAMPLES) {
      assertTrue(
          animationRegistry
              .find(new ModelId(example.modelId()), new AnimationId(example.animationId()))
              .isPresent());
    }
  }

  private InputStream resource(String path) throws IOException {
    InputStream input = getClass().getResourceAsStream(path);
    if (input == null) {
      throw new IOException("Missing model resource " + path);
    }
    return input;
  }

  private record ModelExample(String fileName, String modelId, String animationId) {}
}
