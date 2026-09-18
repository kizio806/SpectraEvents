package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
        (content, id) ->
            new SpectraAssetDocument(
                1,
                id,
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyList(),
                java.util.Collections.emptyMap());
    ResourcePackBuilder mockBuilder = new ResourcePackBuilder(outDir);

    service =
        new AssetPipelineService(
            mockImport, null, null, mockBuilder, sourceDir, AssetTargetProfile.PROFILE_26_1);
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
  void testZipSlipRejected() throws Exception {
    Path zipPath = sourceDir.resolve("malicious.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
      zos.putNextEntry(new ZipEntry("../../../malicious.bbmodel"));
      zos.write("{}".getBytes());
      zos.closeEntry();
    }

    Exception ex =
        Assertions.assertThrows(
            RuntimeException.class,
            () -> {
              service.importFile("malicious.spectra.zip");
            });
    Assertions.assertTrue(ex.getMessage().contains("ZIP slip detected"));
  }

  @Test
  void testDecompressionLimitEnforced() throws Exception {
    Path zipPath = sourceDir.resolve("bomb.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
      zos.putNextEntry(new ZipEntry("bomb.bbmodel"));
      byte[] chunk = new byte[100000];
      // Write ~5.1MB which exceeds 5MB limit
      for (int i = 0; i < 52; i++) {
        zos.write(chunk);
      }
      zos.closeEntry();
    }

    Exception ex =
        Assertions.assertThrows(
            RuntimeException.class,
            () -> {
              service.importFile("bomb.spectra.zip");
            });
    Assertions.assertTrue(ex.getMessage().contains("Decompression limit exceeded"));
  }
}
