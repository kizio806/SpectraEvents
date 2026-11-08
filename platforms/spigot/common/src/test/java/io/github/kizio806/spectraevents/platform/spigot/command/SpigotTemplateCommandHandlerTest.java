package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotTemplateCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;
  private SpigotCommandHelper helper;
  private Plugin dummyPlugin;
  private SpectraEventsApplication application;
  private BukkitDefinitionConfigBootstrap definitions;
  private List<String> messages;
  private CommandSender sender;

  @BeforeEach
  void setUp() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    helper = new SpigotCommandHelper(locales);

    dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getDataFolder".equals(method.getName())) {
                    return tempDir.toFile();
                  }
                  if ("getLogger".equals(method.getName())) {
                    return java.util.logging.Logger.getLogger("template-test");
                  }
                  if ("getDescription".equals(method.getName())) {
                    return new PluginDescriptionFile("SpectraEvents", "1.0.0", "SpigotBootstrap");
                  }
                  return null;
                });

    DefinitionLoader loader =
        new DefinitionLoader(
            new EventSpecYamlParser(),
            new EventDefinitionCompiler(),
            new EventDefinitionRegistry());

    definitions = new BukkitDefinitionConfigBootstrap(dummyPlugin, loader);

    LifecycleReporter reporter =
        (LifecycleReporter)
            Proxy.newProxyInstance(
                LifecycleReporter.class.getClassLoader(),
                new Class<?>[] {LifecycleReporter.class},
                (proxy, method, args) -> null);

    EventTaskScheduler scheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (proxy, method, args) -> null);

    PlatformActionPort actionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (proxy, method, args) -> null);

    PlatformEntityReconcilerPort reconciler =
        (PlatformEntityReconcilerPort)
            Proxy.newProxyInstance(
                PlatformEntityReconcilerPort.class.getClassLoader(),
                new Class<?>[] {PlatformEntityReconcilerPort.class},
                (proxy, method, args) -> null);

    PlatformCapabilityQuery capabilityQuery =
        (PlatformCapabilityQuery)
            Proxy.newProxyInstance(
                PlatformCapabilityQuery.class.getClassLoader(),
                new Class<?>[] {PlatformCapabilityQuery.class},
                (proxy, method, args) -> null);

    ModelRendererPort renderer =
        (ModelRendererPort)
            Proxy.newProxyInstance(
                ModelRendererPort.class.getClassLoader(),
                new Class<?>[] {ModelRendererPort.class},
                (proxy, method, args) -> null);

    application =
        new SpectraEventsApplication(
            reporter,
            scheduler,
            actionPort,
            new InMemoryEventInstanceRepository(),
            reconciler,
            capabilityQuery,
            renderer);

    Path sourceDir = tempDir.resolve("assets").resolve("source");
    Path outputDir = tempDir.resolve("output");
    Files.createDirectories(sourceDir);
    Files.createDirectories(outputDir);
    AssetPipelineService assetPipeline =
        new AssetPipelineService(
            path -> new SpectraAssetDocument(1, "metin_stone", Map.of(), List.of(), Map.of()),
            new ResourcePackBuilder(outputDir),
            sourceDir,
            AssetTargetProfile.PROFILE_26_1);
    application.setAssetPipelineService(assetPipeline);

    messages = new ArrayList<>();
    sender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("sendMessage".equals(method.getName()) && args != null && args.length > 0) {
                    messages.add(String.valueOf(args[0]));
                    return null;
                  }
                  if ("hasPermission".equals(method.getName())) {
                    return true;
                  }
                  if ("isOp".equals(method.getName())) {
                    return true;
                  }
                  return null;
                });
  }

  @Test
  void rejectsNullConstructorParameters() {
    assertThrows(
        NullPointerException.class,
        () -> new SpigotTemplateCommandHandler(null, null, null, helper));
  }

  @Test
  void testListTemplates() {
    SpigotTemplateCommandHandler handler =
        new SpigotTemplateCommandHandler(dummyPlugin, application, definitions, helper);

    handler.execute(sender, new String[] {});
    assertFalse(messages.isEmpty());
    assertTrue(messages.getFirst().contains("metin"));

    messages.clear();
    handler.execute(sender, new String[] {"list"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.getFirst().contains("metin"));
  }

  @Test
  void testInstallArgumentsValidation() {
    SpigotTemplateCommandHandler handler =
        new SpigotTemplateCommandHandler(dummyPlugin, application, definitions, helper);

    // Missing second argument
    assertThrows(
        IllegalArgumentException.class, () -> handler.execute(sender, new String[] {"install"}));

    // Too many arguments
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"install", "metin", "extra"}));

    // Two arguments but not install
    assertThrows(
        IllegalArgumentException.class, () -> handler.execute(sender, new String[] {"foo", "bar"}));

    // Unknown template
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"install", "unknown"}));
  }

  @Test
  void testInstallMetinMissingArchiveFails() {
    SpigotTemplateCommandHandler handler =
        new SpigotTemplateCommandHandler(dummyPlugin, application, definitions, helper);

    // templates/metin.spectra.zip does not exist in dataFolder -> IllegalArgumentException
    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"install", "metin"}));
  }

  @Test
  void testInstallMetinCorruptArchiveThrowsIllegalArgumentException() throws IOException {
    SpigotTemplateCommandHandler handler =
        new SpigotTemplateCommandHandler(dummyPlugin, application, definitions, helper);

    Path templatesDir = tempDir.resolve("templates");
    Files.createDirectories(templatesDir);
    Path archive = templatesDir.resolve("metin.spectra.zip");
    Files.write(archive, new byte[] {0, 1, 2, 3, 4, 5});

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"install", "metin"}));
  }

  @Test
  void testInstallMetinIOExceptionThrowsIllegalStateException() throws IOException {
    SpigotTemplateCommandHandler handler =
        new SpigotTemplateCommandHandler(dummyPlugin, application, definitions, helper);

    Path templatesDir = tempDir.resolve("templates");
    Files.createDirectories(templatesDir);
    Path archive = templatesDir.resolve("metin.spectra.zip");

    try (InputStream in =
        SpectraEventsApplication.class.getResourceAsStream("/templates/metin.spectra.zip")) {
      if (in != null) {
        Files.copy(in, archive);
        // Create events as a file instead of a directory so extraction encounters
        // FileAlreadyExistsException (IOException)
        Files.createFile(tempDir.resolve("events"));
        assertThrows(
            IllegalStateException.class,
            () -> handler.execute(sender, new String[] {"install", "metin"}));
      }
    }
  }

  @Test
  void testInstallMetinValidationFailsThrowsIllegalStateException() throws IOException {
    SpigotTemplateCommandHandler handler =
        new SpigotTemplateCommandHandler(dummyPlugin, application, definitions, helper);

    Path templatesDir = tempDir.resolve("templates");
    Files.createDirectories(templatesDir);
    Path archive = templatesDir.resolve("metin.spectra.zip");

    // Put a broken event file in events/ that is not overwritten by the bundle
    Path eventsDir = tempDir.resolve("events");
    Files.createDirectories(eventsDir);
    Files.writeString(eventsDir.resolve("broken.yml"), "broken: [invalid");

    try (InputStream in =
        SpectraEventsApplication.class.getResourceAsStream("/templates/metin.spectra.zip")) {
      if (in != null) {
        Files.copy(in, archive);
        assertThrows(
            IllegalStateException.class,
            () -> handler.execute(sender, new String[] {"install", "metin"}));
      }
    }
  }

  @Test
  void testInstallMetinValidArchiveSucceeds() throws IOException {
    SpigotTemplateCommandHandler handler =
        new SpigotTemplateCommandHandler(dummyPlugin, application, definitions, helper);

    Path templatesDir = tempDir.resolve("templates");
    Files.createDirectories(templatesDir);
    Path archive = templatesDir.resolve("metin.spectra.zip");

    try (InputStream in =
        SpectraEventsApplication.class.getResourceAsStream("/templates/metin.spectra.zip")) {
      if (in != null) {
        Files.copy(in, archive);
        messages.clear();
        handler.execute(sender, new String[] {"install", "metin"});
        assertFalse(messages.isEmpty());
        assertTrue(messages.stream().anyMatch(m -> m.contains("Template metin is installed")));
      }
    }
  }

  @Test
  void tabCompletesSubcommands() {
    SpigotTemplateCommandHandler handler =
        new SpigotTemplateCommandHandler(dummyPlugin, application, definitions, helper);

    List<String> completions = handler.tabComplete(sender, new String[] {""});
    assertEquals(List.of("list", "install"), completions);

    List<String> installCompletions = handler.tabComplete(sender, new String[] {"install", "m"});
    assertEquals(List.of("metin"), installCompletions);

    List<String> nonInstallTwoArgCompletions =
        handler.tabComplete(sender, new String[] {"other", "m"});
    assertTrue(nonInstallTwoArgCompletions.isEmpty());

    List<String> threeArgCompletions =
        handler.tabComplete(sender, new String[] {"install", "metin", "extra"});
    assertTrue(threeArgCompletions.isEmpty());

    List<String> nullSender = handler.tabComplete(null, new String[] {""});
    assertTrue(nullSender.isEmpty());

    CommandSender noPermSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });
    assertTrue(handler.tabComplete(noPermSender, new String[] {""}).isEmpty());
    assertTrue(handler.tabComplete(sender, new String[] {"other", "arg"}).isEmpty());
  }
}
