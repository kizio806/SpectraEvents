package io.github.kizio806.spectraevents.adapter.blockbench;

import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BlockbenchProjectReaderTest {

  private final BlockbenchProjectReader reader = new BlockbenchProjectReader();

  @Test
  void testParsesVersion5Format() {
    String json =
        """
      {
        "meta": {
          "format_version": "4.10.4"
        },
        "textures": [],
        "outliner": [],
        "animations": []
      }
    """;
    SpectraAssetDocument doc = reader.read(json, "test_model");
    Assertions.assertEquals("test_model", doc.modelId());
  }

  @Test
  void testParsesVersion4Format() {
    BlockbenchProjectReader reader = new BlockbenchProjectReader();
    String json =
        """
      {
        "meta": {
          "format_version": "4.10.4"
        },
        "textures": [],
        "outliner": [],
        "animations": []
      }
    """;
    SpectraAssetDocument doc = reader.read(json, "test_bb4");
    Assertions.assertEquals("test_bb4", doc.modelId());
  }

  @Test
  void testRejectsUnknownFormat() {
    String json =
        """
      {
        "meta": {
          "format_version": "6.0.0"
        },
        "textures": [],
        "outliner": [],
        "animations": []
      }
    """;
    Exception exception =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> reader.read(json, "test_model"));
    Assertions.assertTrue(
        exception.getMessage().contains("Unsupported Blockbench format version: 6.0.0"));
  }

  @Test
  void testRejectsUnsupportedInterpolation() {
    String json =
        """
      {
        "meta": {
          "format_version": "4.10.4"
        },
        "textures": [],
        "outliner": [],
        "animations": [
          {
            "name": "anim_test",
            "length": 1.0,
            "animators": {
              "bone": {
                "name": "bone",
                "keyframes": [
                  {
                    "channel": "rotation",
                    "interpolation": "catmullrom",
                    "data_points": [{"x":"0", "y":"0", "z":"0"}]
                  }
                ]
              }
            }
          }
        ]
      }
    """;
    Exception exception =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> reader.read(json, "test_model"));
    Assertions.assertTrue(exception.getMessage().contains("UNSUPPORTED_INTERPOLATION: catmullrom"));
  }

  @Test
  void testOversizedEmbeddedTextureRejected() {
    BlockbenchProjectReader reader = new BlockbenchProjectReader();
    StringBuilder sb = new StringBuilder();
    sb.append(
        "{\"meta\":{\"format_version\":\"5.0.0\"},\"textures\":[{\"id\":\"1\",\"name\":\"tex\",\"source\":\"data:image/png,");
    for (int i = 0; i < 2_000_001; i++) {
      sb.append("A"); // Build a base64 string > 2MB
    }
    sb.append("\"}]}");

    Exception ex =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> {
              reader.read(sb.toString(), "test");
            });
    Assertions.assertTrue(ex.getMessage().contains("exceeds size limit"));
  }
}
