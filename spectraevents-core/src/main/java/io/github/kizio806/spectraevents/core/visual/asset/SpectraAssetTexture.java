package io.github.kizio806.spectraevents.core.visual.asset;

import java.util.Objects;

/** Represents a texture mapped to the asset. */
public record SpectraAssetTexture(String name, byte[] data, String sourcePath) {
  public SpectraAssetTexture {
    Objects.requireNonNull(name, "name cannot be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name cannot be blank");
    }
    if (data == null && (sourcePath == null || sourcePath.isBlank())) {
      throw new IllegalArgumentException("Texture must have either embedded data or a sourcePath");
    }
  }
}
