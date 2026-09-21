package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.nio.file.Files;
import java.nio.file.Path;
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
    AssetImportPort mockImport =
        source ->
            new SpectraAssetDocument(
                1, "test", java.util.Map.of(), java.util.List.of(), java.util.Map.of());
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
}
