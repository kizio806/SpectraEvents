package io.github.kizio806.spectraevents.core.visual.asset;

import java.util.List;
import java.util.Objects;

/** Represents a single textured face of a cube. */
public record SpectraAssetFace(List<Double> uv, String textureRef, int rotation) {
  public SpectraAssetFace {
    Objects.requireNonNull(uv, "uv cannot be null");
    if (uv.size() != 4) {
      throw new IllegalArgumentException("UV must have exactly 4 elements");
    }
    if (textureRef == null || textureRef.isBlank()) {
      throw new IllegalArgumentException("textureRef cannot be null or blank");
    }
    uv = List.copyOf(uv);
  }
}
