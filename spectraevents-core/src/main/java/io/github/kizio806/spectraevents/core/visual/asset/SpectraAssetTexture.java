package io.github.kizio806.spectraevents.core.visual.asset;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** Represents a texture mapped to the asset. */
public final class SpectraAssetTexture {
  private final String textureName;
  private final byte[] embeddedData;
  private final String textureSourcePath;

  public SpectraAssetTexture(String name, byte[] data, String sourcePath) {
    this.textureName = Objects.requireNonNull(name, "name cannot be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name cannot be blank");
    }
    if (data == null && (sourcePath == null || sourcePath.isBlank())) {
      throw new IllegalArgumentException("Texture must have either embedded data or a sourcePath");
    }
    this.embeddedData = data == null ? null : Arrays.copyOf(data, data.length);
    this.textureSourcePath = sourcePath;
  }

  public String name() {
    return textureName;
  }

  public Optional<byte[]> data() {
    return embeddedData == null
        ? Optional.empty()
        : Optional.of(Arrays.copyOf(embeddedData, embeddedData.length));
  }

  public String sourcePath() {
    return textureSourcePath;
  }
}
