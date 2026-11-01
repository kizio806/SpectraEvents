package io.github.kizio806.spectraevents.adapter.blockbench;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Safe ZIP archive extraction and bounded reading utilities resisting zip-slip and zip-bomb
 * attacks.
 */
final class SafeArchiveReader {
  static final int MAX_ARCHIVE_ENTRIES = 18;
  static final int MAX_ENTRY_BYTES = 24_000_000;
  static final int MAX_TOTAL_UNCOMPRESSED_BYTES = 32_000_000;

  private SafeArchiveReader() {}

  static Map<String, byte[]> readArchive(Path sourceFile) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    Set<String> names = new HashSet<>();
    long totalBytes = 0L;
    int entryCount = 0;

    try (InputStream input = Files.newInputStream(sourceFile);
        ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        entryCount++;
        if (entryCount > MAX_ARCHIVE_ENTRIES) {
          throw new IllegalArgumentException("Asset bundle contains more than 18 ZIP entries");
        }
        String name = entry.getName();
        validateSafeEntryName(name);
        if (!names.add(name)) {
          throw new IllegalArgumentException("Asset bundle contains duplicate ZIP entry: " + name);
        }
        if (entry.isDirectory()) {
          if (!"textures/".equals(name)) {
            throw new IllegalArgumentException(
                "Asset bundle contains unsupported directory: " + name);
          }
          continue;
        }

        byte[] bytes = readBoundedEntry(zip, name);
        totalBytes += bytes.length;
        if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
          throw new IllegalArgumentException(
              "Asset bundle exceeds expanded size limit of "
                  + MAX_TOTAL_UNCOMPRESSED_BYTES
                  + " bytes");
        }
        entries.put(name, bytes);
      }
    }
    return Map.copyOf(entries);
  }

  static void validateSafeEntryName(String name) {
    if (name == null
        || name.isBlank()
        || name.startsWith("/")
        || name.startsWith("\\")
        || name.contains("\\")
        || name.contains("..")
        || Path.of(name).isAbsolute()
        || !Path.of(name).normalize().toString().replace('\\', '/').equals(name)) {
      throw new IllegalArgumentException("Asset bundle contains unsafe ZIP entry: " + name);
    }
  }

  static byte[] readBoundedEntry(InputStream input, String name) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int read;
    while ((read = input.read(buffer)) != -1) {
      if (output.size() > MAX_ENTRY_BYTES - read) {
        throw new IllegalArgumentException(
            "Asset bundle entry exceeds " + MAX_ENTRY_BYTES + " bytes: " + name);
      }
      output.write(buffer, 0, read);
    }
    return output.toByteArray();
  }

  static byte[] requiredEntry(Map<String, byte[]> entries, String path) {
    byte[] bytes = entries.get(path);
    if (bytes == null) {
      throw new IllegalArgumentException("Asset bundle is missing required entry: " + path);
    }
    return bytes;
  }
}
