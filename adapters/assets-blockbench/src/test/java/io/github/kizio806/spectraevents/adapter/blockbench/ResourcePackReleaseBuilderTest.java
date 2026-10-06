package io.github.kizio806.spectraevents.adapter.blockbench;

import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcePackReleaseBuilderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void buildsOnePackForEverySupportedMinecraftReleaseLine() throws Exception {
    Path sourceDirectory = Files.createDirectory(temporaryDirectory.resolve("source"));
    Path outputDirectory = temporaryDirectory.resolve("output");
    Files.writeString(sourceDirectory.resolve("meteor.bbmodel"), validProject());

    ResourcePackReleaseBuilder.main(
        new String[] {sourceDirectory.toString(), outputDirectory.toString()});

    for (AssetTargetProfile profile : AssetTargetProfile.values()) {
      Path pack =
          outputDirectory.resolve(
              "spectraevents-" + profile.name().toLowerCase(Locale.ROOT) + ".zip");
      Assertions.assertTrue(Files.isRegularFile(pack), () -> "Missing generated pack " + pack);
      Assertions.assertTrue(Files.size(pack) > 0);
    }
  }

  private static String validProject() {
    return """
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
          "outliner": [{"uuid": "root", "name": "Root", "origin": [8, 8, 8], "children": ["cube"]}]
        }
        """;
  }
}
