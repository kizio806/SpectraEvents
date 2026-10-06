package io.github.kizio806.spectraevents.application.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.loader.FileSystemDefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpectraBundleInstallerTest {
  @TempDir Path dataDirectory;

  @Test
  void installsTheBundledMetinWithoutCopyingLooseFiles() throws Exception {
    Path archive = copyBundledMetinArchive();

    SpectraBundleInstaller.InstallResult result =
        new SpectraBundleInstaller(dataDirectory).install(archive);

    assertEquals("metin", result.id());
    assertTrue(Files.isRegularFile(dataDirectory.resolve("events/metin.yml")));
    Path model = dataDirectory.resolve("assets/source/metin_stone.bbmodel");
    assertTrue(Files.isRegularFile(model));
    assertTrue(Files.size(model) > 0L);

    FileSystemDefinitionLoader loader =
        new FileSystemDefinitionLoader(
            dataDirectory,
            new DefinitionLoader(
                new EventSpecYamlParser(),
                new EventDefinitionCompiler(),
                new EventDefinitionRegistry()));
    var definitions = loader.loadFromDisk();
    assertEquals(1, definitions.loaded().size(), definitions.failures()::toString);
    assertEquals("metin", definitions.loaded().getFirst().definition().id().value());
  }

  @Test
  void refusesToOverwriteAnExistingActiveDefinition() throws Exception {
    Files.createDirectories(dataDirectory.resolve("events"));
    Files.writeString(dataDirectory.resolve("events/metin.yml"), "existing: server-owned");

    IllegalStateException error =
        assertThrows(
            IllegalStateException.class,
            () -> new SpectraBundleInstaller(dataDirectory).install(copyBundledMetinArchive()));

    assertTrue(error.getMessage().contains("overwrite"));
  }

  @Test
  void rejectsTemplatesThatRequireANewerSpectraEventsVersion() throws Exception {
    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new SpectraBundleInstaller(dataDirectory, "0.1.1-beta.1")
                    .install(rewriteMinimumVersion(copyBundledMetinArchive(), "9.0.0")));

    assertTrue(error.getMessage().contains("requires SpectraEvents 9.0.0"));
  }

  private Path copyBundledMetinArchive() throws Exception {
    Path archive = dataDirectory.resolve("metin.spectra.zip");
    try (InputStream input = getClass().getResourceAsStream("/templates/metin.spectra.zip")) {
      assertTrue(input != null, "The application JAR must contain the bundled Metin template");
      Files.copy(input, archive);
    }
    return archive;
  }

  private Path rewriteMinimumVersion(Path archive, String minimumVersion) throws Exception {
    Path rewritten = dataDirectory.resolve("minimum-version.spectra.zip");
    try (var input = new java.util.zip.ZipInputStream(Files.newInputStream(archive));
        var output = new java.util.zip.ZipOutputStream(Files.newOutputStream(rewritten))) {
      java.util.zip.ZipEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        output.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
        byte[] contents = input.readAllBytes();
        if ("manifest.yml".equals(entry.getName())) {
          String manifest = new String(contents, java.nio.charset.StandardCharsets.UTF_8);
          contents =
              manifest
                  .replace(
                      "minimum-spectraevents-version: 0.1.1-beta.1",
                      "minimum-spectraevents-version: " + minimumVersion)
                  .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        output.write(contents);
        output.closeEntry();
      }
    }
    return rewritten;
  }
}
