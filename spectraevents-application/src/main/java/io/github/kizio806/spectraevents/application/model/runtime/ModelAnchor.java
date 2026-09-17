package io.github.kizio806.spectraevents.application.model.runtime;

import java.util.Objects;

/** Platform-neutral spatial anchor defining world position and orientation for a spawned model. */
public record ModelAnchor(String worldName, double x, double y, double z, float yaw, float pitch) {
  public ModelAnchor {
    Objects.requireNonNull(worldName, "worldName cannot be null");
    if (worldName.isBlank()) {
      throw new IllegalArgumentException("worldName cannot be blank");
    }
  }

  public static ModelAnchor of(String worldName, double x, double y, double z) {
    return new ModelAnchor(worldName, x, y, z, 0.0f, 0.0f);
  }

  public static ModelAnchor of(
      String worldName, double x, double y, double z, float yaw, float pitch) {
    return new ModelAnchor(worldName, x, y, z, yaw, pitch);
  }
}
