package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AssetPipelineService {

  private static final Logger LOGGER = Logger.getLogger(AssetPipelineService.class.getName());
  private final AssetImportPort importPort;
  private final ModelDefinitionRegistry modelRegistry;
  private final AnimationDefinitionRegistry animationRegistry;
  private final ResourcePackBuilder resourcePackBuilder;
  private final Path sourceDirectory;

  // Simple incremental cache mapping filename to SHA-256 hash of source
  private final Map<String, String> sourceCache = new HashMap<>();

  public AssetPipelineService(
      AssetImportPort importPort,
      ModelDefinitionRegistry modelRegistry,
      AnimationDefinitionRegistry animationRegistry,
      ResourcePackBuilder resourcePackBuilder,
      Path sourceDirectory) {
    this.importPort = importPort;
    this.modelRegistry = modelRegistry;
    this.animationRegistry = animationRegistry;
    this.resourcePackBuilder = resourcePackBuilder;
    this.sourceDirectory = sourceDirectory;
  }

  public void buildAssets() {
    LOGGER.info("Starting Asset Pipeline build...");
    if (!Files.exists(sourceDirectory)) {
      LOGGER.info("Source directory does not exist: " + sourceDirectory);
      return;
    }

    try {
      boolean changesDetected = false;
      Map<String, SpectraAssetDocument> compiledDocuments = new HashMap<>();

      Files.walk(sourceDirectory)
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".bbmodel") || p.toString().endsWith(".spectra.zip"))
          .forEach(
              path -> {
                try {
                  String filename = path.getFileName().toString();
                  String currentHash = computeSha256(path);

                  if (currentHash.equals(sourceCache.get(filename))) {
                    LOGGER.fine("Skipping unchanged asset source: " + filename);
                    return;
                  }

                  LOGGER.info("Compiling asset source: " + filename);
                  String content = Files.readString(path);

                  // Extract model ID from filename (remove extension)
                  String modelIdStr = filename.replace(".bbmodel", "").replace(".spectra.zip", "");

                  SpectraAssetDocument doc = importPort.read(content, modelIdStr);
                  compiledDocuments.put(modelIdStr, doc);
                  sourceCache.put(filename, currentHash);

                } catch (Exception e) {
                  LOGGER.severe("Failed to compile asset source " + path + ": " + e.getMessage());
                }
              });

      if (!compiledDocuments.isEmpty()) {
        LOGGER.info("Rebuilding resource pack...");
        resourcePackBuilder.build(compiledDocuments.values());

        // TODO: Generate and register ModelDefinition / AnimationDefinition to registries

        LOGGER.info("Asset Pipeline build complete. Resource pack generated.");
      } else {
        LOGGER.info("No asset changes detected. Incremental build skipped.");
      }

    } catch (IOException e) {
      LOGGER.severe("Failed to walk source directory: " + e.getMessage());
    }
  }

  private String computeSha256(Path path) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] bytes = Files.readAllBytes(path);
    byte[] hash = digest.digest(bytes);
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
