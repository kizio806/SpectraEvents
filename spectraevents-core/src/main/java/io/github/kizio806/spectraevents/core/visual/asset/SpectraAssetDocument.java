package io.github.kizio806.spectraevents.core.visual.asset;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stable intermediate representation of an imported asset bundle (e.g. from Blockbench). This model
 * contains zero Blockbench-specific types and serves as the source of truth for generating both
 * runtime ModelDefinitions and resource pack components.
 */
public record SpectraAssetDocument(
    int schemaVersion,
    String modelId,
    Map<String, SpectraAssetTexture> textures,
    List<SpectraAssetNode> nodes,
    Map<String, SpectraAssetAnimation> animations) {

  public SpectraAssetDocument {
    Objects.requireNonNull(modelId, "modelId cannot be null");
    Objects.requireNonNull(textures, "textures cannot be null");
    Objects.requireNonNull(nodes, "nodes cannot be null");
    Objects.requireNonNull(animations, "animations cannot be null");
    if (modelId.isBlank()) {
      throw new IllegalArgumentException("modelId cannot be blank");
    }
    textures = Map.copyOf(textures);
    nodes = List.copyOf(nodes);
    animations = Map.copyOf(animations);
  }
}
