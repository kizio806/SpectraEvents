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
  void testZipTotalDecompressionLimitEnforced() throws Exception {
    Path zipPath = sourceDir.resolve("bomb.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
      byte[] chunk = new byte[1100000]; // 1.1MB
      // 5 entries of 1.1MB = 5.5MB total. Should fail total limit.
      for (int i = 0; i < 5; i++) {
        zos.putNextEntry(new ZipEntry("bomb" + i + ".bbmodel"));
        zos.write(chunk);
        zos.closeEntry();
      }
    }

    Exception ex =
        Assertions.assertThrows(
            RuntimeException.class,
            () -> {
              service.importFile("bomb.spectra.zip");
            });
    Assertions.assertTrue(ex.getMessage().contains("Decompression limit exceeded (max 5000000"));
  }

  @Test
  void testZipTooManyEntriesRejected() throws Exception {
    Path zipPath = sourceDir.resolve("many.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
      // Exceed MAX_ZIP_ENTRIES = 50
      for (int i = 0; i < 55; i++) {
        zos.putNextEntry(new ZipEntry("file" + i + ".txt"));
        zos.write("a".getBytes());
        zos.closeEntry();
      }
    }

    Exception ex =
        Assertions.assertThrows(
            RuntimeException.class,
            () -> {
              service.importFile("many.spectra.zip");
            });
    Assertions.assertTrue(ex.getMessage().contains("Too many entries in ZIP file"));
  }

  @Test
  void testZipSingleEntrySizeRejected() throws Exception {
    Path zipPath = sourceDir.resolve("huge_entry.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
      zos.putNextEntry(new ZipEntry("huge.bbmodel"));
      byte[] chunk = new byte[100000];
      // 22 * 100kb = 2.2MB single entry (limit is 2MB)
      for (int i = 0; i < 22; i++) {
        zos.write(chunk);
      }
      zos.closeEntry();
    }

    Exception ex =
        Assertions.assertThrows(
            RuntimeException.class,
            () -> {
              service.importFile("huge_entry.spectra.zip");
            });
    Assertions.assertTrue(
        ex.getMessage().contains("Single entry exceeded decompression limit (max 2000000"));
  }
}
