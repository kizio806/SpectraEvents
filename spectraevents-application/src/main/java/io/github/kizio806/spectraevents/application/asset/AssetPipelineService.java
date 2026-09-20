package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class AssetPipelineService {

  private static final Logger LOGGER = Logger.getLogger(AssetPipelineService.class.getName());
  private static final String SPECTRA_BUNDLE_EXTENSION = ".spectra.zip";
  private final AssetImportPort importPort;
  private final ResourcePackBuilder resourcePackBuilder;
  private final Path sourceDirectory;
  private final AssetTargetProfile targetProfile;

  private final Map<String, String> sourceCache = new HashMap<>();
  private final Map<String, SpectraAssetDocument> compiledDocuments = new HashMap<>();

  public AssetPipelineService(
      AssetImportPort importPort,
      ResourcePackBuilder resourcePackBuilder,
      Path sourceDirectory,
      AssetTargetProfile targetProfile) {
    this.importPort = Objects.requireNonNull(importPort, "importPort");
    this.resourcePackBuilder = Objects.requireNonNull(resourcePackBuilder, "resourcePackBuilder");
    this.sourceDirectory =
        Objects.requireNonNull(sourceDirectory, "sourceDirectory").toAbsolutePath().normalize();
    this.targetProfile = Objects.requireNonNull(targetProfile, "targetProfile");
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

      try (java.util.stream.Stream<Path> stream = Files.walk(sourceDirectory)) {
        stream
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(SPECTRA_BUNDLE_EXTENSION))
            .forEach(
                path -> {
                  try {
                    if (importSource(path)) {
                      changesDetected.set(true);
                    }

                  } catch (Exception e) {
                    LOGGER.severe("Failed to compile asset source " + path + ": " + e.getMessage());
                  }
                });
      }

      if (changesDetected.get() && !compiledDocuments.isEmpty()) {
        LOGGER.info("Rebuilding resource pack...");
        resourcePackBuilder.build(compiledDocuments.values(), targetProfile);
        LOGGER.info("Asset Pipeline build complete. Resource pack generated.");
      } else {
        LOGGER.info("No asset changes detected or no documents. Incremental build skipped.");
      }

    } catch (IOException e) {
      LOGGER.severe("Failed to walk source directory: " + e.getMessage());
    }
  }

  public void importFile(String filename) {
    Objects.requireNonNull(filename, "filename");
    Path path = sourceDirectory.resolve(filename).toAbsolutePath().normalize();
    if (!path.startsWith(sourceDirectory)
        || path.getParent() == null
        || !path.getParent().equals(sourceDirectory)) {
      throw new SecurityException("Path traversal attempt detected: " + filename);
    }

    if (!Files.isRegularFile(path) || !filename.endsWith(SPECTRA_BUNDLE_EXTENSION)) {
      throw new IllegalArgumentException("File not found or invalid format: " + filename);
    }

    try {
      importSource(path);
    } catch (Exception e) {
      throw new RuntimeException("Import failed: " + e.getMessage(), e);
    }
  }

  private boolean importSource(Path source) throws Exception {
    String cacheKey = source.getFileName().toString();
    String currentHash = computeSha256(source);
    if (currentHash.equals(sourceCache.get(cacheKey))) {
      LOGGER.fine("Skipping unchanged asset source: " + cacheKey);
      return false;
    }

    SpectraAssetDocument document = importPort.read(source);
    compiledDocuments.put(document.modelId(), document);
    sourceCache.put(cacheKey, currentHash);
    return true;
  }

  public boolean validateModel(String modelId) {
    if (!compiledDocuments.containsKey(modelId)) return false;
    SpectraAssetDocument doc = compiledDocuments.get(modelId);
    // Simple validation rule checks
    if (doc.nodes().isEmpty()) return false;
    return true;
  }

  public Collection<String> listModels() {
    return List.copyOf(compiledDocuments.keySet());
  }

  public SpectraAssetDocument getModelInfo(String modelId) {
    return compiledDocuments.get(modelId);
  }

  public void clean() {
    sourceCache.clear();
    compiledDocuments.clear();
  }

  private String computeSha256(Path path) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (java.io.InputStream input = Files.newInputStream(path)) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
    }
    byte[] hash = digest.digest();
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
