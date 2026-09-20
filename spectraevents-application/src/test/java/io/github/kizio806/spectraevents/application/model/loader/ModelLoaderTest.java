package io.github.kizio806.spectraevents.application.model.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelLoaderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void registersAnimationsTogetherWithTheModelDefinition() throws Exception {
    Files.writeString(
        temporaryDirectory.resolve("animated.yml"),
        """
        id: animated_model
        parts:
          core:
            type: item_display
            item: minecraft:stone
        animations:
          hover:
            duration: 1s
            tracks:
              core:
                translation:
                  - at: 0s
                    value: [0.0, 0.0, 0.0]
                  - at: 1s
                    value: [0.0, 1.0, 0.0]
        """);

    ModelDefinitionRegistry modelRegistry = new ModelDefinitionRegistry();
    AnimationDefinitionRegistry animationRegistry = new AnimationDefinitionRegistry();
    ModelLoader loader = new ModelLoader(new ModelCompiler(), modelRegistry, animationRegistry);

    ModelLoader.ModelLoaderResult result = loader.loadDirectory(temporaryDirectory);

    assertEquals(1, result.loadedCount());
    assertEquals(0, result.invalidCount());
    assertTrue(modelRegistry.contains(new ModelId("animated_model")));
    assertTrue(
        animationRegistry
            .find(new ModelId("animated_model"), new AnimationId("hover"))
            .isPresent());
  }
}
