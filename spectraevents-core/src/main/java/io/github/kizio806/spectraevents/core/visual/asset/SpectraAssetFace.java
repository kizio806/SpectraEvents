package io.github.kizio806.spectraevents.core.visual.asset;

import java.util.Objects;

/** Represents a single textured face of a cube. */
public record SpectraAssetFace(double[] uv, String textureRef, int rotation) {
  public SpectraAssetFace {
    Objects.requireNonNull(uv, "uv cannot be null");
    if (uv.length != 4) {
      throw new IllegalArgumentException("UV must have exactly 4 elements");
    }
    if (textureRef == null || textureRef.isBlank()) {
      throw new IllegalArgumentException("textureRef cannot be null or blank");
    }
  }
}
