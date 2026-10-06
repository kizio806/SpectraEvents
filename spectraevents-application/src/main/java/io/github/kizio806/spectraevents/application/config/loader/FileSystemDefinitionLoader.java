package io.github.kizio806.spectraevents.application.config.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Platform-independent filesystem discovery and loading of active event definitions. Bundled YAML
 * definitions are extracted as opt-in presets and are never silently activated or restored.
 */
public final class FileSystemDefinitionLoader {
  private static final Logger LOGGER = Logger.getLogger(FileSystemDefinitionLoader.class.getName());
  private static final String EVENTS_DIRECTORY_NAME = "events";
  private static final List<String> DEFAULT_BUNDLED_EVENTS =
      List.of("meteor.yml", "airdrop.yml", "metin.yml", "pinata.yml", "boss-portal.yml");

  private final Path dataDirectory;
  private final DefinitionLoader definitionLoader;

  public FileSystemDefinitionLoader(Path dataDirectory, DefinitionLoader definitionLoader) {
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    this.definitionLoader = Objects.requireNonNull(definitionLoader, "definitionLoader");
  }

  public Path eventsDirectory() {
    return dataDirectory.resolve(EVENTS_DIRECTORY_NAME);
  }

  public Path presetsDirectory() {
    return eventsDirectory().resolve("presets");
  }

  /**
   * Returns the directory containing installable Spectra bundles.
   *
   * <p>Files in this directory are deliberately not definition sources. A template becomes active
   * only through the installer, which writes its declared event file into {@link
   * #eventsDirectory()}.
   */
  public Path templatesDirectory() {
    return dataDirectory.resolve("templates");
  }

  /** Ensures the active directory exists and exposes bundled definitions as copyable presets. */
  public void ensureDefaultConfiguration() throws IOException {
    Path eventsDir = eventsDirectory();
    Files.createDirectories(eventsDir);
    Path presetsDir = presetsDirectory();
    Files.createDirectories(presetsDir);
    Files.createDirectories(templatesDirectory());

    for (String fileName : DEFAULT_BUNDLED_EVENTS) {
      Path targetPath = presetsDir.resolve(fileName);
      if (!Files.exists(targetPath)) {
        String resourcePath = "/events/" + fileName;
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
          if (in != null) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
            LOGGER.fine("Extracted bundled event preset to " + targetPath);
          }
        } catch (Exception e) {
          LOGGER.log(
              Level.WARNING,
              "Failed to extract default resource " + resourcePath + ": " + e.getMessage());
        }
      }
    }
  }

  /** Loads all YAML event definition files from disk into the application. */
  public DefinitionLoadResult loadFromDisk() throws IOException {
    ensureDefaultConfiguration();
    return definitionLoader.load(readYamlSources(eventsDirectory()));
  }

  /** Reloads all YAML event definition files from disk into the application. */
  public DefinitionLoadResult reloadFromDisk() throws IOException {
    return definitionLoader.reload(readYamlSources(eventsDirectory()));
  }

  /** Validates every YAML source without changing the live definition registry. */
  public DefinitionLoadResult validateFromDisk() throws IOException {
    return definitionLoader.validate(readYamlSources(eventsDirectory()));
  }

  private Map<String, String> readYamlSources(Path eventsDir) throws IOException {
    Map<String, String> sources = new LinkedHashMap<>();
    if (!Files.isDirectory(eventsDir)) {
      return sources;
    }

    List<Path> yamlFiles;
    try (Stream<Path> stream = Files.list(eventsDir)) {
      yamlFiles =
          stream
              .filter(Files::isRegularFile)
              .filter(
                  path -> {
                    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (name.equals("example.yml")
                        || name.equals("example.yaml")
                        || name.startsWith("example-")
                        || name.startsWith("example_")) {
                      return false;
                    }
                    return name.endsWith(".yml") || name.endsWith(".yaml");
                  })
              .sorted()
              .toList();
    }

    for (Path yamlFile : yamlFiles) {
      String relativeSource = EVENTS_DIRECTORY_NAME + "/" + yamlFile.getFileName();
      sources.put(relativeSource, Files.readString(yamlFile, StandardCharsets.UTF_8));
    }
    return sources;
  }
}
