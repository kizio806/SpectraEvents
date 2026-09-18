package io.github.kizio806.spectraevents.application.config.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Platform-independent filesystem discovery and loading of event definitions. Handles extracting
 * default bundled YAML definitions on first run.
 */
public final class FileSystemDefinitionLoader {
  private static final Logger LOGGER = Logger.getLogger(FileSystemDefinitionLoader.class.getName());
  private static final String EVENTS_DIRECTORY_NAME = "events";
  private static final List<String> DEFAULT_BUNDLED_EVENTS =
      List.of("meteor.yml", "airdrop.yml", "metin.yml", "example.yml");

  private final Path dataDirectory;
  private final DefinitionLoader definitionLoader;

  public FileSystemDefinitionLoader(Path dataDirectory, DefinitionLoader definitionLoader) {
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    this.definitionLoader = Objects.requireNonNull(definitionLoader, "definitionLoader");
  }

  public Path eventsDirectory() {
    return dataDirectory.resolve(EVENTS_DIRECTORY_NAME);
  }

  /** Ensures events directory exists and copies bundled default event definitions if missing. */
  public void ensureDefaultConfiguration() throws IOException {
    Path eventsDir = eventsDirectory();
    Files.createDirectories(eventsDir);

    for (String fileName : DEFAULT_BUNDLED_EVENTS) {
      Path targetPath = eventsDir.resolve(fileName);
      if (!Files.exists(targetPath)) {
        String resourcePath = "/events/" + fileName;
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
          if (in != null) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
            LOGGER.info("Extracted default event definition to " + targetPath);
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

  private LinkedHashMap<String, String> readYamlSources(Path eventsDir) throws IOException {
    LinkedHashMap<String, String> sources = new LinkedHashMap<>();
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
                    String name = path.getFileName().toString().toLowerCase();
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
