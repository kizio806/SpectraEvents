package io.github.kizio806.spectraevents.application.config.loader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PublicExamplesCompilationTest {
  @Test
  void everyPublicEventExampleParsesAndCompiles() throws IOException {
    Path examples = Path.of("..", "examples", "events").normalize();
    EventSpecYamlParser parser = new EventSpecYamlParser();
    EventDefinitionCompiler compiler = new EventDefinitionCompiler();

    try (Stream<Path> files = Files.list(examples)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".yml")).sorted().toList()) {
        assertDoesNotThrow(
            () -> compiler.compile(parser.parse(Files.readString(file), file.toString())),
            () -> "Public example must compile: " + file);
      }
    }
  }
}
