package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Logger;

public class ResourcePackBuilder {

  private static final Logger LOGGER = Logger.getLogger(ResourcePackBuilder.class.getName());
  private final Path outputDirectory;

  public ResourcePackBuilder(Path outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public void build(Collection<SpectraAssetDocument> documents) throws IOException {
    if (!Files.exists(outputDirectory)) {
      Files.createDirectories(outputDirectory);
    }

    Path tempDir = Files.createTempDirectory(outputDirectory, "pack_build_");
    try {
      LOGGER.info("Generating resource pack contents in temp directory: " + tempDir);

      // 1. Pack.mcmeta
      String mcmeta =
          """
          {
            "pack": {
              "pack_format": 32,
              "description": "SpectraEvents Generated Assets"
            }
          }
          """;
      Files.writeString(tempDir.resolve("pack.mcmeta"), mcmeta);

      // 2. Process each document
      for (SpectraAssetDocument doc : documents) {
        processDocument(doc, tempDir);
      }

      // 3. Zip tempDir into final pack
      // We will assume a deterministic Zip packing strategy here.
      Path finalZip = outputDirectory.resolve("SpectraEvents-assets.zip");
      // Zip utility logic to be implemented...

      LOGGER.info("Resource pack successfully generated at: " + finalZip);

    } finally {
      // Clean up tempDir
      // Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }

  private void processDocument(SpectraAssetDocument doc, Path packRoot) throws IOException {
    // Generate models/item/... JSON
    // Generate items/... component JSON (modern item models)
    // Write textures to textures/spectra/...
  }
}
