package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/** Immutable compiled definition of a single 3D model visual display part. */
public record ModelPartDefinition(
    ModelPartId partId,
    ModelPartId parentPartId,
    ModelPartType type,
    ModelTransform localTransform,
    ModelTransform composedTransform,
    ModelRenderProperties renderProperties,
    Object visualAsset) {

  public ModelPartDefinition {
    Objects.requireNonNull(partId, "partId cannot be null");
    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(localTransform, "localTransform cannot be null");
    Objects.requireNonNull(composedTransform, "composedTransform cannot be null");
    Objects.requireNonNull(renderProperties, "renderProperties cannot be null");
    Objects.requireNonNull(visualAsset, "visualAsset cannot be null");

    // Validate asset type matches part type
    switch (type) {
      case ITEM_DISPLAY -> {
        if (!(visualAsset instanceof ItemAssetRef)) {
          throw new IllegalArgumentException(
              "ITEM_DISPLAY part must specify ItemAssetRef, found: "
                  + visualAsset.getClass().getName());
        }
      }
      case BLOCK_DISPLAY -> {
        if (!(visualAsset instanceof BlockAssetRef)) {
          throw new IllegalArgumentException(
              "BLOCK_DISPLAY part must specify BlockAssetRef, found: "
                  + visualAsset.getClass().getName());
        }
      }
      case TEXT_DISPLAY -> {
        if (!(visualAsset instanceof TextAssetRef)) {
          throw new IllegalArgumentException(
              "TEXT_DISPLAY part must specify TextAssetRef, found: "
                  + visualAsset.getClass().getName());
        }
      }
      default -> {}
    }
  }
}
