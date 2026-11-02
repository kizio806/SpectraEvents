package io.github.kizio806.spectraevents.adapter.blockbench;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SafeArchiveReaderTest {

  @TempDir Path tempDir;

  @Test
  void testValidateSafeEntryNameRejectsUnsafe() {
    assertThrows(
        IllegalArgumentException.class, () -> SafeArchiveReader.validateSafeEntryName(null));
    assertThrows(IllegalArgumentException.class, () -> SafeArchiveReader.validateSafeEntryName(""));
    assertThrows(
        IllegalArgumentException.class, () -> SafeArchiveReader.validateSafeEntryName("   "));
    assertThrows(
        IllegalArgumentException.class,
        () -> SafeArchiveReader.validateSafeEntryName("/absolute/path"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SafeArchiveReader.validateSafeEntryName("\\backslash"));
    assertThrows(
        IllegalArgumentException.class, () -> SafeArchiveReader.validateSafeEntryName("foo\\bar"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SafeArchiveReader.validateSafeEntryName("../traversal"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SafeArchiveReader.validateSafeEntryName("dir/../file"));
  }

  @Test
  void testValidateSafeEntryNameAcceptsValid() {
    SafeArchiveReader.validateSafeEntryName("model.bbmodel");
    SafeArchiveReader.validateSafeEntryName("textures/texture.png");
  }

  @Test
  void testReadBoundedEntrySuccess() throws Exception {
    byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
    byte[] result = SafeArchiveReader.readBoundedEntry(new ByteArrayInputStream(data), "test.txt");
    assertArrayEquals(data, result);
  }

  @Test
  void testRequiredEntry() {
    Map<String, byte[]> entries = Map.of("manifest.json", new byte[] {1, 2, 3});
    assertNotNull(SafeArchiveReader.requiredEntry(entries, "manifest.json"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SafeArchiveReader.requiredEntry(entries, "missing.json"));
  }

  @Test
  void testReadArchiveValidZip() throws Exception {
    Path zipFile = tempDir.resolve("valid.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      ZipEntry dir = new ZipEntry("textures/");
      zos.putNextEntry(dir);
      zos.closeEntry();

      ZipEntry entry = new ZipEntry("textures/stone.png");
      zos.putNextEntry(entry);
      zos.write("PNG_DATA".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }

    Map<String, byte[]> result = SafeArchiveReader.readArchive(zipFile);
    assertEquals(1, result.size());
    assertNotNull(result.get("textures/stone.png"));
  }

  @Test
  void testReadArchiveRejectsUnsupportedDirectory() throws Exception {
    Path zipFile = tempDir.resolve("bad_dir.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      ZipEntry dir = new ZipEntry("scripts/");
      zos.putNextEntry(dir);
      zos.closeEntry();
    }

    assertThrows(IllegalArgumentException.class, () -> SafeArchiveReader.readArchive(zipFile));
  }

  @Test
  void testReadArchiveRejectsTooManyEntries() throws Exception {
    Path zipFile = tempDir.resolve("too_many.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      for (int i = 0; i < SafeArchiveReader.MAX_ARCHIVE_ENTRIES + 1; i++) {
        ZipEntry entry = new ZipEntry("file_" + i + ".txt");
        zos.putNextEntry(entry);
        zos.write(new byte[] {1});
        zos.closeEntry();
      }
    }

    assertThrows(IllegalArgumentException.class, () -> SafeArchiveReader.readArchive(zipFile));
  }
}
