package io.github.kizio806.spectraevents.core.visual.asset;

import java.util.Map;
import java.util.Objects;

/**
 * Manifest for a compiled asset bundle, providing versioning, generator information, and checksums
 * to track provenance and enable caching.
 */
public record SpectraAssetManifest(
    int schemaVersion,
    String generator,
    String generatorVersion,
    String modelId,
    String sourceFormat,
    String sourceFormatVersion,
    String createdWithBlockbenchVersion,
    Map<String, String> checksums) {

  public SpectraAssetManifest {
    Objects.requireNonNull(generator, "generator cannot be null");
    Objects.requireNonNull(generatorVersion, "generatorVersion cannot be null");
    Objects.requireNonNull(modelId, "modelId cannot be null");
    Objects.requireNonNull(sourceFormat, "sourceFormat cannot be null");
    Objects.requireNonNull(sourceFormatVersion, "sourceFormatVersion cannot be null");
    Objects.requireNonNull(
        createdWithBlockbenchVersion, "createdWithBlockbenchVersion cannot be null");
    Objects.requireNonNull(checksums, "checksums cannot be null");

    checksums = Map.copyOf(checksums);
  }
}
