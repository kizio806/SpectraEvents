package io.github.kizio806.spectraevents.application.config.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemLocaleLoaderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void extractsAllSupportedLocalesWithoutOverwritingAnOperatorTranslation() throws Exception {
    FileSystemLocaleLoader loader = new FileSystemLocaleLoader(temporaryDirectory);
    Files.createDirectories(loader.localesDirectory());
    Path polish = loader.localesDirectory().resolve("pl-PL.yml");
    Files.writeString(polish, "schema-version: 1\nmessages: { custom: zachowaj }\n");

    loader.ensureBundledLocales();

    assertEquals("schema-version: 1\nmessages: { custom: zachowaj }\n", Files.readString(polish));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("en-US.yml")));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("de-DE.yml")));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("es-ES.yml")));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("fr-FR.yml")));
    assertTrue(Files.isRegularFile(loader.localesDirectory().resolve("pt-BR.yml")));
  }
}
