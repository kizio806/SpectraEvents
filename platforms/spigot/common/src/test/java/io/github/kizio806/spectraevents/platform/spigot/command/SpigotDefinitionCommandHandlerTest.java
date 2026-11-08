package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
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
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotDefinitionCommandHandlerTest {

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
    Map<String, String> localeMap = new HashMap<>();
    localeMap.put("command.spigot.definitions-title", "Definitions (%count%):");
    localeMap.put("command.spigot.definition-entry", " - %definition%");
    localeMap.put("command.spigot.definitions-reloaded", "Reloaded %loaded% (%failed% failed)");
    localeMap.put("command.spigot.definitions-validated", "Validated %loaded% (%failed% failed)");
    localeMap.put("command.spigot.diagnostic-entry", "%path%: %message%");

    LocaleCatalog locales = LocaleCatalog.of(localeMap);
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
                    return java.util.logging.Logger.getLogger("test");
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

    io.github.kizio806.spectraevents.application.port.AssetImportPort importPort =
        (io.github.kizio806.spectraevents.application.port.AssetImportPort)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.AssetImportPort.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.AssetImportPort.class
                },
                (proxy, method, args) -> null);

    io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder packBuilder =
        new io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder(
            tempDir.resolve("rp-output"));

    io.github.kizio806.spectraevents.application.asset.AssetPipelineService pipeline =
        new io.github.kizio806.spectraevents.application.asset.AssetPipelineService(
            importPort,
            packBuilder,
            tempDir.resolve("assets"),
            io.github.kizio806.spectraevents.application.asset.AssetTargetProfile.PROFILE_26_1);
    application.setAssetPipelineService(pipeline);

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
        () -> new SpigotDefinitionCommandHandler(null, null, null, helper));
  }

  @Test
  void testListDefinitions() {
    SpigotDefinitionCommandHandler handler =
        new SpigotDefinitionCommandHandler(dummyPlugin, application, definitions, helper);

    // Empty list
    handler.execute(sender, new String[] {});
    assertFalse(messages.isEmpty());

    // Non-empty list
    messages.clear();
    PhaseDefinition phase =
        new PhaseDefinition(new PhaseId("start"), Set.of(), List.of(), List.of());
    EventDefinition def =
        new EventDefinition(
            new EventDefinitionId("meteor"),
            new PhaseId("start"),
            Map.of(new PhaseId("start"), phase));
    application.definitionRegistry().register(def, "events/meteor.yml");

    handler.execute(sender, new String[] {"list"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("meteor")));
  }

  @Test
  void testReloadAndValidateDefinitions() throws IOException {
    SpigotDefinitionCommandHandler handler =
        new SpigotDefinitionCommandHandler(dummyPlugin, application, definitions, helper);

    // Reload empty
    messages.clear();
    handler.execute(sender, new String[] {"reload"});
    assertFalse(messages.isEmpty());

    // Validate empty (triggers warning SE-READY-PACK-001)
    messages.clear();
    handler.execute(sender, new String[] {"validate"});
    assertFalse(messages.isEmpty());

    // Write resource-pack.yml with required: true and enabled: false to trigger ERROR
    // SE-READY-PACK-003
    Files.writeString(
        tempDir.resolve("resource-pack.yml"),
        "enabled: false\nrequired: true\nsource: manual\nurl: \"\"\nsha1: \"\"\n");
    messages.clear();
    handler.execute(sender, new String[] {"validate"});
    assertFalse(messages.isEmpty());

    // Write a broken YAML file in events/ directory to trigger failures
    Path eventsDir = tempDir.resolve("events");
    Files.createDirectories(eventsDir);
    Files.writeString(eventsDir.resolve("broken.yml"), "invalid: [broken yaml");

    messages.clear();
    handler.execute(sender, new String[] {"validate"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("broken.yml")));

    messages.clear();
    handler.execute(sender, new String[] {"reload"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("broken.yml")));
  }

  @Test
  void testReloadOrValidateThrowsOnIoException() {
    DefinitionLoader loader =
        new DefinitionLoader(
            new EventSpecYamlParser(),
            new EventDefinitionCompiler(),
            new EventDefinitionRegistry());

    BukkitDefinitionConfigBootstrap brokenDefinitions =
        new BukkitDefinitionConfigBootstrap(dummyPlugin, loader) {
          @Override
          public io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult
              reloadFromDisk() throws IOException {
            throw new IOException("Simulated reload error");
          }

          @Override
          public io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult
              validateFromDisk() throws IOException {
            throw new IOException("Simulated validate error");
          }
        };

    SpigotDefinitionCommandHandler brokenHandler =
        new SpigotDefinitionCommandHandler(dummyPlugin, application, brokenDefinitions, helper);

    assertThrows(
        IllegalStateException.class, () -> brokenHandler.execute(sender, new String[] {"reload"}));
    assertThrows(
        IllegalStateException.class,
        () -> brokenHandler.execute(sender, new String[] {"validate"}));
  }

  @Test
  void testUnknownSubcommandThrowsException() {
    SpigotDefinitionCommandHandler handler =
        new SpigotDefinitionCommandHandler(dummyPlugin, application, definitions, helper);

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"unknown_sub"}));
  }

  @Test
  void tabCompletesSubcommands() {
    SpigotDefinitionCommandHandler handler =
        new SpigotDefinitionCommandHandler(dummyPlugin, application, definitions, helper);

    List<String> completions = handler.tabComplete(sender, new String[] {""});
    assertEquals(List.of("list", "reload", "validate"), completions);

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
    assertTrue(handler.tabComplete(sender, new String[] {"list", "extra"}).isEmpty());
  }
}
