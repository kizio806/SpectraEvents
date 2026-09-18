package io.github.kizio806.spectraevents.core.visual.asset;

import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.Map;
import java.util.Objects;

/** Represents a single cube/cuboid in the asset geometry. */
public record SpectraAssetGeometry(
    Vector3 from,
    Vector3 to,
    Vector3 origin,
    EulerRotation rotation,
    double inflate,
    Map<String, SpectraAssetFace> faces) {

  public SpectraAssetGeometry {
    Objects.requireNonNull(from, "from cannot be null");
    Objects.requireNonNull(to, "to cannot be null");
    Objects.requireNonNull(origin, "origin cannot be null");
    Objects.requireNonNull(rotation, "rotation cannot be null");
    Objects.requireNonNull(faces, "faces cannot be null");

    faces = Map.copyOf(faces);
  }
}
