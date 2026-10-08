package io.github.kizio806.spectraevents.application.config.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemDefinitionLoaderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void preparesAFreshDataDirectoryWithoutActivatingDefinitions() throws Exception {
    FileSystemDefinitionLoader loader =
        new FileSystemDefinitionLoader(
            temporaryDirectory,
            new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry()));

    DefinitionLoadResult result = loader.loadFromDisk();

    assertTrue(result.loaded().isEmpty());
    assertTrue(Files.isRegularFile(loader.presetsDirectory().resolve("meteor.yml")));
    assertTrue(Files.isDirectory(loader.templatesDirectory()));
    try (var presets = Files.list(loader.presetsDirectory())) {
      assertEquals(5, presets.count());
    }
    try (var activeDefinitions = Files.list(loader.eventsDirectory())) {
      assertTrue(activeDefinitions.noneMatch(Files::isRegularFile));
    }
  }
}
