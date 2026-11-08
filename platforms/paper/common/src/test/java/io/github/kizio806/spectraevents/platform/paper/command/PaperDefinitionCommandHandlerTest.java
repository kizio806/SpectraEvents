package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperDefinitionCommandHandlerTest {

  @TempDir Path tempDir;
  private EventDefinitionRegistry defRegistry;
  private BukkitDefinitionConfigBootstrap configBootstrap;
  private SpectraEventsApplication application;
  private Plugin mockPlugin;
  private IntegrationRegistry integrations;
  private LocaleCatalog locales;
  private DefinitionCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private CommandSourceStack source;

  @BeforeEach
  void setUp() {
    defRegistry = new EventDefinitionRegistry();
    mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (p, m, a) -> {
                  if ("getDataFolder".equals(m.getName())) return tempDir.toFile();
                  if ("getLogger".equals(m.getName())) {
                    return java.util.logging.Logger.getAnonymousLogger();
                  }
                  return null;
                });

    io.github.kizio806.spectraevents.application.port.LifecycleReporter mockReporter =
        (io.github.kizio806.spectraevents.application.port.LifecycleReporter)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.LifecycleReporter.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.LifecycleReporter.class
                },
                (p, m, a) -> null);

    application = new SpectraEventsApplication(mockReporter, null, null, null, null, null, null);
    io.github.kizio806.spectraevents.application.port.AssetImportPort importPort = p -> null;
    io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder packBuilder =
        new io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder(
            tempDir.resolve("rp"));
    io.github.kizio806.spectraevents.application.asset.AssetPipelineService assetPipeline =
        new io.github.kizio806.spectraevents.application.asset.AssetPipelineService(
            importPort,
            packBuilder,
            tempDir.resolve("assets"),
            io.github.kizio806.spectraevents.application.asset.AssetTargetProfile.PROFILE_26_1);
    application.setAssetPipelineService(assetPipeline);

    io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader loader =
        new io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader(
            new io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser(),
            new io.github.kizio806.spectraevents.application.config.compiler
                .EventDefinitionCompiler(),
            defRegistry);

    configBootstrap = new BukkitDefinitionConfigBootstrap(mockPlugin, loader);
    integrations = new IntegrationRegistry();

    locales =
        LocaleCatalog.of(
            Map.of(
                "command.definitions.title", "Definitions (%count%):",
                "command.definitions.entry-id", " - ID: ",
                "command.definitions.entry-source", " source: ",
                "command.definitions.reloaded", "Reloaded loaded=%loaded%, failed=%failed%",
                "command.definitions.reload-failed", "Reload failed: %reason%",
                "command.definitions.validation-passed", "Validation passed count=%count%",
                "command.definitions.validation-failed", "Validation failed count=%count%",
                "command.definitions.failure-source", "Failure in %source%",
                "command.definitions.diagnostic", "[%code%] %message%",
                "command.definitions.validation-error", "Validation error: %reason%"));

    handler =
        new DefinitionCommandHandler(
            defRegistry, configBootstrap, application, mockPlugin, integrations, locales);

    messages = new ArrayList<>();
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
    dispatcher.register(handler.buildRootValidationCommand());
  }

  @Test
  void testListDefinitions() throws Exception {
    dispatcher.execute("definition list", source);
    assertFalse(messages.isEmpty());

    // Register a definition and list again
    PhaseDefinition phase =
        new PhaseDefinition(new PhaseId("start"), Set.of(), List.of(), List.of());
    EventDefinition def =
        new EventDefinition(
            new EventDefinitionId("meteor"),
            new PhaseId("start"),
            Map.of(new PhaseId("start"), phase));
    defRegistry.register(def, "events/meteor.yml");

    messages.clear();
    dispatcher.execute("definition list", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testReloadDefinitions() throws Exception {
    dispatcher.execute("definition reload", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("definition reload \"all\"", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("definition reload \"meteor\"", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testValidateDefinitionsSuccessAndFailure() throws Exception {
    dispatcher.execute("definition validate", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("validate", source);
    assertFalse(messages.isEmpty());

    // Write an invalid YAML file in events/ directory to trigger validation failure
    Path eventsDir = tempDir.resolve("events");
    Files.createDirectories(eventsDir);
    Files.writeString(eventsDir.resolve("broken.yml"), "invalid: [yaml: broken");

    messages.clear();
    dispatcher.execute("definition validate", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testReloadAndValidateWithFailuresAndExceptions() throws Exception {
    ValidationDiagnostic errorDiag =
        new io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic(
            io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic
                .Severity.ERROR,
            "ERR01",
            "events/broken.yml",
            "Syntax error");
    ValidationDiagnostic warnDiag =
        new io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic(
            io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic
                .Severity.WARNING,
            "WARN01",
            "events/test.yml",
            "Deprecated field");
    ValidationDiagnostic infoDiag =
        new io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic(
            io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic
                .Severity.INFO,
            "INFO01",
            "events/test.yml",
            "Informational note");

    io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult
            .SourceFileFailure
        failure =
            new io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult
                .SourceFileFailure("events/broken.yml", List.of(errorDiag, warnDiag, infoDiag));

    io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult failureResult =
        new io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult(
            List.of(), List.of(failure));

    io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader loader =
        new io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader(
            new io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser(),
            new io.github.kizio806.spectraevents.application.config.compiler
                .EventDefinitionCompiler(),
            defRegistry);

    // Bootstrap returning failures
    BukkitDefinitionConfigBootstrap failingBootstrap =
        new BukkitDefinitionConfigBootstrap(mockPlugin, loader) {
          @Override
          public io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult
              reloadFromDisk() {
            return failureResult;
          }

          @Override
          public io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult
              validateFromDisk() {
            return failureResult;
          }
        };

    DefinitionCommandHandler failingHandler =
        new DefinitionCommandHandler(
            defRegistry, failingBootstrap, application, mockPlugin, integrations, locales);
    CommandDispatcher<CommandSourceStack> failingDispatcher =
        PaperCommandTestSupport.createDispatcher();
    failingDispatcher.register(failingHandler.build());

    // Write a resource-pack.yml that fails readiness validation with an ERROR diagnostic
    Files.writeString(tempDir.resolve("resource-pack.yml"), "enabled: false\nrequired: true\n");

    messages.clear();
    failingDispatcher.execute("definition reload", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    failingDispatcher.execute("definition validate", source);
    assertFalse(messages.isEmpty());

    // Bootstrap throwing exceptions
    BukkitDefinitionConfigBootstrap exceptionalBootstrap =
        new BukkitDefinitionConfigBootstrap(mockPlugin, loader) {
          @Override
          public io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult
              reloadFromDisk() {
            throw new RuntimeException("Simulated reload failure");
          }

          @Override
          public io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult
              validateFromDisk() {
            throw new RuntimeException("Simulated validate failure");
          }
        };

    DefinitionCommandHandler exceptionalHandler =
        new DefinitionCommandHandler(
            defRegistry, exceptionalBootstrap, application, mockPlugin, integrations, locales);
    CommandDispatcher<CommandSourceStack> exceptionalDispatcher =
        PaperCommandTestSupport.createDispatcher();
    exceptionalDispatcher.register(exceptionalHandler.build());

    messages.clear();
    exceptionalDispatcher.execute("definition reload", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    exceptionalDispatcher.execute("definition validate", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testReloadAndValidateWithIoException() throws Exception {
    // Point dataFolder to a regular file so Directory operations throw IOException
    Path blockedFile = tempDir.resolve("blocked_file");
    Files.writeString(blockedFile, "not a directory");

    Plugin brokenPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (p, m, a) -> {
                  if ("getDataFolder".equals(m.getName())) return blockedFile.toFile();
                  return null;
                });

    io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader loader =
        new io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader(
            new io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser(),
            new io.github.kizio806.spectraevents.application.config.compiler
                .EventDefinitionCompiler(),
            defRegistry);

    BukkitDefinitionConfigBootstrap brokenBootstrap =
        new BukkitDefinitionConfigBootstrap(brokenPlugin, loader);

    DefinitionCommandHandler brokenHandler =
        new DefinitionCommandHandler(
            defRegistry, brokenBootstrap, application, brokenPlugin, integrations, locales);

    CommandDispatcher<CommandSourceStack> brokenDispatcher =
        PaperCommandTestSupport.createDispatcher();
    brokenDispatcher.register(brokenHandler.build());

    messages.clear();
    brokenDispatcher.execute("definition reload", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    brokenDispatcher.execute("definition validate", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testSuggestions() {
    PhaseDefinition phase =
        new PhaseDefinition(new PhaseId("start"), Set.of(), List.of(), List.of());
    EventDefinition def =
        new EventDefinition(
            new EventDefinitionId("meteor"),
            new PhaseId("start"),
            Map.of(new PhaseId("start"), phase));
    defRegistry.register(def, "events/meteor.yml");

    var suggestionsAll =
        dispatcher.getCompletionSuggestions(dispatcher.parse("definition reload a", source)).join();
    assertFalse(suggestionsAll.getList().isEmpty());

    var suggestionsMeteor =
        dispatcher
            .getCompletionSuggestions(dispatcher.parse("definition reload met", source))
            .join();
    assertFalse(suggestionsMeteor.getList().isEmpty());
  }
}
