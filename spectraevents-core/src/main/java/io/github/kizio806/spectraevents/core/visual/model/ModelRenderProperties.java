package io.github.kizio806.spectraevents.core.visual.model;

import java.util.Objects;

/** Platform-neutral render properties for Display entities. */
public record ModelRenderProperties(
    BillboardMode billboard,
    int brightnessBlock,
    int brightnessSky,
    float shadowRadius,
    float shadowStrength,
    float viewRange,
    float displayWidth,
    float displayHeight,
    String glowColor,
    int interpolationDelayTicks,
    int interpolationDurationTicks,
    int teleportDurationTicks) {

  public static final ModelRenderProperties DEFAULT =
      new ModelRenderProperties(
          BillboardMode.FIXED, -1, -1, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, null, 0, 0, 0);

  public ModelRenderProperties {
    Objects.requireNonNull(billboard, "billboard mode cannot be null");
    if (shadowRadius < 0.0f || shadowStrength < 0.0f) {
      throw new IllegalArgumentException("Shadow properties cannot be negative");
    }
    if (viewRange <= 0.0f) {
      throw new IllegalArgumentException("viewRange must be positive");
    }
    if (interpolationDelayTicks < 0
        || interpolationDurationTicks < 0
        || teleportDurationTicks < 0) {
      throw new IllegalArgumentException("Interpolation ticks cannot be negative");
    }
  }
}
