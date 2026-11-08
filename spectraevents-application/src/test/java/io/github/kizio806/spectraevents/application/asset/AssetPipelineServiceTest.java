package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetNode;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AssetPipelineServiceTest {

  private AssetPipelineService service;

  @TempDir Path tempDir;

  private static final String TEST_MODEL = "test.spectra.zip";

  @BeforeEach
  void setUp() {
    SpectraAssetNode node =
        new SpectraAssetNode(
            "bone1",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(),
            List.of());
    AssetImportPort mockImport =
        source -> new SpectraAssetDocument(1, "test", Map.of(), List.of(node), Map.of());
    ResourcePackBuilder mockBuilder = new ResourcePackBuilder(tempDir.resolve("out"));

    service =
        new AssetPipelineService(mockImport, mockBuilder, tempDir, AssetTargetProfile.PROFILE_26_1);
  }

  @Test
  void testImportFileSuccess() throws Exception {
    Path dummyFile = tempDir.resolve(TEST_MODEL);
    Files.writeString(dummyFile, "bundle");

    service.importFile(TEST_MODEL);

    Assertions.assertTrue(service.listModels().contains("test"));
    Assertions.assertNotNull(service.getModelInfo("test"));
    Assertions.assertTrue(
        Files.isRegularFile(tempDir.resolve("out").resolve("spectraevents-profile_26_1.zip")));
  }

  @Test
  void testCleanRemovesCache() throws Exception {
    Path dummyFile = tempDir.resolve(TEST_MODEL);
    Files.writeString(dummyFile, "bundle");

    service.importFile(TEST_MODEL);
    service.clean();

    Assertions.assertTrue(service.listModels().isEmpty());
  }

  @Test
  void importsFileAsynchronously() throws Exception {
    Files.writeString(tempDir.resolve(TEST_MODEL), "bundle");

    service.importFileAsync(TEST_MODEL).join();

    Assertions.assertTrue(service.listModels().contains("test"));
  }

  @Test
  void testBuildAssetsSuccess() {
    service.buildAssets();
    Assertions.assertTrue(service.isReady());
    Assertions.assertFalse(service.isBuilding());
    Assertions.assertNull(service.lastFailure());
    Assertions.assertEquals(AssetTargetProfile.PROFILE_26_1, service.targetProfile());
  }

  @Test
  void testBuildAssetsAsync() {
    service.buildAssetsAsync().join();
    Assertions.assertTrue(service.isReady());
  }

  @Test
  void testValidateModel() throws Exception {
    Path dummyFile = tempDir.resolve(TEST_MODEL);
    Files.writeString(dummyFile, "bundle");
    service.importFile(TEST_MODEL);

    Assertions.assertTrue(service.validateModel("test"));
    Assertions.assertFalse(service.validateModel("nonexistent"));
  }

  @Test
  void testValidateModelReturnsFalseForEmptyNodes() throws Exception {
    AssetImportPort emptyImport =
        source -> new SpectraAssetDocument(1, "empty-model", Map.of(), List.of(), Map.of());
    ResourcePackBuilder mockBuilder = new ResourcePackBuilder(tempDir.resolve("out-empty"));
    AssetPipelineService emptyService =
        new AssetPipelineService(
            emptyImport, mockBuilder, tempDir, AssetTargetProfile.PROFILE_26_1);

    String name = "empty.spectra.zip";
    Files.writeString(tempDir.resolve(name), "empty");
    emptyService.importFile(name);

    Assertions.assertFalse(emptyService.validateModel("empty-model"));
  }

  @Test
  void testImportFileSecurityExceptionOnTraversal() {
    Assertions.assertThrows(SecurityException.class, () -> service.importFile("../escaped.zip"));
    Assertions.assertThrows(
        SecurityException.class, () -> service.importFile("sub/../../escaped.zip"));
  }

  @Test
  void testImportFileRejectsInvalidFileFormat() throws Exception {
    String invalidName = "invalid.txt";
    Files.writeString(tempDir.resolve(invalidName), "hello");
    Assertions.assertThrows(IllegalArgumentException.class, () -> service.importFile(invalidName));
  }

  @Test
  void testImportFileRejectsNonExistentFile() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> service.importFile("missing.spectra.zip"));
  }

  @Test
  void testImportFileNullFilename() {
    Assertions.assertThrows(NullPointerException.class, () -> service.importFile(null));
  }

  @Test
  void testImportFileDuplicateSkipsRebuilding() throws Exception {
    Path dummyFile = tempDir.resolve(TEST_MODEL);
    Files.writeString(dummyFile, "bundle-content");

    service.importFile(TEST_MODEL);
    Assertions.assertTrue(service.listModels().contains("test"));

    // Second import with identical hash should skip without error
    service.importFile(TEST_MODEL);
    Assertions.assertTrue(service.listModels().contains("test"));
  }

  @Test
  void testImportFileThrowsWhenImportPortFails() throws Exception {
    AssetImportPort failingImport =
        source -> {
          throw new java.io.IOException("Disk read error");
        };
    ResourcePackBuilder mockBuilder = new ResourcePackBuilder(tempDir.resolve("out-fail"));
    AssetPipelineService failingService =
        new AssetPipelineService(
            failingImport, mockBuilder, tempDir, AssetTargetProfile.PROFILE_26_1);

    String name = "failing.spectra.zip";
    Files.writeString(tempDir.resolve(name), "broken");
    RuntimeException error =
        Assertions.assertThrows(RuntimeException.class, () -> failingService.importFile(name));
    Assertions.assertTrue(error.getMessage().contains("Import failed"));
  }

  @Test
  void testBuildAssetsWithNonExistentSourceDirectory() {
    Path missingDir = tempDir.resolve("missing_src_dir");
    AssetPipelineService nonExistentService =
        new AssetPipelineService(
            source -> null,
            new ResourcePackBuilder(tempDir.resolve("out_non_existent")),
            missingDir,
            AssetTargetProfile.PROFILE_26_1);

    nonExistentService.buildAssets();
    Assertions.assertTrue(nonExistentService.isReady());
    Assertions.assertNull(nonExistentService.lastFailure());
  }

  @Test
  void testBuildAssetsThrowsWhenImportFails() throws Exception {
    AssetImportPort failingImport =
        source -> {
          throw new java.io.IOException("File corrupted");
        };
    ResourcePackBuilder mockBuilder = new ResourcePackBuilder(tempDir.resolve("out-err"));
    Path srcDir = tempDir.resolve("sources_err");
    Files.createDirectories(srcDir);
    Files.writeString(srcDir.resolve("model1.spectra.zip"), "corrupted-content");

    AssetPipelineService failingService =
        new AssetPipelineService(
            failingImport, mockBuilder, srcDir, AssetTargetProfile.PROFILE_26_1);

    IllegalStateException ex =
        Assertions.assertThrows(IllegalStateException.class, failingService::buildAssets);
    Assertions.assertTrue(ex.getMessage().contains("Asset pipeline rejected bundle"));
    Assertions.assertFalse(failingService.isReady());
    Assertions.assertNotNull(failingService.lastFailure());
  }

  @Test
  void testImportWithModelRegistrarAndBbmodelExtension() throws Exception {
    io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler modelCompiler =
        new io.github.kizio806.spectraevents.application.model.compiler.ModelCompiler();
    io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry
        modelRegistry =
            new io.github.kizio806.spectraevents.application.model.registry
                .ModelDefinitionRegistry();
    io.github.kizio806.spectraevents.application.model.animation.registry
            .AnimationDefinitionRegistry
        animRegistry =
            new io.github.kizio806.spectraevents.application.model.animation.registry
                .AnimationDefinitionRegistry();
    ImportedAssetModelRegistrar mockRegistrar =
        new ImportedAssetModelRegistrar(modelCompiler, modelRegistry, animRegistry);

    SpectraAssetNode node =
        new SpectraAssetNode(
            "bone1",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(),
            List.of());
    AssetImportPort mockImport =
        source -> new SpectraAssetDocument(1, "bbmodel-test", Map.of(), List.of(node), Map.of());
    ResourcePackBuilder mockBuilder = new ResourcePackBuilder(tempDir.resolve("out-reg"));

    AssetPipelineService registrarService =
        new AssetPipelineService(
            mockImport, mockBuilder, tempDir, AssetTargetProfile.PROFILE_26_1, mockRegistrar);

    String bbmodelName = "custom.bbmodel";
    Files.writeString(tempDir.resolve(bbmodelName), "bbmodel-data");

    registrarService.importFile(bbmodelName);
    Assertions.assertTrue(
        modelRegistry.contains(
            new io.github.kizio806.spectraevents.core.visual.model.ModelId("bbmodel-test")));
    Assertions.assertTrue(registrarService.listModels().contains("bbmodel-test"));
  }
}
