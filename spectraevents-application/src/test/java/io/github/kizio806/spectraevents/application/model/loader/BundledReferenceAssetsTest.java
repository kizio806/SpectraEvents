package io.github.kizio806.spectraevents.application.model.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.GeneratedAssetItem;
import io.github.kizio806.spectraevents.application.asset.ImportedAssetModelRegistrar;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.loader.FileSystemDefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetNode;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundledReferenceAssetsTest {
  private static final List<String> REFERENCE_FILES =
      List.of("meteor.yml", "airdrop.yml", "metin.yml", "pinata.yml", "boss-portal.yml");

  @TempDir Path dataDirectory;

  @Test
  void extractsAndLoadsEveryBundledReferenceEventAndModel() throws Exception {
    FileSystemDefinitionLoader definitionLoader =
        new FileSystemDefinitionLoader(
            dataDirectory,
            new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry()));
    var result = definitionLoader.loadFromDisk();

    assertEquals(5, result.loaded().size(), result.failures()::toString);
    assertTrue(result.failures().isEmpty(), result.failures()::toString);

    ModelLoader modelLoader =
        new ModelLoader(
            new ModelCompiler(), new ModelDefinitionRegistry(), new AnimationDefinitionRegistry());
    FileSystemModelLoader fileSystemModelLoader =
        new FileSystemModelLoader(dataDirectory, modelLoader);
    ModelLoader.ModelLoaderResult modelResult = fileSystemModelLoader.loadFromDisk();

    assertEquals(5, modelResult.loadedCount(), modelResult.diagnostics()::toString);
    assertEquals(0, modelResult.invalidCount(), modelResult.diagnostics()::toString);
    for (String fileName : REFERENCE_FILES) {
      assertTrue(Files.isRegularFile(definitionLoader.eventsDirectory().resolve(fileName)));
      assertTrue(Files.isRegularFile(fileSystemModelLoader.modelsDirectory().resolve(fileName)));
    }
  }

  @Test
  void importedAssetReplacesNativeFallbackWithTheSameModelId() throws Exception {
    ModelDefinitionRegistry modelRegistry = new ModelDefinitionRegistry();
    AnimationDefinitionRegistry animationRegistry = new AnimationDefinitionRegistry();
    FileSystemModelLoader fileSystemModelLoader =
        new FileSystemModelLoader(
            dataDirectory, new ModelLoader(new ModelCompiler(), modelRegistry, animationRegistry));
    fileSystemModelLoader.loadFromDisk();

    new ImportedAssetModelRegistrar(new ModelCompiler(), modelRegistry, animationRegistry)
        .register(
            new SpectraAssetDocument(
                1,
                "meteor_core",
                Map.of(),
                List.of(
                    new SpectraAssetNode(
                        "imported_root",
                        Vector3.ZERO,
                        Vector3.ZERO,
                        EulerRotation.ZERO,
                        Vector3.ONE,
                        List.of(),
                        List.of())),
                Map.of()));

    var definition = modelRegistry.get(new ModelId("meteor_core")).orElseThrow();
    assertEquals(1, definition.parts().size());
    ItemAssetRef visual = (ItemAssetRef) definition.parts().getFirst().visualAsset();
    assertEquals(GeneratedAssetItem.reference("meteor_core", "imported_root"), visual.itemRef());
  }
}
