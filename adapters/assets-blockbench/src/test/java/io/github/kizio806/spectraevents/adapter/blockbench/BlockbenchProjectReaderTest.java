package io.github.kizio806.spectraevents.adapter.blockbench;

import io.github.kizio806.spectraevents.core.visual.animation.Easing;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlockbenchProjectReaderTest {
  private static final byte[] TEXTURE =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==");

  @TempDir Path tempDirectory;

  private final BlockbenchProjectReader reader = new BlockbenchProjectReader();

  @Test
  void importsVerifiedGenericModelGeometryTextureHierarchyAndAnimation() throws Exception {
    Path bundle = writeBundle(projectJson("free"), false);

    SpectraAssetDocument document = reader.read(bundle);

    Assertions.assertEquals("meteor", document.modelId());
    Assertions.assertEquals(1, document.textures().size());
    Assertions.assertArrayEquals(TEXTURE, document.textures().get("texture").data().orElseThrow());
    Assertions.assertEquals(1, document.nodes().size());
    Assertions.assertEquals("root", document.nodes().getFirst().nodeId());
    Assertions.assertEquals(1, document.nodes().getFirst().cubes().size());
    Assertions.assertEquals(16.0f, document.nodes().getFirst().cubes().getFirst().to().x());
    Assertions.assertEquals(1, document.animations().size());
    Assertions.assertEquals(
        Easing.STEP,
        document.animations().get("pulse").rotationTracks().get("root").getFirst().easing());
  }

  @Test
  void rejectsUnsupportedBlockbenchProjectFormat() throws Exception {
    Path bundle = writeBundle(projectJson("java_block"), false);

    IllegalArgumentException error =
        Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(bundle));

    Assertions.assertTrue(error.getMessage().contains("Generic Model"));
  }

  @Test
  void rejectsChecksumMismatch() throws Exception {
    Path bundle = writeBundle(projectJson("free"), true);

    IllegalArgumentException error =
        Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(bundle));

    Assertions.assertTrue(error.getMessage().contains("SHA-256 mismatch"));
  }

  @Test
  void rejectsZipSlipBeforeParsingManifest() throws IOException {
    Path bundle = tempDirectory.resolve("malicious.spectra.zip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(bundle))) {
      zip.putNextEntry(new ZipEntry("../manifest.json"));
      zip.write("{}".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }

    IllegalArgumentException error =
        Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(bundle));

    Assertions.assertTrue(error.getMessage().contains("unsafe ZIP entry"));
  }

  @Test
  void rejectsLooseBlockbenchProjectFiles() throws IOException {
    Path source = tempDirectory.resolve("model.bbmodel");
    Files.writeString(source, projectJson("free"));

    IllegalArgumentException error =
        Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(source));

    Assertions.assertTrue(error.getMessage().contains(".spectra.zip"));
  }

  private Path writeBundle(String model, boolean corruptChecksum) throws Exception {
    Path bundle = tempDirectory.resolve("meteor.spectra.zip");
    String modelHash =
        corruptChecksum ? "0".repeat(64) : sha256(model.getBytes(StandardCharsets.UTF_8));
    String textureHash = sha256(TEXTURE);
    String manifest =
        """
        {
          "schemaVersion": 1,
          "modelId": "meteor",
          "model": "model.bbmodel",
          "textures": ["textures/gem.png"],
          "sha256": {
            "model.bbmodel": "%s",
            "textures/gem.png": "%s"
          }
        }
        """
            .formatted(modelHash, textureHash);
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(bundle))) {
      writeEntry(zip, "manifest.json", manifest.getBytes(StandardCharsets.UTF_8));
      writeEntry(zip, "model.bbmodel", model.getBytes(StandardCharsets.UTF_8));
      writeEntry(zip, "textures/gem.png", TEXTURE);
    }
    return bundle;
  }

  private static void writeEntry(ZipOutputStream zip, String name, byte[] content)
      throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(content);
    zip.closeEntry();
  }

  private static String sha256(byte[] bytes) throws Exception {
    StringBuilder result = new StringBuilder();
    for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) {
      result.append(String.format(Locale.ROOT, "%02x", value));
    }
    return result.toString();
  }

  private static String projectJson(String modelFormat) {
    return """
        {
          "meta": {"format_version": "5.0.0", "model_format": "%s"},
          "textures": [{"id": "texture", "name": "gem.png", "source": "textures/gem.png"}],
          "elements": [{
            "uuid": "cube",
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "origin": [8, 8, 8],
            "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#texture"}}
          }],
          "outliner": [{"uuid": "root", "name": "Root", "origin": [8, 8, 8], "children": ["cube"]}],
          "animations": [{
            "name": "pulse",
            "length": 1.0,
            "loop": "loop",
            "animators": {"root": {"keyframes": [{
              "channel": "rotation",
              "time": 0.0,
              "interpolation": "step",
              "data_points": [{"x": "0", "y": "180", "z": "0"}]
            }]}}
          }]
        }
        """
        .formatted(modelFormat);
  }
}
