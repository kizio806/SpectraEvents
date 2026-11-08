package io.github.kizio806.spectraevents.application.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.loader.FileSystemDefinitionLoader;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
                new SpectraBundleInstaller(dataDirectory, "0.1.1-beta.2")
                    .install(rewriteMinimumVersion(copyBundledMetinArchive(), "9.0.0")));

    assertTrue(error.getMessage().contains("requires SpectraEvents 9.0.0"));
  }

  @Test
  void rejectsMissingOrInvalidArchive() throws Exception {
    SpectraBundleInstaller installer = new SpectraBundleInstaller(dataDirectory);

    assertThrows(
        IllegalArgumentException.class,
        () -> installer.install(dataDirectory.resolve("missing.spectra.zip")));

    Path txtFile = dataDirectory.resolve("not_a_bundle.txt");
    Files.writeString(txtFile, "hello");
    assertThrows(IllegalArgumentException.class, () -> installer.install(txtFile));

    assertThrows(NullPointerException.class, () -> installer.install(null));
  }

  @Test
  void rejectsUnsafeEntryNames() throws Exception {
    SpectraBundleInstaller installer = new SpectraBundleInstaller(dataDirectory);

    // Traversal
    Path zip1 = dataDirectory.resolve("traversal.spectra.zip");
    createBundle(zip1, Map.of("../evil.txt", "data".getBytes(StandardCharsets.UTF_8)));
    var ex1 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip1));
    assertTrue(ex1.getMessage().contains("unsafe ZIP entry"));

    // Backslash
    Path zip2 = dataDirectory.resolve("backslash.spectra.zip");
    createBundle(zip2, Map.of("sub\\evil.txt", "data".getBytes(StandardCharsets.UTF_8)));
    var ex2 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip2));
    assertTrue(ex2.getMessage().contains("unsafe ZIP entry"));

    // Starts with slash
    Path zip3 = dataDirectory.resolve("slash.spectra.zip");
    createBundle(zip3, Map.of("/evil.txt", "data".getBytes(StandardCharsets.UTF_8)));
    var ex3 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip3));
    assertTrue(ex3.getMessage().contains("unsafe ZIP entry"));
  }

  @Test
  void rejectsInvalidManifestStructure() throws Exception {
    SpectraBundleInstaller installer = new SpectraBundleInstaller(dataDirectory);

    // Manifest not a map
    Path zip1 = dataDirectory.resolve("not_map.spectra.zip");
    createBundle(zip1, Map.of("manifest.yml", "- list item\n".getBytes(StandardCharsets.UTF_8)));
    var ex1 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip1));
    assertTrue(ex1.getMessage().contains("must contain a YAML map"));

    // Unsupported fields
    Path zip2 = dataDirectory.resolve("unsupported.spectra.zip");
    String yamlUnsupported =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [models/stone.bbmodel]
        dependencies: []
        unsupported_field: true
        """;
    createBundle(zip2, Map.of("manifest.yml", yamlUnsupported.getBytes(StandardCharsets.UTF_8)));
    var ex2 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip2));
    assertTrue(ex2.getMessage().contains("unsupported field(s)"));

    // Schema version not 1
    Path zip3 = dataDirectory.resolve("bad_schema.spectra.zip");
    String yamlBadSchema =
        """
        schema-version: 2
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [models/stone.bbmodel]
        dependencies: []
        """;
    createBundle(zip3, Map.of("manifest.yml", yamlBadSchema.getBytes(StandardCharsets.UTF_8)));
    var ex3 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip3));
    assertTrue(ex3.getMessage().contains("schema-version must be 1"));

    // Invalid ID regex
    Path zip4 = dataDirectory.resolve("bad_id.spectra.zip");
    String yamlBadId =
        """
        schema-version: 1
        id: INVALID_ID
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [models/stone.bbmodel]
        dependencies: []
        """;
    createBundle(zip4, Map.of("manifest.yml", yamlBadId.getBytes(StandardCharsets.UTF_8)));
    var ex4 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip4));
    assertTrue(ex4.getMessage().contains("manifest.yml id must match"));

    // Invalid version semver
    Path zip5 = dataDirectory.resolve("bad_ver.spectra.zip");
    String yamlBadVer =
        """
        schema-version: 1
        id: test
        version: not_semver
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [models/stone.bbmodel]
        dependencies: []
        """;
    createBundle(zip5, Map.of("manifest.yml", yamlBadVer.getBytes(StandardCharsets.UTF_8)));
    var ex5 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip5));
    assertTrue(ex5.getMessage().contains("must be a semantic version"));

    // Dependencies not empty
    Path zip6 = dataDirectory.resolve("deps.spectra.zip");
    String yamlDeps =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [models/stone.bbmodel]
        dependencies: [dep1]
        """;
    createBundle(zip6, Map.of("manifest.yml", yamlDeps.getBytes(StandardCharsets.UTF_8)));
    var ex6 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip6));
    assertTrue(ex6.getMessage().contains("dependencies are not installed locally yet"));

    // Empty definitions or assets
    Path zip7 = dataDirectory.resolve("empty_def.spectra.zip");
    String yamlEmptyDef =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: []
        assets: []
        dependencies: []
        """;
    createBundle(zip7, Map.of("manifest.yml", yamlEmptyDef.getBytes(StandardCharsets.UTF_8)));
    var ex7 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip7));
    assertTrue(ex7.getMessage().contains("must declare at least one event definition and asset"));
  }

  @Test
  void rejectsInvalidLayoutAndDefinitions() throws Exception {
    SpectraBundleInstaller installer = new SpectraBundleInstaller(dataDirectory);

    // Definition not "event.yml"
    Path zip1 = dataDirectory.resolve("bad_def_name.spectra.zip");
    String manifest1 =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [other.yml]
        assets: [models/stone.bbmodel]
        dependencies: []
        """;
    createBundle(
        zip1,
        Map.of(
            "manifest.yml", manifest1.getBytes(StandardCharsets.UTF_8),
            "other.yml",
                "schema-version: 1\nid: test\ninitial-phase: p1\nphases:\n  p1: {}\n"
                    .getBytes(StandardCharsets.UTF_8),
            "models/stone.bbmodel", "model_data".getBytes(StandardCharsets.UTF_8)));
    var ex1 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip1));
    assertTrue(ex1.getMessage().contains("must use the canonical event.yml path"));

    // Invalid asset path
    Path zip2 = dataDirectory.resolve("bad_asset_path.spectra.zip");
    String manifest2 =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [invalid_folder/stone.png]
        dependencies: []
        """;
    createBundle(
        zip2,
        Map.of(
            "manifest.yml", manifest2.getBytes(StandardCharsets.UTF_8),
            "event.yml",
                "schema-version: 1\nid: test\ninitial-phase: p1\nphases:\n  p1: {}\n"
                    .getBytes(StandardCharsets.UTF_8),
            "invalid_folder/stone.png", "data".getBytes(StandardCharsets.UTF_8)));
    var ex2 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip2));
    assertTrue(ex2.getMessage().contains("Bundle asset path is invalid"));

    // Missing declared asset file in ZIP
    Path zip3 = dataDirectory.resolve("missing_asset_file.spectra.zip");
    String manifest3 =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [models/stone.bbmodel]
        dependencies: []
        """;
    createBundle(
        zip3,
        Map.of(
            "manifest.yml", manifest3.getBytes(StandardCharsets.UTF_8),
            "event.yml",
                "schema-version: 1\nid: test\ninitial-phase: p1\nphases:\n  p1: {}\n"
                    .getBytes(StandardCharsets.UTF_8)));
    var ex3 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip3));
    assertTrue(ex3.getMessage().contains("undeclared or missing files"));

    // Invalid event.yml compilation failure
    Path zip4 = dataDirectory.resolve("bad_event_yaml.spectra.zip");
    createBundle(
        zip4,
        Map.of(
            "manifest.yml", manifest3.getBytes(StandardCharsets.UTF_8),
            "event.yml", "invalid: [yaml: content".getBytes(StandardCharsets.UTF_8),
            "models/stone.bbmodel", "data".getBytes(StandardCharsets.UTF_8)));
    var ex4 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip4));
    assertTrue(ex4.getMessage().contains("Bundle event definition is invalid"));
  }

  @Test
  void refusesToOverwriteExistingAsset() throws Exception {
    Path assetsDir = dataDirectory.resolve("assets/source");
    Files.createDirectories(assetsDir);
    Files.writeString(assetsDir.resolve("stone.bbmodel"), "existing_model");

    Path zip = dataDirectory.resolve("overwrite_asset.spectra.zip");
    String manifest =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [models/stone.bbmodel]
        dependencies: []
        """;
    createBundle(
        zip,
        Map.of(
            "manifest.yml", manifest.getBytes(StandardCharsets.UTF_8),
            "event.yml",
                "schema-version: 1\nid: test\ninitial-phase: p1\nphases:\n  p1: {}\n"
                    .getBytes(StandardCharsets.UTF_8),
            "models/stone.bbmodel", "data".getBytes(StandardCharsets.UTF_8)));

    var ex =
        assertThrows(
            IllegalStateException.class,
            () -> new SpectraBundleInstaller(dataDirectory).install(zip));
    assertTrue(ex.getMessage().contains("overwrite existing server content"));
  }

  @Test
  void testManifestFieldValidationsAndErrors() throws Exception {
    SpectraBundleInstaller installer = new SpectraBundleInstaller(dataDirectory);

    // Missing id in manifest
    Path zip1 = dataDirectory.resolve("missing_id.spectra.zip");
    String yamlMissingId =
        """
        schema-version: 1
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [models/stone.bbmodel]
        dependencies: []
        """;
    createBundle(zip1, Map.of("manifest.yml", yamlMissingId.getBytes(StandardCharsets.UTF_8)));
    var ex1 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip1));
    assertTrue(ex1.getMessage().contains("manifest.yml requires id"));

    // event-definitions is not a list
    Path zip2 = dataDirectory.resolve("not_a_list.spectra.zip");
    String yamlNotList =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: "not_a_list"
        assets: [models/stone.bbmodel]
        dependencies: []
        """;
    createBundle(zip2, Map.of("manifest.yml", yamlNotList.getBytes(StandardCharsets.UTF_8)));
    var ex2 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip2));
    assertTrue(ex2.getMessage().contains("requires event-definitions as a list"));

    // assets has duplicate entries
    Path zip3 = dataDirectory.resolve("duplicate_assets.spectra.zip");
    String yamlDupAssets =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [models/stone.bbmodel, models/stone.bbmodel]
        dependencies: []
        """;
    createBundle(zip3, Map.of("manifest.yml", yamlDupAssets.getBytes(StandardCharsets.UTF_8)));
    var ex3 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip3));
    assertTrue(ex3.getMessage().contains("must contain unique non-blank strings"));

    // assets has blank entry
    Path zip4 = dataDirectory.resolve("blank_asset.spectra.zip");
    String yamlBlankAsset =
        """
        schema-version: 1
        id: test
        version: 1.0.0
        minimum-spectraevents-version: 0.1.1-beta.2
        event-definitions: [event.yml]
        assets: [""]
        dependencies: []
        """;
    createBundle(zip4, Map.of("manifest.yml", yamlBlankAsset.getBytes(StandardCharsets.UTF_8)));
    var ex4 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip4));
    assertTrue(ex4.getMessage().contains("must contain unique non-blank strings"));
  }

  @Test
  void rejectsDuplicateEntriesInArchive() throws Exception {
    Path zip = dataDirectory.resolve("duplicate_entries.spectra.zip");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      zos.putNextEntry(new ZipEntry("entry.txt"));
      zos.write("data".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
    byte[] bytes = baos.toByteArray();
    int cdIdx = bytes.length;
    for (int i = 0; i < bytes.length - 3; i++) {
      if (bytes[i] == 0x50
          && bytes[i + 1] == 0x4b
          && bytes[i + 2] == 0x01
          && bytes[i + 3] == 0x02) {
        cdIdx = i;
        break;
      }
    }
    ByteArrayOutputStream dupStream = new ByteArrayOutputStream();
    dupStream.write(bytes, 0, cdIdx);
    dupStream.write(bytes, 0, cdIdx);
    dupStream.write(bytes, cdIdx, bytes.length - cdIdx);
    Files.write(zip, dupStream.toByteArray());

    var ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SpectraBundleInstaller(dataDirectory).install(zip));
    assertTrue(ex.getMessage().contains("duplicate entry"));
  }

  @Test
  void rollbacksInstalledFilesWhenAssetWriteFails() throws Exception {
    Path zip = copyBundledMetinArchive();

    // Make assets directory a file so creating directories / files inside it throws IOException
    Path assetsDir = dataDirectory.resolve("assets").resolve("source");
    Path assetsParent = assetsDir.getParent();
    if (assetsParent != null) {
      Files.createDirectories(assetsParent);
    }
    Files.writeString(assetsDir, "blocking_file_not_dir");

    var ex =
        assertThrows(Exception.class, () -> new SpectraBundleInstaller(dataDirectory).install(zip));
    assertTrue(ex instanceof java.io.IOException || ex instanceof IllegalStateException);

    // Verify rollback: events/metin.yml was deleted by rollback logic
    Path eventDef = dataDirectory.resolve("events/metin.yml");
    org.junit.jupiter.api.Assertions.assertFalse(
        Files.exists(eventDef), "Installed definition must be cleaned up on failure");
  }

  @Test
  void rejectsManifestWithNonCanonicalEventDefinitionOrInvalidAsset() throws Exception {
    SpectraBundleInstaller installer = new SpectraBundleInstaller(dataDirectory);

    // 1. Non-canonical event definition name
    String manifestBadDef =
        """
        schema-version: 1
        id: bad_def
        version: 1.0.0
        minimum-spectraevents-version: 0.1.0
        event-definitions:
          - other.yml
        assets:
          - models/test.bbmodel
        dependencies: []
        """;
    Path zip1 = dataDirectory.resolve("bad_def.spectra.zip");
    createBundle(
        zip1,
        Map.of(
            "manifest.yml", manifestBadDef.getBytes(StandardCharsets.UTF_8),
            "other.yml",
                "schema-version: 1\nid: bad_def\ninitial-phase: p\nphases: {p: {}}\n"
                    .getBytes(StandardCharsets.UTF_8),
            "models/test.bbmodel", "data".getBytes(StandardCharsets.UTF_8)));
    var ex1 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip1));
    assertTrue(ex1.getMessage().contains("must use the canonical event.yml path"));

    // 2. Invalid asset path (not under models/)
    String manifestBadAsset =
        """
        schema-version: 1
        id: bad_asset
        version: 1.0.0
        minimum-spectraevents-version: 0.1.0
        event-definitions:
          - event.yml
        assets:
          - invalid_dir/test.bbmodel
        dependencies: []
        """;
    Path zip2 = dataDirectory.resolve("bad_asset.spectra.zip");
    createBundle(
        zip2,
        Map.of(
            "manifest.yml", manifestBadAsset.getBytes(StandardCharsets.UTF_8),
            "event.yml",
                "schema-version: 1\nid: bad_asset\ninitial-phase: p\nphases: {p: {}}\n"
                    .getBytes(StandardCharsets.UTF_8),
            "invalid_dir/test.bbmodel", "data".getBytes(StandardCharsets.UTF_8)));
    var ex2 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip2));
    assertTrue(ex2.getMessage().contains("Bundle asset path is invalid"));

    // 3. Duplicate strings in assets list
    String manifestDup =
        """
        schema-version: 1
        id: dup_asset
        version: 1.0.0
        minimum-spectraevents-version: 0.1.0
        event-definitions:
          - event.yml
        assets:
          - models/test.bbmodel
          - models/test.bbmodel
        dependencies: []
        """;
    Path zip3 = dataDirectory.resolve("dup_asset.spectra.zip");
    createBundle(
        zip3,
        Map.of(
            "manifest.yml", manifestDup.getBytes(StandardCharsets.UTF_8),
            "event.yml",
                "schema-version: 1\nid: dup_asset\ninitial-phase: p\nphases: {p: {}}\n"
                    .getBytes(StandardCharsets.UTF_8),
            "models/test.bbmodel", "data".getBytes(StandardCharsets.UTF_8)));
    var ex3 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip3));
    assertTrue(ex3.getMessage().contains("must contain unique non-blank strings"));

    // 4. Declared dependencies rejected as not yet supported
    String manifestDeps =
        """
        schema-version: 1
        id: with_deps
        version: 1.0.0
        minimum-spectraevents-version: 0.1.0
        event-definitions:
          - event.yml
        assets:
          - models/test.bbmodel
        dependencies:
          - uninstalled-bundle
        """;
    Path zip4 = dataDirectory.resolve("with_deps.spectra.zip");
    createBundle(
        zip4,
        Map.of(
            "manifest.yml", manifestDeps.getBytes(StandardCharsets.UTF_8),
            "event.yml",
                "schema-version: 1\nid: with_deps\ninitial-phase: p\nphases: {p: {}}\n"
                    .getBytes(StandardCharsets.UTF_8),
            "models/test.bbmodel", "data".getBytes(StandardCharsets.UTF_8)));
    var ex4 = assertThrows(IllegalArgumentException.class, () -> installer.install(zip4));
    assertTrue(ex4.getMessage().contains("dependencies are not installed locally"));
  }

  private static void createBundle(Path target, Map<String, byte[]> entries) throws Exception {
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(target))) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        ZipEntry ze = new ZipEntry(entry.getKey());
        zos.putNextEntry(ze);
        zos.write(entry.getValue());
        zos.closeEntry();
      }
    }
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
                      "minimum-spectraevents-version: 0.1.1-beta.2",
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
