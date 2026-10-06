package io.github.kizio806.spectraevents.application.template;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.update.SemVer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Installs a self-contained, local Spectra Bundle v1 without overwriting server-owned content. */
public final class SpectraBundleInstaller {
  private static final String DEFAULT_SPECTRAEVENTS_VERSION = "0.1.1-beta.1";
  private static final long MAX_ARCHIVE_BYTES = 25_000_000L;
  private static final long MAX_TOTAL_BYTES = 32_000_000L;
  private static final int MAX_ENTRIES = 64;
  private static final Pattern BUNDLE_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
  private static final Pattern ASSET_PATH =
      Pattern.compile("models/[a-z0-9][a-z0-9._-]{0,63}\\.(bbmodel|spectra\\.zip)");
  private static final Set<String> MANIFEST_KEYS =
      Set.of(
          "schema-version",
          "id",
          "version",
          "minimum-spectraevents-version",
          "event-definitions",
          "assets",
          "dependencies");

  private final Path dataDirectory;
  private final String spectraEventsVersion;

  public SpectraBundleInstaller(Path dataDirectory) {
    this(dataDirectory, DEFAULT_SPECTRAEVENTS_VERSION);
  }

  public SpectraBundleInstaller(Path dataDirectory, String spectraEventsVersion) {
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath();
    this.spectraEventsVersion =
        requireSemanticVersion(
            Objects.requireNonNull(spectraEventsVersion, "spectraEventsVersion"),
            "SpectraEvents version");
  }

  /**
   * Validates and installs a bundle. Existing active definitions and model sources are never
   * replaced: an operator must remove or rename them deliberately before installing a conflicting
   * bundle.
   */
  public InstallResult install(Path archive) throws IOException {
    Objects.requireNonNull(archive, "archive");
    if (!Files.isRegularFile(archive)
        || !archive.getFileName().toString().endsWith(".spectra.zip")) {
      throw new IllegalArgumentException("Bundle must be a regular .spectra.zip file");
    }
    if (Files.size(archive) > MAX_ARCHIVE_BYTES) {
      throw new IllegalArgumentException("Bundle exceeds the 25 MB compressed size limit");
    }

    Map<String, byte[]> entries = readArchive(archive);
    BundleManifest manifest = parseManifest(required(entries, "manifest.yml"));
    verifyMinimumVersion(manifest.minimumVersion());
    validateLayout(entries, manifest);
    validateDefinitions(entries, manifest);

    Path eventsDirectory = dataDirectory.resolve("events");
    Path assetsDirectory = dataDirectory.resolve("assets").resolve("source");
    List<Path> targets = new ArrayList<>();
    targets.add(eventsDirectory.resolve(manifest.id() + ".yml"));
    for (String asset : manifest.assets()) {
      targets.add(assetsDirectory.resolve(Path.of(asset).getFileName()));
    }
    for (Path target : targets) {
      if (Files.exists(target)) {
        throw new IllegalStateException(
            "Bundle would overwrite existing server content: " + target);
      }
    }

    List<Path> installedDefinitions = new ArrayList<>();
    List<Path> installedAssets = new ArrayList<>();
    try {
      Path definitionTarget = eventsDirectory.resolve(manifest.id() + ".yml");
      writeAtomically(definitionTarget, required(entries, manifest.eventDefinitions().getFirst()));
      installedDefinitions.add(definitionTarget);
      for (String asset : manifest.assets()) {
        Path target = assetsDirectory.resolve(Path.of(asset).getFileName());
        writeAtomically(target, required(entries, asset));
        installedAssets.add(target);
      }
    } catch (IOException | RuntimeException exception) {
      installedDefinitions.forEach(SpectraBundleInstaller::deleteQuietly);
      installedAssets.forEach(SpectraBundleInstaller::deleteQuietly);
      throw exception;
    }
    return new InstallResult(
        manifest.id(), manifest.version(), installedDefinitions, installedAssets);
  }

