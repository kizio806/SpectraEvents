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
}
