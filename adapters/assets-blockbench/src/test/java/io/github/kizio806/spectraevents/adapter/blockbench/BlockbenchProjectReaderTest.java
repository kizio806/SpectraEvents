package io.github.kizio806.spectraevents.adapter.blockbench;

import io.github.kizio806.spectraevents.core.visual.animation.Easing;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
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
    Path bundle = writeBundle(projectJson("bedrock"), false);

    IllegalArgumentException error =
        Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(bundle));

    Assertions.assertTrue(error.getMessage().contains("Generic Model"));
  }

  @Test
  void importsJavaBlockProjectGeometryTextureHierarchyAndAnimation() throws Exception {
    Path bundle = writeBundle(projectJson("java_block"), false);

    SpectraAssetDocument document = reader.read(bundle);

    Assertions.assertEquals("meteor", document.modelId());
    Assertions.assertEquals(1, document.nodes().size());
    Assertions.assertTrue(document.animations().containsKey("pulse"));
  }

  @Test
  void normalizesEmbeddedJpegTextureToPng() throws Exception {
    Path bundle =
        writeBundle(
            projectJson("free").replaceFirst("data:image/png;base64,[^\"]+", embeddedJpegDataUri()),
            false);

    SpectraAssetDocument document = reader.read(bundle);

    byte[] texture = document.textures().get("texture").data().orElseThrow();
    Assertions.assertArrayEquals(
        new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47}, java.util.Arrays.copyOf(texture, 4));
  }

  @Test
  void acceptsAnEmbedded4096By4096Texture() throws Exception {
    Path source = tempDirectory.resolve("model.bbmodel");
    Files.writeString(
        source,
        projectJson("free")
            .replaceFirst("data:image/png;base64,[^\"]+", embeddedPngDataUri(4096, 4096)));

    SpectraAssetDocument document = reader.read(source);

    Assertions.assertTrue(document.textures().get("texture").data().orElseThrow().length > 0);
  }

  @Test
  void acceptsAProjectLargerThanTheLegacyTwoMegabyteLimit() throws Exception {
    Path source = tempDirectory.resolve("model.bbmodel");
    Files.writeString(source, projectJson("free") + " ".repeat(2_100_000));

    SpectraAssetDocument document = reader.read(source);

    Assertions.assertEquals("model", document.modelId());
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
  void importsLooseBlockbenchProjectFiles() throws IOException {
    Path source = tempDirectory.resolve("model.bbmodel");
    Files.writeString(source, projectJson("free"));

    var document = reader.read(source);

    Assertions.assertEquals("model", document.modelId());
    Assertions.assertTrue(document.animations().containsKey("pulse"));
  }

  @Test
  void importsBlockbenchTextureIndexReferences() throws IOException {
    Path source = tempDirectory.resolve("model.bbmodel");
    Files.writeString(
        source, projectJson("free").replace("\"texture\": \"#texture\"", "\"texture\": 0"));

    SpectraAssetDocument document = reader.read(source);

    Assertions.assertEquals(
        "texture",
        document.nodes().getFirst().cubes().getFirst().faces().get("north").textureRef());
  }

  @Test
  void normalizesMirroredCubeBoundsForVanillaResourcePackOutput() throws IOException {
    Path source = tempDirectory.resolve("model.bbmodel");
    Files.writeString(
        source,
        projectJson("free")
            .replace("\"from\": [0, 0, 0]", "\"from\": [16, 0, 0]")
            .replace("\"to\": [16, 16, 16]", "\"to\": [0, 16, 16]"));

    SpectraAssetDocument document = reader.read(source);

    Assertions.assertEquals(0.0f, document.nodes().getFirst().cubes().getFirst().from().x());
    Assertions.assertEquals(16.0f, document.nodes().getFirst().cubes().getFirst().to().x());
  }

  @Test
  void rejectsMeshGeometryWithAnActionableExportDiagnostic() throws IOException {
    Path source = tempDirectory.resolve("mesh.bbmodel");
    Files.writeString(
        source,
        projectJson("free")
            .replace("\"uuid\": \"cube\"", "\"uuid\": \"cube\", \"type\": \"mesh\""));

    IllegalArgumentException error =
        Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(source));

    Assertions.assertTrue(error.getMessage().contains("export cuboid elements"));
  }

  @Test
  void mapsBlockbenchCatmullRomInterpolationToTheSupportedSmoothCurve() throws IOException {
    Path source = tempDirectory.resolve("model.bbmodel");
    Files.writeString(
        source,
        projectJson("free")
            .replace("\"interpolation\": \"step\"", "\"interpolation\": \"catmullrom\""));

    SpectraAssetDocument document = reader.read(source);

    Assertions.assertEquals(
        Easing.EASE_IN_OUT_CUBIC,
        document.animations().get("pulse").rotationTracks().get("root").getFirst().easing());
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
            "model.bbmodel": "MODEL_HASH",
            "textures/gem.png": "TEXTURE_HASH"
          }
        }
        """
            .replace("MODEL_HASH", modelHash)
            .replace("TEXTURE_HASH", textureHash);
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

  @Test
  void readsDirectBbmodelFile() throws Exception {
    Path bbmodelFile = tempDirectory.resolve("gem.bbmodel");
    Files.writeString(bbmodelFile, projectJson("free"), StandardCharsets.UTF_8);

    SpectraAssetDocument document = reader.read(bbmodelFile);
    Assertions.assertEquals("gem", document.modelId());
    Assertions.assertEquals(1, document.nodes().size());
  }

  @Test
  void rejectsInvalidSourceFileTypes() {
    Assertions.assertThrows(NullPointerException.class, () -> reader.read(null));

    Path missing = tempDirectory.resolve("missing.bbmodel");
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(missing));

    Path badExtension = tempDirectory.resolve("model.txt");
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          Files.writeString(badExtension, "content");
          reader.read(badExtension);
        });

    // Invalid modelId in filename (uppercase)
    Path badModelId = tempDirectory.resolve("InvalidModelName.bbmodel");
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          Files.writeString(badModelId, validProject());
          reader.read(badModelId);
        });
  }

  @Test
  void rejectsInvalidProjectStructuresAndLimits() throws Exception {
    // 1. Unsupported format version 3.0.0
    String v3 =
        validProject().replace("\"format_version\": \"5.0.0\"", "\"format_version\": \"3.0.0\"");
    Path fileV3 = tempDirectory.resolve("v3.bbmodel");
    Files.writeString(fileV3, v3);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(fileV3));

    // 2. Zero size cube on X axis
    String zeroCube = validProject().replace("\"from\": [0, 0, 0]", "\"from\": [16, 0, 0]");
    Path fileZero = tempDirectory.resolve("zero.bbmodel");
    Files.writeString(fileZero, zeroCube);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(fileZero));

    // 3. Unsupported face name
    String badFace = validProject().replace("\"north\"", "\"invalid_face\"");
    Path fileFace = tempDirectory.resolve("bad_face.bbmodel");
    Files.writeString(fileFace, badFace);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(fileFace));

    // 4. Invalid face rotation (45 deg)
    String badRot =
        validProject()
            .replace(
                "\"north\": {\"uv\": [0, 0, 16, 16], \"texture\": \"#texture\"}",
                "\"north\": {\"uv\": [0, 0, 16, 16], \"texture\": \"#texture\", \"rotation\": 45}");
    Path fileRot = tempDirectory.resolve("bad_rot.bbmodel");
    Files.writeString(fileRot, badRot);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(fileRot));

    // 5. Unknown texture reference in cube
    String unkTex =
        validProject().replace("\"texture\": \"#texture\"", "\"texture\": \"#missing_tex\"");
    Path fileTex = tempDirectory.resolve("unk_tex.bbmodel");
    Files.writeString(fileTex, unkTex);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(fileTex));

    // 6. Animation position and scale channels
    String animChannels =
        """
        {
          "meta": {"format_version": "5.0.0", "model_format": "free"},
          "textures": [{"id": "texture", "name": "gem.png", "source": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg=="}],
          "elements": [{
            "uuid": "cube",
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "origin": [8, 8, 8],
            "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#texture"}}
          }],
          "outliner": [{"uuid": "root", "name": "Root", "origin": [8, 8, 8], "children": ["cube"]}],
          "animations": [{
            "name": "move",
            "length": 1.0,
            "loop": "once",
            "animators": {"root": {"keyframes": [
              {
                "channel": "position",
                "time": 0.0,
                "data_points": [{"x": "0", "y": "1", "z": "0"}]
              },
              {
                "channel": "scale",
                "time": 0.5,
                "data_points": [{"x": "1", "y": "2", "z": "1"}]
              }
            ]}}
          }]
        }
        """;
    Path fileAnim = tempDirectory.resolve("channels.bbmodel");
    Files.writeString(fileAnim, animChannels);
    SpectraAssetDocument docAnim = reader.read(fileAnim);
    Assertions.assertEquals(1, docAnim.animations().size());

    // 7. JPEG embedded texture normalization
    String jpegProject =
        validProject()
            .replace(
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==",
                embeddedJpegDataUri());
    Path fileJpeg = tempDirectory.resolve("jpeg_model.bbmodel");
    Files.writeString(fileJpeg, jpegProject);
    SpectraAssetDocument docJpeg = reader.read(fileJpeg);
    Assertions.assertTrue(docJpeg.textures().get("texture").data().isPresent());

    // 8. Texture dimensions exceeding limit
    String oversizedProject =
        validProject()
            .replace(
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==",
                embeddedPngDataUri(5000, 10));
    Path fileOversized = tempDirectory.resolve("oversized.bbmodel");
    Files.writeString(fileOversized, oversizedProject);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(fileOversized));
  }

  @Test
  void rejectsInvalidManifestStructures() throws Exception {
    String modelBytes = validProject();
    String modelHash = sha256(modelBytes.getBytes(StandardCharsets.UTF_8));
    String texHash = sha256(TEXTURE);

    // 1. Unsupported schemaVersion
    String badSchema =
        """
        {
          "schemaVersion": 2,
          "modelId": "meteor",
          "model": "model.bbmodel",
          "textures": ["textures/gem.png"],
          "sha256": {"model.bbmodel": "MODEL_HASH", "textures/gem.png": "TEX_HASH"}
        }
        """
            .replace("MODEL_HASH", modelHash)
            .replace("TEX_HASH", texHash);
    Path b1 = tempDirectory.resolve("b1.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(b1))) {
      writeEntry(zos, "manifest.json", badSchema.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "model.bbmodel", modelBytes.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "textures/gem.png", TEXTURE);
    }
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(b1));

    // 2. Invalid modelId regex in manifest
    String badModelId =
        """
        {
          "schemaVersion": 1,
          "modelId": "Invalid_Model!",
          "model": "model.bbmodel",
          "textures": ["textures/gem.png"],
          "sha256": {"model.bbmodel": "MODEL_HASH", "textures/gem.png": "TEX_HASH"}
        }
        """
            .replace("MODEL_HASH", modelHash)
            .replace("TEX_HASH", texHash);
    Path b2 = tempDirectory.resolve("b2.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(b2))) {
      writeEntry(zos, "manifest.json", badModelId.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "model.bbmodel", modelBytes.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "textures/gem.png", TEXTURE);
    }
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(b2));

    // 3. Manifest model name not model.bbmodel
    String badModelName =
        """
        {
          "schemaVersion": 1,
          "modelId": "meteor",
          "model": "wrong.bbmodel",
          "textures": ["textures/gem.png"],
          "sha256": {"model.bbmodel": "MODEL_HASH", "textures/gem.png": "TEX_HASH"}
        }
        """
            .replace("MODEL_HASH", modelHash)
            .replace("TEX_HASH", texHash);
    Path b3 = tempDirectory.resolve("b3.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(b3))) {
      writeEntry(zos, "manifest.json", badModelName.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "model.bbmodel", modelBytes.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "textures/gem.png", TEXTURE);
    }
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(b3));

    // 4. Manifest textures contains invalid path
    String badTexPath =
        """
        {
          "schemaVersion": 1,
          "modelId": "meteor",
          "model": "model.bbmodel",
          "textures": ["invalid_path.png"],
          "sha256": {"model.bbmodel": "MODEL_HASH", "invalid_path.png": "TEX_HASH"}
        }
        """
            .replace("MODEL_HASH", modelHash)
            .replace("TEX_HASH", texHash);
    Path b4 = tempDirectory.resolve("b4.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(b4))) {
      writeEntry(zos, "manifest.json", badTexPath.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "model.bbmodel", modelBytes.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "invalid_path.png", TEXTURE);
    }
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(b4));

    // 5. Manifest sha256 contains invalid hex
    String badSha =
        """
        {
          "schemaVersion": 1,
          "modelId": "meteor",
          "model": "model.bbmodel",
          "textures": ["textures/gem.png"],
          "sha256": {"model.bbmodel": "not_64_hex_characters", "textures/gem.png": "TEX_HASH"}
        }
        """
            .replace("TEX_HASH", texHash);
    Path b5 = tempDirectory.resolve("b5.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(b5))) {
      writeEntry(zos, "manifest.json", badSha.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "model.bbmodel", modelBytes.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "textures/gem.png", TEXTURE);
    }
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(b5));

    // 6. Zip contains file outside manifest
    Path b6 = tempDirectory.resolve("b6.spectra.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(b6))) {
      String validManifest =
          """
          {
            "schemaVersion": 1,
            "modelId": "meteor",
            "model": "model.bbmodel",
            "textures": ["textures/gem.png"],
            "sha256": {"model.bbmodel": "MODEL_HASH", "textures/gem.png": "TEX_HASH"}
          }
          """
              .replace("MODEL_HASH", modelHash)
              .replace("TEX_HASH", texHash);
      writeEntry(zos, "manifest.json", validManifest.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "model.bbmodel", modelBytes.getBytes(StandardCharsets.UTF_8));
      writeEntry(zos, "textures/gem.png", TEXTURE);
      writeEntry(zos, "extra.txt", new byte[] {1, 2, 3});
    }
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(b6));
  }

  @Test
  void rejectsInvalidOutlinerAndElements() throws Exception {
    // 1. Empty outliner
    String emptyOutliner =
        validProject()
            .replace(
                "\"outliner\": [{\"uuid\": \"root\", \"name\": \"Root\", \"origin\": [8, 8, 8], \"children\": [\"cube\"]}]",
                "\"outliner\": []");
    Path f1 = tempDirectory.resolve("f1.bbmodel");
    Files.writeString(f1, emptyOutliner);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f1));

    // 2. Empty elements
    String emptyElements =
        validProject().replaceFirst("\"elements\":\\s*\\[[\\s\\S]*?\\],", "\"elements\": [],");
    Path f2 = tempDirectory.resolve("f2.bbmodel");
    Files.writeString(f2, emptyElements);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f2));

    // 3. Outliner references unknown cube
    String unkCube =
        validProject().replace("\"children\": [\"cube\"]", "\"children\": [\"nonexistent_cube\"]");
    Path f3 = tempDirectory.resolve("f3.bbmodel");
    Files.writeString(f3, unkCube);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f3));

    // 4. Outliner child invalid type (number)
    String badChild = validProject().replace("\"children\": [\"cube\"]", "\"children\": [123]");
    Path f4 = tempDirectory.resolve("f4.bbmodel");
    Files.writeString(f4, badChild);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f4));

    // 5. Duplicate group uuid
    String dupGroup =
        validProject()
            .replace(
                "\"outliner\": [{\"uuid\": \"root\", \"name\": \"Root\", \"origin\": [8, 8, 8], \"children\": [\"cube\"]}]",
                "\"outliner\": [{\"uuid\": \"root\", \"name\": \"Root\", \"children\": [\"cube\"]}, {\"uuid\": \"root\", \"name\": \"Root2\", \"children\": []}]");
    Path f5 = tempDirectory.resolve("f5.bbmodel");
    Files.writeString(f5, dupGroup);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f5));

    // 6. Group uuid normalizes to invalid model ID
    String badGroupUuid = validProject().replace("\"uuid\": \"root\"", "\"uuid\": \"Bad Group!\"");
    Path f6 = tempDirectory.resolve("f6.bbmodel");
    Files.writeString(f6, badGroupUuid);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f6));

    // 7. Face texture reference blank
    String blankTex = validProject().replace("\"texture\": \"#texture\"", "\"texture\": \"   \"");
    Path f7 = tempDirectory.resolve("f7.bbmodel");
    Files.writeString(f7, blankTex);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f7));

    // 8. Face texture index out of bounds
    String outOfBoundsTex = validProject().replace("\"texture\": \"#texture\"", "\"texture\": 99");
    Path f8 = tempDirectory.resolve("f8.bbmodel");
    Files.writeString(f8, outOfBoundsTex);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f8));

    // 9. Face texture index valid numeric index
    String validNumTex = validProject().replace("\"texture\": \"#texture\"", "\"texture\": 0");
    Path f9 = tempDirectory.resolve("f9.bbmodel");
    Files.writeString(f9, validNumTex);
    SpectraAssetDocument docNum = reader.read(f9);
    Assertions.assertEquals(
        "texture", docNum.nodes().getFirst().cubes().getFirst().faces().get("north").textureRef());
  }

  @Test
  void rejectsInvalidAnimations() throws Exception {
    // 1. Animation length <= 0
    String zeroLen = validProject().replace("\"length\": 1.0", "\"length\": 0.0");
    Path f1 = tempDirectory.resolve("a1.bbmodel");
    Files.writeString(f1, zeroLen);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f1));

    // 2. Animation targets unknown group
    String unkGroup =
        validProject().replace("\"animators\": {\"root\":", "\"animators\": {\"unknown_group\":");
    Path f2 = tempDirectory.resolve("a2.bbmodel");
    Files.writeString(f2, unkGroup);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f2));

    // 3. Keyframe time exceeds animation length
    String excessTime = validProject().replace("\"time\": 0.0", "\"time\": 5.0");
    Path f3 = tempDirectory.resolve("a3.bbmodel");
    Files.writeString(f3, excessTime);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f3));

    // 4. Unsupported animation channel
    String badChannel =
        validProject().replace("\"channel\": \"rotation\"", "\"channel\": \"color\"");
    Path f4 = tempDirectory.resolve("a4.bbmodel");
    Files.writeString(f4, badChannel);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f4));

    // 5. Duplicate animation name
    String dupAnim =
        validProject()
            .replace(
                "\"animations\": [{",
                "\"animations\": [{\"name\": \"pulse\", \"length\": 1.0, \"loop\": \"once\", \"animators\": {}}, {");
    Path f5 = tempDirectory.resolve("a5.bbmodel");
    Files.writeString(f5, dupAnim);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f5));
  }

  @Test
  void rejectsInvalidEmbeddedTextures() throws Exception {
    // 1. Embedded texture invalid Base64
    String badB64 =
        validProject()
            .replace(
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==",
                "data:image/png;base64,???not_base64!!!");
    Path f1 = tempDirectory.resolve("t1.bbmodel");
    Files.writeString(f1, badB64);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f1));

    // 2. Decodable but not PNG or JPEG (e.g. GIF)
    // 1x1 transparent GIF
    String gifData =
        "data:image/png;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
    String gifProject =
        validProject()
            .replace(
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==",
                gifData);
    Path f2 = tempDirectory.resolve("t2.bbmodel");
    Files.writeString(f2, gifProject);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f2));

    // 3. Texture source declared bundle path but not present in bundleTextures
    String missingBundleTex =
        validProject()
            .replace(
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==",
                "textures/gem.png");
    Path f3 = tempDirectory.resolve("t3.bbmodel");
    Files.writeString(f3, missingBundleTex);
    Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f3));
  }

  @Test
  void rejectsOversizedArchiveAndEntryFiles() throws Exception {
    // 1. Sparse bbmodel > 24_000_000 bytes
    Path bigBbmodel = tempDirectory.resolve("big_model.bbmodel");
    try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(bigBbmodel.toFile(), "rw")) {
      raf.setLength(24_000_001L);
    }
    var ex1 =
        Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(bigBbmodel));
    Assertions.assertTrue(
        ex1.getMessage().contains("exceeds size limit"), () -> "Got: " + ex1.getMessage());

    // 2. Sparse zip > 25_000_000 bytes
    Path bigZip = tempDirectory.resolve("big_bundle.spectra.zip");
    try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(bigZip.toFile(), "rw")) {
      raf.setLength(25_000_001L);
    }
    var ex2 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(bigZip));
    Assertions.assertTrue(
        ex2.getMessage().contains("exceeds compressed size limit"),
        () -> "Got: " + ex2.getMessage());
  }

  @Test
  void rejectsInvalidFacesAndRotations() throws Exception {
    // 1. Invalid face rotation (e.g. 45 degrees)
    String badRot =
        validProject()
            .replace("\"texture\": \"#texture\"", "\"texture\": \"#texture\", \"rotation\": 45");
    Path f1 = tempDirectory.resolve("rot_45.bbmodel");
    Files.writeString(f1, badRot);
    var ex1 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f1));
    Assertions.assertTrue(
        ex1.getMessage().contains("Face rotation must be 0, 90, 180, or 270"),
        () -> "Got: " + ex1.getMessage());

    // 2. Blank face texture
    String blankTex = validProject().replace("\"texture\": \"#texture\"", "\"texture\": \"   \"");
    Path f2 = tempDirectory.resolve("blank_tex.bbmodel");
    Files.writeString(f2, blankTex);
    var ex2 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f2));
    Assertions.assertTrue(
        ex2.getMessage().contains("faces.texture must not be blank"),
        () -> "Got: " + ex2.getMessage());

    // 3. Boolean face texture
    String boolTex = validProject().replace("\"texture\": \"#texture\"", "\"texture\": true");
    Path f3 = tempDirectory.resolve("bool_tex.bbmodel");
    Files.writeString(f3, boolTex);
    var ex3 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f3));
    Assertions.assertTrue(
        ex3.getMessage().contains("faces.texture must be a texture ID or index"),
        () -> "Got: " + ex3.getMessage());
  }

  @Test
  void rejectsEmptyOutlinerAndExcessiveTextures() throws Exception {
    // 1. Empty elements
    String emptyElements =
        """
        {
          "meta": {"format_version": "5.0.0", "model_format": "free"},
          "textures": [{"id": "texture", "name": "gem.png", "source": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg=="}],
          "elements": [],
          "outliner": []
        }
        """;
    Path f0 = tempDirectory.resolve("no_elements.bbmodel");
    Files.writeString(f0, emptyElements);
    var ex0 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f0));
    Assertions.assertTrue(
        ex0.getMessage().contains("must contain at least one cube"),
        () -> "Got: " + ex0.getMessage());

    // 2. Empty outliner with cubes present
    String emptyOutliner =
        validProject()
            .replace(
                "\"outliner\": [{\"uuid\": \"root\", \"name\": \"Root\", \"origin\": [8, 8, 8], \"children\": [\"cube\"]}]",
                "\"outliner\": []");
    Path f1 = tempDirectory.resolve("no_outliner.bbmodel");
    Files.writeString(f1, emptyOutliner);
    var ex1 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f1));
    Assertions.assertTrue(
        ex1.getMessage()
            .contains("Every Blockbench cube must belong to exactly one outliner group"),
        () -> "Got: " + ex1.getMessage());

    // 3. More than 16 textures
    StringBuilder manyTextures = new StringBuilder("[");
    for (int i = 0; i < 17; i++) {
      if (i > 0) manyTextures.append(",");
      manyTextures
          .append("{\"id\": \"t")
          .append(i)
          .append("\", \"name\": \"t")
          .append(i)
          .append(
              ".png\", \"source\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==\"}");
    }
    manyTextures.append("]");
    String excessiveTex =
        validProject()
            .replace(
                "[{\"id\": \"texture\", \"name\": \"gem.png\", \"source\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==\"}]",
                manyTextures.toString());
    Path f2 = tempDirectory.resolve("many_tex.bbmodel");
    Files.writeString(f2, excessiveTex);
    var ex2 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f2));
    Assertions.assertTrue(
        ex2.getMessage().contains("contains more than 16 textures"),
        () -> "Got: " + ex2.getMessage());
  }

  @Test
  void rejectsInvalidManifestAndAnimationLimits() throws Exception {
    // 1. Manifest with more than 16 textures
    Path b1 = tempDirectory.resolve("manifest_17_tex.spectra.zip");
    StringBuilder texList = new StringBuilder();
    for (int i = 0; i < 17; i++) {
      if (i > 0) texList.append(", ");
      texList.append("\"textures/t").append(i).append(".png\"");
    }
    String m1 =
        """
        {
          "schemaVersion": 1,
          "modelId": "manifest_17_tex",
          "model": "model.bbmodel",
          "textures": [TEXTURES],
          "sha256": {"model.bbmodel": "abc"}
        }
        """
            .replace("TEXTURES", texList.toString());
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(b1))) {
      writeEntry(zip, "manifest.json", m1.getBytes(StandardCharsets.UTF_8));
    }
    var ex1 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(b1));
    Assertions.assertTrue(
        ex1.getMessage().contains("manifest.json contains more than 16 textures"),
        () -> "Got: " + ex1.getMessage());

    // 2. Manifest textures containing non-string
    Path b2 = tempDirectory.resolve("manifest_non_str_tex.spectra.zip");
    String m2 =
        """
        {
          "schemaVersion": 1,
          "modelId": "manifest_non_str",
          "model": "model.bbmodel",
          "textures": [123],
          "sha256": {"model.bbmodel": "abc"}
        }
        """;
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(b2))) {
      writeEntry(zip, "manifest.json", m2.getBytes(StandardCharsets.UTF_8));
    }
    var ex2 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(b2));
    Assertions.assertTrue(
        ex2.getMessage().contains("manifest.json textures must contain only strings"),
        () -> "Got: " + ex2.getMessage());

    // 3. Manifest sha256 containing non-string value
    Path b3 = tempDirectory.resolve("manifest_non_str_sha.spectra.zip");
    String m3 =
        """
        {
          "schemaVersion": 1,
          "modelId": "manifest_non_str_sha",
          "model": "model.bbmodel",
          "textures": ["textures/gem.png"],
          "sha256": {"model.bbmodel": 123}
        }
        """;
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(b3))) {
      writeEntry(zip, "manifest.json", m3.getBytes(StandardCharsets.UTF_8));
    }
    var ex3 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(b3));
    Assertions.assertTrue(
        ex3.getMessage().contains("manifest.json sha256 values must be strings"),
        () -> "Got: " + ex3.getMessage());

    // 4. Blockbench project with > 32 animations
    StringBuilder anims = new StringBuilder("[");
    for (int i = 0; i < 33; i++) {
      if (i > 0) anims.append(",");
      anims
          .append("{\"name\": \"anim_")
          .append(i)
          .append("\", \"length\": 1.0, \"animators\": {}}");
    }
    anims.append("]");
    String excessiveAnims =
        validProject().substring(0, validProject().indexOf("\"animations\":"))
            + "\"animations\": "
            + anims
            + "\n}";
    Path f4 = tempDirectory.resolve("many_anims.bbmodel");
    Files.writeString(f4, excessiveAnims);
    var ex4 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f4));
    Assertions.assertTrue(
        ex4.getMessage().contains("contains more than 32 animations"),
        () -> "Got: " + ex4.getMessage());

    // 5. Animation length <= 0
    String invalidLength = validProject().replace("\"length\": 1.0", "\"length\": 0.0");
    Path f5 = tempDirectory.resolve("invalid_length.bbmodel");
    Files.writeString(f5, invalidLength);
    var ex5 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f5));
    Assertions.assertTrue(
        ex5.getMessage().contains("length must be between 0 and 3600 seconds"),
        () -> "Got: " + ex5.getMessage());

    // 6. Animation targets unknown group
    String unknownGroup =
        validProject().replace("\"animators\": {\"root\":", "\"animators\": {\"ghost_group\":");
    Path f6 = tempDirectory.resolve("unknown_group.bbmodel");
    Files.writeString(f6, unknownGroup);
    var ex6 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f6));
    Assertions.assertTrue(
        ex6.getMessage().contains("targets unknown group ghost_group"),
        () -> "Got: " + ex6.getMessage());

    // 7. Duplicate texture id
    String dupTex =
        validProject()
            .replace(
                "\"textures\": [{\"id\": \"texture\", \"name\": \"gem.png\", \"source\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==\"}]",
                "\"textures\": [{\"id\": \"texture\", \"name\": \"gem.png\", \"source\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==\"}, {\"id\": \"texture\", \"name\": \"gem2.png\", \"source\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==\"}]");
    Path f7 = tempDirectory.resolve("dup_tex.bbmodel");
    Files.writeString(f7, dupTex);
    var ex7 = Assertions.assertThrows(IllegalArgumentException.class, () -> reader.read(f7));
    Assertions.assertTrue(
        ex7.getMessage().contains("contains duplicate texture id: texture"),
        () -> "Got: " + ex7.getMessage());

    // 8. Texture reference without hash
    String texNoHash =
        validProject().replace("\"texture\": \"#texture\"", "\"texture\": \"texture\"");
    Path f8 = tempDirectory.resolve("tex_no_hash.bbmodel");
    Files.writeString(f8, texNoHash);
    var doc8 = reader.read(f8);
    Assertions.assertEquals(
        "texture", doc8.nodes().getFirst().cubes().getFirst().faces().get("north").textureRef());
  }

  private static String validProject() {
    return projectJson("free");
  }

  private static String projectJson(String modelFormat) {
    return """
        {
          "meta": {"format_version": "5.0.0", "model_format": "MODEL_FORMAT"},
          "textures": [{"id": "texture", "name": "gem.png", "source": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg=="}],
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
        .replace("MODEL_FORMAT", modelFormat);
  }

  private static String embeddedJpegDataUri() throws IOException {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Assertions.assertTrue(ImageIO.write(image, "jpeg", output));
    return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
  }

  private static String embeddedPngDataUri(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Assertions.assertTrue(ImageIO.write(image, "png", output));
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
  }
}