  private static Map<String, byte[]> readArchive(Path archive) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    long totalBytes = 0L;
    int count = 0;
    try (InputStream input = Files.newInputStream(archive);
        ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (++count > MAX_ENTRIES) {
          throw new IllegalArgumentException("Bundle contains more than 64 ZIP entries");
        }
        String name = entry.getName();
        validateEntryName(name);
        if (entry.isDirectory()) {
          continue;
        }
        byte[] bytes = zip.readAllBytes();
        totalBytes += bytes.length;
        if (totalBytes > MAX_TOTAL_BYTES) {
          throw new IllegalArgumentException("Bundle exceeds the 32 MB expanded size limit");
        }
        if (entries.putIfAbsent(name, bytes) != null) {
          throw new IllegalArgumentException("Bundle contains duplicate entry: " + name);
        }
      }
    }
    return Map.copyOf(entries);
  }

  private static void validateEntryName(String name) {
    if (name.isBlank()
        || name.startsWith("/")
        || name.contains("\\")
        || name.contains("..")
        || Path.of(name).isAbsolute()) {
      throw new IllegalArgumentException("Bundle contains unsafe ZIP entry: " + name);
    }
  }

  private static BundleManifest parseManifest(byte[] manifestBytes) {
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(false);
    options.setNestingDepthLimit(16);
    options.setCodePointLimit(100_000);
    Object document =
        new Yaml(new SafeConstructor(options)).load(new ByteArrayInputStream(manifestBytes));
    if (!(document instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("manifest.yml must contain a YAML map");
    }
    Set<String> keys = new LinkedHashSet<>();
    for (Object key : map.keySet()) {
      keys.add(String.valueOf(key));
    }
    if (!MANIFEST_KEYS.containsAll(keys)) {
      throw new IllegalArgumentException("manifest.yml contains unsupported field(s): " + keys);
    }
    if (!"1".equals(String.valueOf(map.get("schema-version")))) {
      throw new IllegalArgumentException("manifest.yml schema-version must be 1");
    }
    String id = requiredString(map, "id");
    if (!BUNDLE_ID.matcher(id).matches()) {
      throw new IllegalArgumentException("manifest.yml id must match " + BUNDLE_ID.pattern());
    }
    String version = requireSemanticVersion(requiredString(map, "version"), "manifest.yml version");
    String minimumVersion =
        requireSemanticVersion(
            requiredString(map, "minimum-spectraevents-version"),
            "manifest.yml minimum-spectraevents-version");
    List<String> definitions = stringList(map, "event-definitions");
    List<String> assets = stringList(map, "assets");
    List<String> dependencies = stringList(map, "dependencies");
    if (definitions.isEmpty() || assets.isEmpty()) {
      throw new IllegalArgumentException(
          "manifest.yml must declare at least one event definition and asset");
    }
    if (!dependencies.isEmpty()) {
      throw new IllegalArgumentException(
          "Bundle dependencies are not installed locally yet: " + String.join(", ", dependencies));
    }
    return new BundleManifest(id, version, minimumVersion, definitions, assets);
  }

  private void verifyMinimumVersion(String minimumVersion) {
    if (SemVer.parse(spectraEventsVersion).compareTo(SemVer.parse(minimumVersion)) < 0) {
      throw new IllegalArgumentException(
          "Bundle requires SpectraEvents "
              + minimumVersion
              + " or newer; this server runs "
              + spectraEventsVersion);
    }
  }

  private static String requireSemanticVersion(String version, String field) {
    String value = version.trim();
    if (!value.matches(
        "v?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?")) {
      throw new IllegalArgumentException(field + " must be a semantic version");
    }
    return value;
  }

  private static void validateLayout(Map<String, byte[]> entries, BundleManifest manifest) {
    Set<String> expected = new LinkedHashSet<>();
    expected.add("manifest.yml");
    expected.addAll(manifest.eventDefinitions());
    expected.addAll(manifest.assets());
    if (!entries.keySet().equals(expected)) {
      throw new IllegalArgumentException("Bundle has undeclared or missing files");
    }
    for (String definition : manifest.eventDefinitions()) {
      if (!"event.yml".equals(definition)) {
        throw new IllegalArgumentException(
            "Bundle definitions must use the canonical event.yml path");
      }
    }
    for (String asset : manifest.assets()) {
      if (!ASSET_PATH.matcher(asset).matches()) {
        throw new IllegalArgumentException("Bundle asset path is invalid: " + asset);
      }
    }
  }

  private static void validateDefinitions(Map<String, byte[]> entries, BundleManifest manifest) {
    Map<String, String> sources = new LinkedHashMap<>();
    for (String definition : manifest.eventDefinitions()) {
      sources.put(
          "bundle:" + definition,
          new String(required(entries, definition), StandardCharsets.UTF_8));
    }
    DefinitionLoader loader =
        new DefinitionLoader(
            new EventSpecYamlParser(),
            new EventDefinitionCompiler(),
            new EventDefinitionRegistry());
    var result = loader.load(sources);
    if (!result.failures().isEmpty() || result.loaded().size() != sources.size()) {
      throw new IllegalArgumentException(
          "Bundle event definition is invalid: " + result.failures());
    }
  }

  private static String requiredString(Map<?, ?> values, String key) {
    Object value = values.get(key);
    if (value == null || String.valueOf(value).isBlank()) {
      throw new IllegalArgumentException("manifest.yml requires " + key);
    }
    return String.valueOf(value);
  }

  private static List<String> stringList(Map<?, ?> values, String key) {
    Object value = values.get(key);
    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException("manifest.yml requires " + key + " as a list");
    }
    List<String> result = list.stream().map(String::valueOf).toList();
    if (result.stream().anyMatch(String::isBlank)
        || result.size() != new LinkedHashSet<>(result).size()) {
      throw new IllegalArgumentException(
          "manifest.yml " + key + " must contain unique non-blank strings");
    }
    return result;
  }

  private static byte[] required(Map<String, byte[]> entries, String path) {
    byte[] value = entries.get(path);
    if (value == null) {
      throw new IllegalArgumentException("Bundle is missing required entry: " + path);
    }
    return value;
  }

  private static void writeAtomically(Path target, byte[] content) throws IOException {
    Files.createDirectories(target.getParent());
    Path temporary =
        Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
    try {
      Files.write(temporary, content);
      try {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // The original installation error is more useful than cleanup failure.
    }
  }

  private record BundleManifest(
      String id,
      String version,
      String minimumVersion,
      List<String> eventDefinitions,
      List<String> assets) {}

  public record InstallResult(
      String id, String version, List<Path> definitions, List<Path> assets) {
    public InstallResult {
      definitions = List.copyOf(definitions);
      assets = List.copyOf(assets);
    }
  }
}
