package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AssetPipelineServiceTest {

  private AssetPipelineService service;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    AssetImportPort mockImport =
        new AssetImportPort() {
          @Override
          public SpectraAssetDocument read(String content, String modelId) {
            return new SpectraAssetDocument(
                1, modelId, new HashMap<>(), new ArrayList<>(), new HashMap<>());
          }
        };
    ResourcePackBuilder mockBuilder = new ResourcePackBuilder(tempDir.resolve("out"));

    service =
        new AssetPipelineService(
            mockImport, null, null, mockBuilder, tempDir, AssetTargetProfile.PROFILE_26_1);
  }

  @Test
  void testImportFileSuccess() throws Exception {
    Path dummyFile = tempDir.resolve("test.bbmodel");
    Files.writeString(dummyFile, "{}");

    service.importFile("test.bbmodel");

    Assertions.assertTrue(service.listModels().contains("test"));
    Assertions.assertNotNull(service.getModelInfo("test"));
  }

  @Test
  void testCleanRemovesCache() throws Exception {
    Path dummyFile = tempDir.resolve("test.bbmodel");
    Files.writeString(dummyFile, "{}");

    service.importFile("test.bbmodel");
    service.clean();

    Assertions.assertTrue(service.listModels().isEmpty());
  }
}
