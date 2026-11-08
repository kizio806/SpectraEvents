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

  @Test
  void loadsReloadsAndValidatesYamlSourcesFilteringExampleFiles() throws Exception {
    EventDefinitionRegistry registry = new EventDefinitionRegistry();
    FileSystemDefinitionLoader loader =
        new FileSystemDefinitionLoader(
            temporaryDirectory,
            new DefinitionLoader(
                new EventSpecYamlParser(), new EventDefinitionCompiler(), registry));

    loader.ensureDefaultConfiguration();

    Path eventsDir = loader.eventsDirectory();
    String validYaml =
        """
        schema-version: 1
        id: valid_event
        initial-phase: p1
        phases:
          p1:
            transitions: []
        """;

    // Valid files
    Files.writeString(eventsDir.resolve("valid-one.yml"), validYaml);
    Files.writeString(
        eventsDir.resolve("valid-two.yaml"), validYaml.replace("valid_event", "valid_two"));

    // Filtered / ignored files
    Files.writeString(eventsDir.resolve("example.yml"), validYaml.replace("valid_event", "ex1"));
    Files.writeString(eventsDir.resolve("example.yaml"), validYaml.replace("valid_event", "ex2"));
    Files.writeString(
        eventsDir.resolve("example-boss.yml"), validYaml.replace("valid_event", "ex3"));
    Files.writeString(
        eventsDir.resolve("example_boss.yml"), validYaml.replace("valid_event", "ex4"));
    Files.writeString(eventsDir.resolve("notes.txt"), "not yaml");

    // Subdirectory inside eventsDir should be ignored
    Files.createDirectories(eventsDir.resolve("sub_dir"));

    // 1. validateFromDisk
    DefinitionLoadResult valResult = loader.validateFromDisk();
    assertEquals(2, valResult.loaded().size());
    assertTrue(registry.isEmpty()); // validate does not register

    // 2. loadFromDisk
    DefinitionLoadResult loadResult = loader.loadFromDisk();
    assertEquals(2, loadResult.loaded().size());
    assertEquals(2, registry.getAll().size());

    // 3. reloadFromDisk
    DefinitionLoadResult reloadResult = loader.reloadFromDisk();
    assertEquals(2, reloadResult.loaded().size());
    assertEquals(2, registry.getAll().size());

    // 4. Calling ensureDefaultConfiguration again when presets already exist
    loader.ensureDefaultConfiguration();
    assertTrue(Files.isRegularFile(loader.presetsDirectory().resolve("meteor.yml")));
  }

  @Test
  void handlesMissingEventsDirectoryGracefully() throws Exception {
    Path emptyBase = temporaryDirectory.resolve("empty_base");
    FileSystemDefinitionLoader loader =
        new FileSystemDefinitionLoader(
            emptyBase,
            new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry()));

    DefinitionLoadResult result = loader.reloadFromDisk();
    assertTrue(result.loaded().isEmpty());
  }
}
