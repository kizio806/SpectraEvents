package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SecurityAssetPipelineTest {

  private AssetPipelineService service;

  @TempDir Path sourceDir;
  @TempDir Path outDir;

  @BeforeEach
  void setUp() {
    AssetImportPort mockImport =
        source ->
            new SpectraAssetDocument(
                1,
                "test",
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyList(),
                java.util.Collections.emptyMap());
    ResourcePackBuilder mockBuilder = new ResourcePackBuilder(outDir);

    service =
        new AssetPipelineService(
            mockImport, mockBuilder, sourceDir, AssetTargetProfile.PROFILE_26_1);
  }

  @Test
  void testPathTraversalRejected() {
    Exception ex =
        Assertions.assertThrows(
            SecurityException.class,
            () -> {
              service.importFile("../../etc/passwd");
            });
    Assertions.assertTrue(ex.getMessage().contains("Path traversal attempt"));
  }

  @Test
  void testAbsolutePathRejected() {
    Exception ex =
        Assertions.assertThrows(
            SecurityException.class,
            () -> {
              service.importFile("/root/texture.png");
            });
    Assertions.assertTrue(ex.getMessage().contains("Path traversal attempt"));
  }

  @Test
  void testLooseBlockbenchFileRejected() {
    Exception ex =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> {
              service.importFile("model.bbmodel");
            });
    Assertions.assertTrue(ex.getMessage().contains("invalid format"));
  }
}
