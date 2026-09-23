package io.github.kizio806.spectraevents.application.asset;

import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class AssetPipelineService {

  private static final Logger LOGGER = Logger.getLogger(AssetPipelineService.class.getName());
  private static final String SPECTRA_BUNDLE_EXTENSION = ".spectra.zip";
  private static final String BLOCKBENCH_EXTENSION = ".bbmodel";
  private final AssetImportPort importPort;
  private final ResourcePackBuilder resourcePackBuilder;
  private final Path sourceDirectory;
  private final AssetTargetProfile targetProfile;
  private final ImportedAssetModelRegistrar modelRegistrar;

  private final Map<String, String> sourceCache = new HashMap<>();
  private final Map<String, SpectraAssetDocument> compiledDocuments = new HashMap<>();
  private boolean resourcePackDirty;

  public AssetPipelineService(
      AssetImportPort importPort,
      ResourcePackBuilder resourcePackBuilder,
      Path sourceDirectory,
      AssetTargetProfile targetProfile) {
    this(importPort, resourcePackBuilder, sourceDirectory, targetProfile, null);
  }

  public AssetPipelineService(
      AssetImportPort importPort,
      ResourcePackBuilder resourcePackBuilder,
      Path sourceDirectory,
      AssetTargetProfile targetProfile,
      ImportedAssetModelRegistrar modelRegistrar) {
    this.importPort = Objects.requireNonNull(importPort, "importPort");
    this.resourcePackBuilder = Objects.requireNonNull(resourcePackBuilder, "resourcePackBuilder");
    this.sourceDirectory =
        Objects.requireNonNull(sourceDirectory, "sourceDirectory").toAbsolutePath().normalize();
    this.targetProfile = Objects.requireNonNull(targetProfile, "targetProfile");
    this.modelRegistrar = modelRegistrar;
  }

  public void buildAssets() {
    LOGGER.fine("Starting Asset Pipeline build...");
    if (!Files.exists(sourceDirectory)) {
      LOGGER.fine("Source directory does not exist: " + sourceDirectory);
      return;
    }

    List<String> failures = new ArrayList<>();
    try {

      try (java.util.stream.Stream<Path> stream = Files.walk(sourceDirectory)) {
        stream
            .filter(Files::isRegularFile)
            .filter(this::isSupportedSource)
            .forEach(
                path -> {
                  try {
                    if (importSource(path)) {
                      resourcePackDirty = true;
                    }

                  } catch (Exception e) {
                    LOGGER.severe("Failed to compile asset source " + path + ": " + e.getMessage());
                    failures.add(path.getFileName() + ": " + e.getMessage());
                  }
                });
      }

      if (!failures.isEmpty()) {
        throw new IllegalStateException(
            "Asset pipeline rejected bundle(s): " + String.join("; ", failures));
      }
      rebuildResourcePackIfDirty();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to walk asset source directory", e);
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

    if (!Files.isRegularFile(path) || !isSupportedSource(path)) {
      throw new IllegalArgumentException("File not found or invalid format: " + filename);
    }

    try {
      importSource(path);
      rebuildResourcePackIfDirty();
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
    if (modelRegistrar != null) {
      modelRegistrar.register(document);
    }
    compiledDocuments.put(document.modelId(), document);
    sourceCache.put(cacheKey, currentHash);
    resourcePackDirty = true;
    return true;
  }

  private boolean isSupportedSource(Path path) {
    String filename = path.getFileName().toString();
    return filename.endsWith(SPECTRA_BUNDLE_EXTENSION) || filename.endsWith(BLOCKBENCH_EXTENSION);
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
    resourcePackDirty = false;
  }

  private void rebuildResourcePackIfDirty() throws IOException {
    if (!resourcePackDirty || compiledDocuments.isEmpty()) {
      LOGGER.fine("No asset changes detected or no documents. Incremental build skipped.");
      return;
    }
    LOGGER.fine("Rebuilding resource pack...");
    resourcePackBuilder.build(compiledDocuments.values(), targetProfile);
    resourcePackDirty = false;
    LOGGER.fine("Asset Pipeline build complete. Resource pack generated.");
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
