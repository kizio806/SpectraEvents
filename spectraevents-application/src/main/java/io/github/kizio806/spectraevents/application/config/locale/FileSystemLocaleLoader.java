package io.github.kizio806.spectraevents.application.config.locale;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

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

  /**
   * Loads the selected locale over the bundled English catalog. Existing operator files are an
   * overlay, so a new release can add English fallback keys without replacing translations.
   */
  public LocaleCatalog loadCatalog(String requestedLocale) throws IOException {
    ensureBundledLocales();
    String selected =
        BUNDLED_LOCALES.contains(requestedLocale + ".yml")
            ? requestedLocale
            : LocaleCatalog.englishLocale();
    Map<String, String> english = readMergedLocale(LocaleCatalog.englishLocale());
    Map<String, String> selectedMessages =
        selected.equals(LocaleCatalog.englishLocale()) ? english : readMergedLocale(selected);
    return new LocaleCatalog(selected, english, selectedMessages);
  }

  public LocaleCatalog loadConfiguredCatalog(Path configFile) throws IOException {
    Map<?, ?> configuration = load(Files.readString(configFile, StandardCharsets.UTF_8));
    Object locale = configuration.get("locale");
    return loadCatalog(locale == null ? LocaleCatalog.englishLocale() : String.valueOf(locale));
  }

  private Map<String, String> readMergedLocale(String locale) throws IOException {
    Map<String, String> result = new LinkedHashMap<>();
    try (InputStream bundled = getClass().getResourceAsStream("/locales/" + locale + ".yml")) {
      if (bundled == null) {
        throw new IOException("Missing bundled locale resource: " + locale);
      }
      flatten(load(new String(bundled.readAllBytes(), StandardCharsets.UTF_8)), "", result);
    }
    Path operatorFile = localesDirectory.resolve(locale + ".yml");
    if (Files.isRegularFile(operatorFile)) {
      flatten(load(Files.readString(operatorFile, StandardCharsets.UTF_8)), "", result);
    }
    result.remove("schema-version");
    return Map.copyOf(result);
  }

  private Map<?, ?> load(String source) throws IOException {
    try {
      Object value = new Yaml(new SafeConstructor(new LoaderOptions())).load(source);
      if (value instanceof Map<?, ?> map) {
        return map;
      }
      throw new IOException("Locale root must be a map.");
    } catch (RuntimeException exception) {
      throw new IOException("Locale YAML is invalid.", exception);
    }
  }

  private void flatten(Map<?, ?> source, String prefix, Map<String, String> target) {
    for (Map.Entry<?, ?> entry : source.entrySet()) {
      String key =
          prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
      if (entry.getValue() instanceof Map<?, ?> nested) {
        flatten(nested, key, target);
      } else if (entry.getValue() != null) {
        target.put(key, String.valueOf(entry.getValue()));
      }
    }
  }
}
