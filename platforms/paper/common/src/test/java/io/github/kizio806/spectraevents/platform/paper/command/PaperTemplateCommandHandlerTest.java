package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.configuration.PluginMeta;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperTemplateCommandHandlerTest {

  @TempDir Path tempDir;
  private Plugin mockPlugin;
  private SpectraEventsApplication application;
  private BukkitDefinitionConfigBootstrap configBootstrap;
  private RegionTaskScheduler scheduler;
  private TemplateCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private CommandSourceStack source;

  @BeforeEach
  void setUp() throws Exception {
    PluginMeta mockMeta =
        (PluginMeta)
            Proxy.newProxyInstance(
                PluginMeta.class.getClassLoader(),
                new Class<?>[] {PluginMeta.class},
                (p, m, a) -> {
                  if ("getVersion".equals(m.getName())) return "1.0.0";
                  return null;
                });

    mockPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (p, m, a) -> {
                  if ("getDataFolder".equals(m.getName())) return tempDir.toFile();
                  if ("getPluginMeta".equals(m.getName())) return mockMeta;
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

    io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader loader =
        new io.github.kizio806.spectraevents.application.config.loader.DefinitionLoader(
            new io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser(),
            new io.github.kizio806.spectraevents.application.config.compiler
                .EventDefinitionCompiler(),
            application.definitionRegistry());

    configBootstrap = new BukkitDefinitionConfigBootstrap(mockPlugin, loader);

    scheduler =
        new RegionTaskScheduler() {
          @Override
          public void executeGlobal(Runnable task) {
            task.run();
          }

          @Override
          public void executeAt(Location location, Runnable task) {
            task.run();
          }

          @Override
          public void executeFor(Entity entity, Runnable task) {
            task.run();
          }
        };

    handler =
        new TemplateCommandHandler(
            mockPlugin, application, configBootstrap, scheduler, Runnable::run);
    messages = new ArrayList<>();
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
  }

  @Test
  void testListTemplates() throws Exception {
    dispatcher.execute("template list", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testInstallUnknownTemplate() throws Exception {
    dispatcher.execute("template install non_existent", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testInstallMetinMissingArchiveFails() throws Exception {
    // templates/metin.spectra.zip does not exist in dataFolder
    dispatcher.execute("template install metin", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testInstallMetinValidArchiveSucceeds() throws Exception {
    Path templatesDir = tempDir.resolve("templates");
    Files.createDirectories(templatesDir);
    Path archive = templatesDir.resolve("metin.spectra.zip");

    createBundleZip(archive, "metin");
    messages.clear();
    dispatcher.execute("template install metin", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testInstallFailsWhenReloadReturnsFailures() throws Exception {
    Path templatesDir = tempDir.resolve("templates");
    Files.createDirectories(templatesDir);
    Path archive = templatesDir.resolve("metin.spectra.zip");
    createBundleZip(archive, "metin");

    DefinitionLoader loader =
        new DefinitionLoader(
            new io.github.kizio806.spectraevents.application.config.yaml.EventSpecYamlParser(),
            new io.github.kizio806.spectraevents.application.config.compiler
                .EventDefinitionCompiler(),
            application.definitionRegistry());

    BukkitDefinitionConfigBootstrap failingReloadBootstrap =
        new BukkitDefinitionConfigBootstrap(mockPlugin, loader) {
          @Override
          public DefinitionLoadResult reloadFromDisk() {
            return new DefinitionLoadResult(
                List.of(),
                List.of(new DefinitionLoadResult.SourceFileFailure("broken.yml", List.of())));
          }
        };

    TemplateCommandHandler failHandler =
        new TemplateCommandHandler(
            mockPlugin, application, failingReloadBootstrap, scheduler, Runnable::run);
    CommandDispatcher<CommandSourceStack> failDispatcher =
        PaperCommandTestSupport.createDispatcher();
    failDispatcher.register(failHandler.build());

    messages.clear();
    failDispatcher.execute("template install metin", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testPermissions() throws Exception {
    CommandSender adminSender =
        createPermissionSender(messages, Set.of("spectraevents.admin"), false);
    CommandSourceStack adminSource = PaperCommandTestSupport.createMockSource(adminSender, null);
    messages.clear();
    dispatcher.execute("template list", adminSource);
    assertFalse(messages.isEmpty());

    CommandSender opSender = createPermissionSender(messages, Set.of(), true);
    CommandSourceStack opSource = PaperCommandTestSupport.createMockSource(opSender, null);
    messages.clear();
    dispatcher.execute("template list", opSource);
    assertFalse(messages.isEmpty());

    CommandSender noPermSender = createPermissionSender(messages, Set.of(), false);
    CommandSourceStack noPermSource = PaperCommandTestSupport.createMockSource(noPermSender, null);
    messages.clear();
    assertThrows(
        com.mojang.brigadier.exceptions.CommandSyntaxException.class,
        () -> dispatcher.execute("template list", noPermSource));
  }

  @Test
  void testInstallTemplateExceptionWithNullCauseMessage() throws Exception {
    Plugin badPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (p, m, a) -> {
                  if ("getDataFolder".equals(m.getName())) {
                    throw new RuntimeException(new NullPointerException());
                  }
                  return null;
                });
    TemplateCommandHandler badHandler =
        new TemplateCommandHandler(
            badPlugin, application, configBootstrap, scheduler, Runnable::run);
    CommandDispatcher<CommandSourceStack> badDispatcher =
        PaperCommandTestSupport.createDispatcher();
    badDispatcher.register(badHandler.build());

    messages.clear();
    badDispatcher.execute("template install metin", source);
    assertFalse(messages.isEmpty());
  }

  private static CommandSender createPermissionSender(
      List<Component> recordedMessages, Set<String> permissions, boolean isOp) {
    return (CommandSender)
        Proxy.newProxyInstance(
            CommandSender.class.getClassLoader(),
            new Class<?>[] {CommandSender.class},
            (p, m, a) -> {
              if ("sendMessage".equals(m.getName())) {
                if (a != null && a.length > 0 && a[0] instanceof Component c) {
                  recordedMessages.add(c);
                }
                return null;
              }
              if ("hasPermission".equals(m.getName()) && a != null && a.length > 0) {
                return permissions.contains(String.valueOf(a[0]));
              }
              if ("getName".equals(m.getName())) {
                return "PermSender";
              }
              if ("isOp".equals(m.getName())) {
                return isOp;
              }
              return null;
            });
  }

  private static void createBundleZip(Path zipPath, String id) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
      zos.putNextEntry(new ZipEntry("manifest.yml"));
      String manifest =
          """
          schema-version: 1
          id: TEMPLATE_ID
          version: 1.0.0
          minimum-spectraevents-version: 1.0.0
          event-definitions:
            - event.yml
          assets:
            - models/metin_stone.bbmodel
          dependencies: []
          """
              .replace("TEMPLATE_ID", id);
      zos.write(manifest.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();

      zos.putNextEntry(new ZipEntry("event.yml"));
      String def =
          """
          schema-version: "1"
          id: TEMPLATE_ID
          initial-phase: start
          phases:
            start:
              on-enter: []
          """
              .replace("TEMPLATE_ID", id);
      zos.write(def.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();

      zos.putNextEntry(new ZipEntry("models/metin_stone.bbmodel"));
      zos.write("dummy-model".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
  }

  @Test
  void testSuggestions() {
    var suggestions =
        dispatcher.getCompletionSuggestions(dispatcher.parse("template install m", source)).join();
    assertNotNull(suggestions);
    assertFalse(suggestions.getList().isEmpty());
  }
}
