package io.github.kizio806.spectraevents.adapter.blockbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.ImportedAssetModelRegistrar;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.loader.FileSystemDefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.spec.ActionSpec;
import io.github.kizio806.spectraevents.application.config.spec.EventSpec;
import io.github.kizio806.spectraevents.application.config.spec.PhaseSpec;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundledReferenceAssetsTest {
  private static final Map<String, String> EVENT_MODELS =
      Map.of(
          "meteor", "meteor_core",
          "airdrop", "airdrop_crate",
          "metin", "metin_stone",
          "pinata", "pinata",
          "boss_portal", "boss_portal");

  @TempDir Path dataDirectory;

  @Test
  void parsesAndRegistersEveryShippedBlockbenchAssetForEveryBundledEvent() throws Exception {
    ModelDefinitionRegistry modelRegistry = new ModelDefinitionRegistry();
    AnimationDefinitionRegistry animationRegistry = new AnimationDefinitionRegistry();
    ImportedAssetModelRegistrar registrar =
        new ImportedAssetModelRegistrar(new ModelCompiler(), modelRegistry, animationRegistry);
    BlockbenchProjectReader reader = new BlockbenchProjectReader();

    for (String modelId : EVENT_MODELS.values()) {
      var document = reader.read(extractBundledAsset(modelId + ".bbmodel"));
      assertEquals(modelId, document.modelId());
      assertTrue(document.textures().size() > 0);
      assertTrue(document.nodes().size() > 0);
      assertTrue(document.animations().size() > 0);
      registrar.register(document);
      assertEquals(
          1,
          modelRegistry.get(new ModelId(modelId)).orElseThrow().interactions().size(),
          "Every imported Blockbench model needs a default interaction hitbox");
    }

    FileSystemDefinitionLoader definitionLoader =
        new FileSystemDefinitionLoader(
            dataDirectory,
            new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry()));
    definitionLoader.ensureDefaultConfiguration();
    try (Stream<Path> presets = Files.list(definitionLoader.presetsDirectory())) {
      for (Path preset : presets.toList()) {
        Files.copy(preset, definitionLoader.eventsDirectory().resolve(preset.getFileName()));
      }
    }
    var result = definitionLoader.loadFromDisk();

    assertEquals(EVENT_MODELS.size(), result.loaded().size(), result.failures()::toString);
    assertTrue(result.failures().isEmpty(), result.failures()::toString);
    for (var registered : result.loaded()) {
      EventSpec event = registered.sourceSpec();
      assertNotNull(event);
      String modelId = EVENT_MODELS.get(event.id());
      assertNotNull(modelId, () -> "Unexpected bundled event " + event.id());
      ModelId model = new ModelId(modelId);
      assertTrue(modelRegistry.contains(model), () -> "Missing model " + modelId);
      assertEquals(
          List.of(modelId),
          actions(event)
              .filter(action -> "spawn_model".equals(action.type()))
              .map(action -> String.valueOf(action.parameters().get("model")))
              .toList());
      actions(event)
          .filter(action -> "play_animation".equals(action.type()))
          .map(action -> String.valueOf(action.parameters().get("animation")))
          .forEach(
              animation ->
                  assertTrue(
                      animationRegistry.find(model, new AnimationId(animation)).isPresent(),
                      () -> modelId + " is missing animation " + animation));
    }
  }

  private Path extractBundledAsset(String fileName) throws Exception {
    Path target = dataDirectory.resolve(fileName);
    try (InputStream resource = getClass().getResourceAsStream("/assets/source/" + fileName)) {
      assertNotNull(resource, () -> "Missing bundled asset " + fileName);
      Files.copy(resource, target);
    }
    return target;
  }

  private static Stream<ActionSpec> actions(EventSpec event) {
    return event.phases().values().stream().flatMap(BundledReferenceAssetsTest::actions);
  }

  private static Stream<ActionSpec> actions(PhaseSpec phase) {
    return Stream.concat(
        collection(phase.onEnter()).stream(),
        collection(phase.transitions()).stream()
            .flatMap(transition -> collection(transition.actions()).stream()));
  }

  private static <T> Collection<T> collection(Collection<T> values) {
    return values == null ? List.of() : values;
  }
}
