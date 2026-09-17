package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/** Platform-neutral item reference (e.g., "minecraft:magma_block", "nexo:meteor_core"). */
public record ItemAssetRef(String itemRef, DisplayTransformMode transformMode) {
  public ItemAssetRef {
    Objects.requireNonNull(itemRef, "itemRef cannot be null");
    Objects.requireNonNull(transformMode, "transformMode cannot be null");
    if (itemRef.isBlank()) {
      throw new IllegalArgumentException("itemRef cannot be blank");
    }
  }

  public static ItemAssetRef of(String itemRef) {
    return new ItemAssetRef(itemRef, DisplayTransformMode.NONE);
  }
}
