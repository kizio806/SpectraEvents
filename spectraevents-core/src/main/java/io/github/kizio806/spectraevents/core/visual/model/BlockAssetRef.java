package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/**
 * Platform-neutral block state reference (e.g., "minecraft:stone", "minecraft:oak_log[axis=y]").
 */
public record BlockAssetRef(String blockStateRef) {
  public BlockAssetRef {
    Objects.requireNonNull(blockStateRef, "blockStateRef cannot be null");
    if (blockStateRef.isBlank()) {
      throw new IllegalArgumentException("blockStateRef cannot be blank");
    }
  }
}
