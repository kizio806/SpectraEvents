package io.github.kizio806.spectraevents.application.schedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Filesystem adapter kept in application because it only parses versioned YAML into value objects.
 */
public final class FileSystemScheduleLoader {
  private final ScheduleYamlLoader yamlLoader;

  public FileSystemScheduleLoader(ScheduleYamlLoader yamlLoader) {
    this.yamlLoader = Objects.requireNonNull(yamlLoader, "yamlLoader");
  }

  public ScheduleLoadResult load(Path schedulesFile) throws IOException {
    Path file = Objects.requireNonNull(schedulesFile, "schedulesFile").toAbsolutePath().normalize();
    return yamlLoader.loadResilient(Files.readString(file), file.toString());
  }
}
