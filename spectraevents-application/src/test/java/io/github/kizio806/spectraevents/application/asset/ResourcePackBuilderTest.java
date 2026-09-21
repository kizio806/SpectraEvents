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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
      Assertions.assertNotNull(zip.getEntry("assets/minecraft/items/paper.json"));
      Assertions.assertNotNull(
          zip.getEntry("assets/spectraevents/textures/item/meteor/texture_0.png"));
      Assertions.assertNotNull(zip.getEntry("assets/spectraevents/models/item/meteor/root.json"));
      Assertions.assertNotNull(zip.getEntry("assets/spectraevents/spectraevents-manifest.json"));
      Assertions.assertTrue(read(zip, "pack.mcmeta").contains("[88,0]"));
      Assertions.assertTrue(
          read(zip, "assets/minecraft/items/paper.json").contains("custom_model_data"));
      Assertions.assertTrue(
          read(zip, "assets/minecraft/items/paper.json")
              .contains(Integer.toString(result.customModelData().get("meteor/root"))));
      Assertions.assertTrue(
          read(zip, "assets/spectraevents/models/item/meteor/root.json").contains("elements"));
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
  void ordersRangeDispatchThresholdsAscending() throws Exception {
    ResourcePackBuildResult result =
        new ResourcePackBuilder(tempDirectory)
            .build(
                List.of(document("meteor"), document("nebula")), AssetTargetProfile.PROFILE_26_2);

    try (ZipFile zip = new ZipFile(result.zipPath().toFile())) {
      String mapping = read(zip, "assets/minecraft/items/paper.json");
      Matcher thresholds = Pattern.compile("\\\"threshold\\\":(\\d+)").matcher(mapping);
      Assertions.assertTrue(thresholds.find());
      int first = Integer.parseInt(thresholds.group(1));
      Assertions.assertTrue(thresholds.find());
      int second = Integer.parseInt(thresholds.group(1));
      Assertions.assertTrue(first < second);
    }
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
