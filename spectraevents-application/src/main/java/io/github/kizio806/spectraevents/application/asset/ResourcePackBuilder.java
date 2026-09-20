package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

public class ResourcePackBuilder {
  public ResourcePackBuilder(Path outputDirectory) {
    Objects.requireNonNull(outputDirectory, "outputDirectory");
  }

  public void build(Collection<SpectraAssetDocument> documents, AssetTargetProfile profile)
      throws IOException {
    throw new UnsupportedOperationException(
        "Resource-pack generation is unavailable: the Blockbench compiler does not yet produce a verified ZIP");
  }
}
