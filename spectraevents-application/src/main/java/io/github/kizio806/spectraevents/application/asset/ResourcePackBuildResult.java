package io.github.kizio806.spectraevents.application.asset;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/** Immutable metadata for a verified local resource-pack artifact. */
public record ResourcePackBuildResult(
    Path zipPath,
    AssetTargetProfile profile,
    String sha1,
    String sha256,
    long sizeBytes,
    Map<String, Integer> customModelData) {

  public ResourcePackBuildResult {
    Objects.requireNonNull(zipPath, "zipPath");
    Objects.requireNonNull(profile, "profile");
    Objects.requireNonNull(sha1, "sha1");
    Objects.requireNonNull(sha256, "sha256");
    Objects.requireNonNull(customModelData, "customModelData");
    if (sizeBytes <= 0L) {
      throw new IllegalArgumentException("sizeBytes must be positive");
    }
    customModelData = Map.copyOf(customModelData);
  }
}
