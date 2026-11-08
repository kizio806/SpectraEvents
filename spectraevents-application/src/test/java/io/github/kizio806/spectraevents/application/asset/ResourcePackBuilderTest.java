package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetFace;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetGeometry;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetNode;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetTexture;
import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcePackBuilderTest {
  private static final byte[] TEXTURE =
      java.util.Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAF/gL+I0Yf9wAAAABJRU5ErkJggg==");

  @TempDir Path tempDirectory;

  @Test
  void buildsInspectableResourcePackWithModelsTexturesMappingsAndHashes() throws Exception {
    ResourcePackBuildResult result =
        new ResourcePackBuilder(tempDirectory)
            .build(List.of(document("meteor")), AssetTargetProfile.PROFILE_26_2);

    Assertions.assertTrue(java.nio.file.Files.isRegularFile(result.zipPath()));
    Assertions.assertEquals(40, result.sha1().length());
    Assertions.assertEquals(64, result.sha256().length());
    Assertions.assertEquals(
        GeneratedAssetItem.customModelData("meteor", "root"),
        result.customModelData().get("meteor/root"));

    try (ZipFile zip = new ZipFile(result.zipPath().toFile())) {
      Assertions.assertNotNull(zip.getEntry("pack.mcmeta"));
      Assertions.assertNull(zip.getEntry("assets/minecraft/items/paper.json"));
      Assertions.assertNotNull(
          zip.getEntry("assets/spectraevents/textures/item/meteor/texture_0.png"));
      Assertions.assertNotNull(zip.getEntry("assets/spectraevents/models/item/meteor/root.json"));
      Assertions.assertNotNull(zip.getEntry("assets/spectraevents/items/meteor/root.json"));
      Assertions.assertNotNull(zip.getEntry("assets/spectraevents/spectraevents-manifest.json"));
      Assertions.assertTrue(read(zip, "pack.mcmeta").contains("[88,0]"));
      Assertions.assertTrue(
          read(zip, "assets/spectraevents/models/item/meteor/root.json").contains("elements"));
      Assertions.assertTrue(
          read(zip, "assets/spectraevents/items/meteor/root.json")
              .contains("spectraevents:item/meteor/root"));
      Assertions.assertTrue(
          read(zip, "assets/spectraevents/spectraevents-manifest.json").contains("meteor/root"));
    }
  }

  @Test
  void rejectsDuplicateImportedModelIds() {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDirectory);

    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () ->
                builder.build(
                    List.of(document("meteor"), document("meteor")),
                    AssetTargetProfile.PROFILE_26_1));

    Assertions.assertTrue(error.getMessage().contains("duplicate"));
  }

  @Test
  void writesIndependentNativeItemDefinitionsForEachGeneratedModelPart() throws Exception {
    ResourcePackBuildResult result =
        new ResourcePackBuilder(tempDirectory)
            .build(
                List.of(document("meteor"), document("nebula")), AssetTargetProfile.PROFILE_26_2);

    try (ZipFile zip = new ZipFile(result.zipPath().toFile())) {
      Assertions.assertTrue(
          read(zip, "assets/spectraevents/items/meteor/root.json")
              .contains("spectraevents:item/meteor/root"));
      Assertions.assertTrue(
          read(zip, "assets/spectraevents/items/nebula/root.json")
              .contains("spectraevents:item/nebula/root"));
    }
  }

  @Test
  void rejectsEmptyDocumentsOrNullArguments() {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDirectory);
    Assertions.assertThrows(
        NullPointerException.class, () -> builder.build(null, AssetTargetProfile.PROFILE_26_1));
    Assertions.assertThrows(NullPointerException.class, () -> builder.build(List.of(), null));
    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> builder.build(List.of(), AssetTargetProfile.PROFILE_26_1));
    Assertions.assertTrue(error.getMessage().contains("without imported asset documents"));
  }

  @Test
  void rejectsInvalidModelIdOrNodeIdSegments() {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDirectory);

    // Invalid model id
    SpectraAssetDocument badModel =
        new SpectraAssetDocument(
            1,
            "Invalid_Model!",
            Map.of("texture", new SpectraAssetTexture("texture.png", TEXTURE, null)),
            List.of(
                new SpectraAssetNode(
                    "root",
                    Vector3.ZERO,
                    Vector3.ZERO,
                    EulerRotation.ZERO,
                    Vector3.ONE,
                    List.of(),
                    List.of())),
            Map.of());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> builder.build(List.of(badModel), AssetTargetProfile.PROFILE_26_1));

    // Invalid node id
    SpectraAssetDocument badNode =
        new SpectraAssetDocument(
            1,
            "valid_model",
            Map.of("texture", new SpectraAssetTexture("texture.png", TEXTURE, null)),
            List.of(
                new SpectraAssetNode(
                    "Bad:Node!",
                    Vector3.ZERO,
                    Vector3.ZERO,
                    EulerRotation.ZERO,
                    Vector3.ONE,
                    List.of(),
                    List.of())),
            Map.of());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> builder.build(List.of(badNode), AssetTargetProfile.PROFILE_26_1));
  }

  @Test
  void rejectsMissingTextureEmbeddedBytes() {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDirectory);
    SpectraAssetDocument doc =
        new SpectraAssetDocument(
            1,
            "meteor",
            Map.of(
                "missing_tex",
                new SpectraAssetTexture("texture.png", null, "external/texture.png")),
            List.of(
                new SpectraAssetNode(
                    "root",
                    Vector3.ZERO,
                    Vector3.ZERO,
                    EulerRotation.ZERO,
                    Vector3.ONE,
                    List.of(),
                    List.of())),
            Map.of());
    IllegalArgumentException ex =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> builder.build(List.of(doc), AssetTargetProfile.PROFILE_26_1));
    Assertions.assertTrue(ex.getMessage().contains("must contain embedded bytes"));
  }

  @Test
  void buildsWithRotationsMultipleTexturesAndCubesAndChildren() throws Exception {
    SpectraAssetFace faceX = new SpectraAssetFace(List.of(0.5, 1.5, 15.5, 15.5), "tex1", 90);
    SpectraAssetFace faceY = new SpectraAssetFace(List.of(0.0, 0.0, 16.0, 16.0), "tex2", 0);

    SpectraAssetGeometry cubeRotX =
        new SpectraAssetGeometry(
            new Vector3(1.5f, 2.5f, 3.5f),
            new Vector3(16.0f, 16.0f, 16.0f),
            new Vector3(8.0f, 8.0f, 8.0f),
            new EulerRotation(22.5f, 0.0f, 0.0f),
            0.0,
            Map.of("north", faceX, "south", faceY));

    SpectraAssetGeometry cubeRotY =
        new SpectraAssetGeometry(
            new Vector3(0.0f, 0.0f, 0.0f),
            new Vector3(10.0f, 10.0f, 10.0f),
            new Vector3(5.0f, 5.0f, 5.0f),
            new EulerRotation(0.0f, 45.0f, 0.0f),
            0.0,
            Map.of("west", faceX));

    SpectraAssetGeometry cubeRotZ =
        new SpectraAssetGeometry(
            new Vector3(0.0f, 0.0f, 0.0f),
            new Vector3(8.0f, 8.0f, 8.0f),
            new Vector3(4.0f, 4.0f, 4.0f),
            new EulerRotation(0.0f, 0.0f, -22.5f),
            0.0,
            Map.of("east", faceY));

    SpectraAssetNode childNode =
        new SpectraAssetNode(
            "child_arm",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(cubeRotZ),
            List.of());

    SpectraAssetNode rootNode =
        new SpectraAssetNode(
            "root",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(cubeRotX, cubeRotY),
            List.of(childNode));

    SpectraAssetDocument doc =
        new SpectraAssetDocument(
            1,
            "complex_model",
            Map.of(
                "tex1", new SpectraAssetTexture("tex1.png", TEXTURE, null),
                "tex2", new SpectraAssetTexture("tex2.png", TEXTURE, null)),
            List.of(rootNode),
            Map.of());

    ResourcePackBuildResult result =
        new ResourcePackBuilder(tempDirectory).build(List.of(doc), AssetTargetProfile.PROFILE_26_3);

    Assertions.assertNotNull(result);
    try (ZipFile zip = new ZipFile(result.zipPath().toFile())) {
      String modelJson = read(zip, "assets/spectraevents/models/item/complex_model/root.json");
      Assertions.assertTrue(modelJson.contains("\"axis\":\"x\""));
      Assertions.assertTrue(modelJson.contains("\"axis\":\"y\""));
      Assertions.assertTrue(modelJson.contains("\"rotation\":90"));
      String childJson = read(zip, "assets/spectraevents/models/item/complex_model/child_arm.json");
      Assertions.assertTrue(childJson.contains("\"axis\":\"z\""));
    }
  }

  @Test
  void rejectsDuplicateNodeIdsWithinSameModel() {
    ResourcePackBuilder builder = new ResourcePackBuilder(tempDirectory);
    SpectraAssetNode duplicateChild =
        new SpectraAssetNode(
            "root",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(),
            List.of());
    SpectraAssetNode root =
        new SpectraAssetNode(
            "root",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(),
            List.of(duplicateChild));
    SpectraAssetDocument doc =
        new SpectraAssetDocument(
            1,
            "meteor",
            Map.of("tex", new SpectraAssetTexture("tex.png", TEXTURE, null)),
            List.of(root),
            Map.of());

    IllegalArgumentException ex =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> builder.build(List.of(doc), AssetTargetProfile.PROFILE_26_1));
    Assertions.assertTrue(ex.getMessage().contains("collision"));
  }

  @Test
  void rejectsNonFiniteNumberInGeometry() {
    SpectraAssetFace face = new SpectraAssetFace(List.of(Double.NaN, 0.0, 16.0, 16.0), "tex", 0);
    SpectraAssetGeometry cube =
        new SpectraAssetGeometry(
            Vector3.ZERO, Vector3.ONE, Vector3.ZERO, EulerRotation.ZERO, 0.0, Map.of("up", face));
    SpectraAssetNode root =
        new SpectraAssetNode(
            "root",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(cube),
            List.of());
    SpectraAssetDocument doc =
        new SpectraAssetDocument(
            1,
            "nan_model",
            Map.of("tex", new SpectraAssetTexture("tex.png", TEXTURE, null)),
            List.of(root),
            Map.of());

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            new ResourcePackBuilder(tempDirectory)
                .build(List.of(doc), AssetTargetProfile.PROFILE_26_1));
  }

  private static String read(ZipFile zip, String path) throws Exception {
    try (InputStream input = zip.getInputStream(zip.getEntry(path))) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static SpectraAssetDocument document(String modelId) {
    SpectraAssetGeometry cube =
        new SpectraAssetGeometry(
            new Vector3(0.0f, 0.0f, 0.0f),
            new Vector3(16.0f, 16.0f, 16.0f),
            new Vector3(8.0f, 8.0f, 8.0f),
            EulerRotation.ZERO,
            0.0,
            Map.of("north", new SpectraAssetFace(List.of(0.0, 0.0, 16.0, 16.0), "texture", 0)));
    SpectraAssetNode root =
        new SpectraAssetNode(
            "root",
            Vector3.ZERO,
            Vector3.ZERO,
            EulerRotation.ZERO,
            Vector3.ONE,
            List.of(cube),
            List.of());
    return new SpectraAssetDocument(
        1,
        modelId,
        Map.of("texture", new SpectraAssetTexture("texture.png", TEXTURE, null)),
        List.of(root),
        Map.of());
  }
}
