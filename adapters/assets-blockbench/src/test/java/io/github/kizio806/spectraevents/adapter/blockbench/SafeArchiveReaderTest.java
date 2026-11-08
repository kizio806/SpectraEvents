package io.github.kizio806.spectraevents.adapter.blockbench;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    assertThrows(
        IllegalArgumentException.class, () -> SafeArchiveReader.validateSafeEntryName("foo/./bar"));
  }

  @Test
  void testReadArchiveRejectsExcessiveTotalUncompressedBytes() throws Exception {
    Path zipFile = tempDir.resolve("huge_total.zip");
    byte[] chunk = new byte[1024 * 1024]; // 1MB of zeroes
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      for (int i = 0; i < 2; i++) {
        ZipEntry entry = new ZipEntry("entry_" + i + ".txt");
        zos.putNextEntry(entry);
        for (int c = 0; c < 17; c++) {
          zos.write(chunk); // 17MB per entry * 2 = 34MB > 32MB MAX_TOTAL_UNCOMPRESSED_BYTES
        }
        zos.closeEntry();
      }
    }

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> SafeArchiveReader.readArchive(zipFile));
    assertTrue(ex.getMessage().contains("expanded size limit"));
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
      zos.write(new byte[0]);
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
      zos.write(new byte[0]);
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

  @Test
  void testReadArchiveRejectsDuplicateEntry() throws Exception {
    Path zipFile = tempDir.resolve("duplicate.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      ZipEntry e1 = new ZipEntry("entry_a.txt");
      zos.putNextEntry(e1);
      zos.write(new byte[] {1});
      zos.closeEntry();

      ZipEntry e2 = new ZipEntry("entry_b.txt");
      zos.putNextEntry(e2);
      zos.write(new byte[] {2});
      zos.closeEntry();
    }

    byte[] bytes = Files.readAllBytes(zipFile);
    byte[] target = "entry_b.txt".getBytes(StandardCharsets.UTF_8);
    byte[] replacement = "entry_a.txt".getBytes(StandardCharsets.UTF_8);
    for (int i = 0; i <= bytes.length - target.length; i++) {
      boolean match = true;
      for (int j = 0; j < target.length; j++) {
        if (bytes[i + j] != target[j]) {
          match = false;
          break;
        }
      }
      if (match) {
        System.arraycopy(replacement, 0, bytes, i, target.length);
      }
    }
    Files.write(zipFile, bytes);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> SafeArchiveReader.readArchive(zipFile));
    assertTrue(ex.getMessage().contains("duplicate ZIP entry"));
  }

  @Test
  void testReadBoundedEntryExceedsMaxEntryBytesThrows() throws Exception {
    try (java.io.InputStream stream =
        new java.io.InputStream() {
          private int count = 0;

          @Override
          public int read() {
            count++;
            return 1;
          }

          @Override
          public int read(byte[] b, int off, int len) {
            count += len;
            if (count > SafeArchiveReader.MAX_ENTRY_BYTES + 8192) {
              return -1;
            }
            return len;
          }
        }) {
      assertThrows(
          IllegalArgumentException.class,
          () -> SafeArchiveReader.readBoundedEntry(stream, "large_file.txt"));
    }
  }
}
