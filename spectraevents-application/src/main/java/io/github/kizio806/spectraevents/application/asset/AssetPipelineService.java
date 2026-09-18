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

  private final Map<String, SpectraAssetDocument> compiledDocuments = new HashMap<>();

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
      java.util.concurrent.atomic.AtomicBoolean changesDetected =
          new java.util.concurrent.atomic.AtomicBoolean(false);

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
                  changesDetected.set(true);

                } catch (Exception e) {
                  LOGGER.severe("Failed to compile asset source " + path + ": " + e.getMessage());
                }
              });

      if (changesDetected.get() && !compiledDocuments.isEmpty()) {
        LOGGER.info("Rebuilding resource pack...");
        resourcePackBuilder.build(compiledDocuments.values());
        LOGGER.info("Asset Pipeline build complete. Resource pack generated.");
      } else {
        LOGGER.info("No asset changes detected or no documents. Incremental build skipped.");
      }

    } catch (IOException e) {
      LOGGER.severe("Failed to walk source directory: " + e.getMessage());
    }
  }

  public void importFile(String filename) {
    Path path = sourceDirectory.resolve(filename);
    if (!Files.exists(path)
        || (!filename.endsWith(".bbmodel") && !filename.endsWith(".spectra.zip"))) {
      throw new IllegalArgumentException("File not found or invalid format: " + filename);
    }

    try {
      String currentHash = computeSha256(path);
      String content = Files.readString(path);
      String modelIdStr = filename.replace(".bbmodel", "").replace(".spectra.zip", "");
      SpectraAssetDocument doc = importPort.read(content, modelIdStr);
      compiledDocuments.put(modelIdStr, doc);
      sourceCache.put(filename, currentHash);
    } catch (Exception e) {
      throw new RuntimeException("Import failed: " + e.getMessage(), e);
    }
  }

  public boolean validateModel(String modelId) {
    if (!compiledDocuments.containsKey(modelId)) return false;
    SpectraAssetDocument doc = compiledDocuments.get(modelId);
    // Simple validation rule checks
    if (doc.nodes().isEmpty()) return false;
    return true;
  }

  public java.util.Collection<String> listModels() {
    return compiledDocuments.keySet();
  }

  public SpectraAssetDocument getModelInfo(String modelId) {
    return compiledDocuments.get(modelId);
  }

  public void clean() {
    sourceCache.clear();
    compiledDocuments.clear();
    try {
      Path genPath = sourceDirectory.getParent().getParent().resolve("generated");
      if (Files.exists(genPath)) {
        Files.walk(genPath)
            .sorted(java.util.Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(java.io.File::delete);
      }
    } catch (IOException e) {
      LOGGER.warning("Failed to clean generated output: " + e.getMessage());
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
