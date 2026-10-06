package io.github.kizio806.spectraevents.adapter.blockbench;

import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Builds one deterministic resource-pack ZIP for every supported Minecraft release line. */
public final class ResourcePackReleaseBuilder {

  private ResourcePackReleaseBuilder() {}

  public static void main(String[] arguments) {
    if (arguments.length != 2) {
      throw new IllegalArgumentException(
          "Usage: ResourcePackReleaseBuilder <asset-source-directory> <output-directory>");
    }
    Path sourceDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
    Path outputDirectory = Path.of(arguments[1]).toAbsolutePath().normalize();
    if (!Files.isDirectory(sourceDirectory)) {
      throw new IllegalArgumentException(
          "Asset source directory does not exist: " + sourceDirectory);
    }

    for (AssetTargetProfile profile : AssetTargetProfile.values()) {
      new AssetPipelineService(
              new BlockbenchProjectReader(),
              new ResourcePackBuilder(outputDirectory),
              sourceDirectory,
              profile)
          .buildAssets();
    }
  }
}
