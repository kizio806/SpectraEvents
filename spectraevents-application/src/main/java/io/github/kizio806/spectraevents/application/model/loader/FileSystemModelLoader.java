package io.github.kizio806.spectraevents.application.model.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Platform-independent filesystem discovery, bundled resource extraction, and loading of 3D models.
 */
public final class FileSystemModelLoader {
  private static final Logger LOGGER = Logger.getLogger(FileSystemModelLoader.class.getName());
  private static final String MODELS_DIRECTORY_NAME = "models";
  private static final List<String> DEFAULT_BUNDLED_MODELS =
      List.of("meteor.yml", "airdrop.yml", "metin.yml");

  private final Path dataDirectory;
  private final ModelLoader modelLoader;

  public FileSystemModelLoader(Path dataDirectory, ModelLoader modelLoader) {
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    this.modelLoader = Objects.requireNonNull(modelLoader, "modelLoader");
  }

  public Path modelsDirectory() {
    return dataDirectory.resolve(MODELS_DIRECTORY_NAME);
  }

  /** Ensures models directory exists and copies bundled default model definitions if missing. */
  public void ensureDefaultModels() throws IOException {
    Path modelsDir = modelsDirectory();
    Files.createDirectories(modelsDir);

    for (String fileName : DEFAULT_BUNDLED_MODELS) {
      Path targetPath = modelsDir.resolve(fileName);
      if (!Files.exists(targetPath)) {
        String resourcePath = "/models/" + fileName;
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
          if (in != null) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
            LOGGER.info("Extracted default model definition to " + targetPath);
          }
        } catch (Exception e) {
          LOGGER.log(
              Level.WARNING,
              "Failed to extract default model resource " + resourcePath + ": " + e.getMessage());
        }
      }
    }
  }

  /** Loads all YAML model definition files from disk into the application. */
  public ModelLoader.ModelLoaderResult loadFromDisk() throws IOException {
    ensureDefaultModels();
    return modelLoader.loadDirectory(modelsDirectory());
  }
}
