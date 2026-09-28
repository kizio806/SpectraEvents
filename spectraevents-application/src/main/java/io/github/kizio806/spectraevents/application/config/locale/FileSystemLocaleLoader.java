package io.github.kizio806.spectraevents.application.config.locale;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/** Extracts the shipped locale catalog without replacing an operator's translation. */
public final class FileSystemLocaleLoader {
  private static final List<String> BUNDLED_LOCALES =
      List.of("pl-PL.yml", "en-US.yml", "de-DE.yml", "es-ES.yml", "fr-FR.yml", "pt-BR.yml");

  private final Path localesDirectory;

  public FileSystemLocaleLoader(Path dataDirectory) {
    this.localesDirectory =
        Objects.requireNonNull(dataDirectory, "dataDirectory").resolve("locales");
  }

  public Path localesDirectory() {
    return localesDirectory;
  }

  /** Copies only missing built-ins. Existing files are always treated as operator-owned. */
  public void ensureBundledLocales() throws IOException {
    Files.createDirectories(localesDirectory);
    for (String fileName : BUNDLED_LOCALES) {
      Path target = localesDirectory.resolve(fileName);
      if (Files.exists(target)) {
        continue;
      }
      try (InputStream resource = getClass().getResourceAsStream("/locales/" + fileName)) {
        if (resource == null) {
          throw new IOException("Missing bundled locale resource: " + fileName);
        }
        Files.copy(resource, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }
}
