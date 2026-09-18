package io.github.kizio806.spectraevents.core.visual.asset;

import io.github.kizio806.spectraevents.core.visual.model.EulerRotation;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import java.util.List;
import java.util.Objects;

/**
 * Represents a hierarchical bone or group in the asset model. Contains pivot points, initial
 * transforms, and geometry.
 */
public record SpectraAssetNode(
    String nodeId,
    Vector3 pivot,
    Vector3 translation,
    EulerRotation rotation,
    Vector3 scale,
    List<SpectraAssetGeometry> cubes,
    List<SpectraAssetNode> children) {

  public SpectraAssetNode {
    Objects.requireNonNull(nodeId, "nodeId cannot be null");
    Objects.requireNonNull(pivot, "pivot cannot be null");
    Objects.requireNonNull(translation, "translation cannot be null");
    Objects.requireNonNull(rotation, "rotation cannot be null");
    Objects.requireNonNull(scale, "scale cannot be null");
    Objects.requireNonNull(cubes, "cubes cannot be null");
    Objects.requireNonNull(children, "children cannot be null");

    if (nodeId.isBlank()) {
      throw new IllegalArgumentException("nodeId cannot be blank");
    }

    cubes = List.copyOf(cubes);
    children = List.copyOf(children);
  }
}
