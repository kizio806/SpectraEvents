package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetDocument;
import io.github.kizio806.spectraevents.core.visual.asset.SpectraAssetNode;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperAssetCommandHandlerTest {

  @TempDir Path tempDir;
  private AssetPipelineService assetPipeline;
  private RegionTaskScheduler scheduler;
  private LocaleCatalog locales;
  private AssetCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private CommandSourceStack source;
  private Path sourceDir;
  private Path outputDir;

  @BeforeEach
  void setUp() throws Exception {
    sourceDir = tempDir.resolve("sources");
    outputDir = tempDir.resolve("output");
    Files.createDirectories(sourceDir);
    Files.createDirectories(outputDir);

    AssetImportPort importPort =
        path -> {
          Path fileName = path.getFileName();
          if (fileName != null && fileName.toString().contains("broken")) {
            throw new RuntimeException("Simulated import error");
          }
          return new SpectraAssetDocument(
              1,
              "metin_stone",
              Map.of(),
              List.of(
                  new SpectraAssetNode(
                      "root",
                      io.github.kizio806.spectraevents.core.visual.model.Vector3.ZERO,
                      io.github.kizio806.spectraevents.core.visual.model.Vector3.ZERO,
                      io.github.kizio806.spectraevents.core.visual.model.EulerRotation.ZERO,
                      io.github.kizio806.spectraevents.core.visual.model.Vector3.ONE,
                      List.of(),
                      List.of())),
              Map.of());
        };

    assetPipeline =
        new AssetPipelineService(
            importPort,
            new ResourcePackBuilder(outputDir),
            sourceDir,
            AssetTargetProfile.PROFILE_26_1);

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

    Map<String, String> localeMap = new java.util.HashMap<>();
    localeMap.put("command.assets.build-started", "Building pack...");
    localeMap.put("command.assets.build-complete", "Pack built!");
    localeMap.put("command.assets.build-failed", "Build failed: %reason%");
    localeMap.put("command.assets.list-title", "Assets (%count%):");
    localeMap.put("command.assets.entry", " - %id%");
    localeMap.put("command.assets.imported", "Imported %file%!");
    localeMap.put("command.assets.import-failed", "Import failed: %reason%");
    localeMap.put("command.assets.missing", "Asset %id% not found");
    localeMap.put("command.assets.info-title", "Asset %id%");
    localeMap.put("command.assets.nodes", "Nodes: %count%");
    localeMap.put("command.assets.textures", "Textures: %count%");
    localeMap.put("command.assets.animations", "Animations: %count%");
    localeMap.put("command.assets.valid", "Asset %id% is valid");
    localeMap.put("command.assets.invalid", "Asset %id% is invalid");
    localeMap.put("command.assets.unavailable", "Asset pipeline unavailable");

    locales = LocaleCatalog.of(localeMap);

    handler = new AssetCommandHandler(scheduler, assetPipeline, locales);
    messages = new ArrayList<>();
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
  }

  @Test
  void testBuildAndListAssets() throws Exception {
    // Put a dummy model file in sourceDir
    Files.writeString(sourceDir.resolve("metin_stone.bbmodel"), "dummy-content");

    dispatcher.execute("assets build", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("assets list", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testImportInfoAndValidate() throws Exception {
    Files.writeString(sourceDir.resolve("metin_stone.bbmodel"), "dummy-content");
    assetPipeline.buildAssets();

    // Info for existing model
    messages.clear();
    dispatcher.execute("assets info \"metin_stone\"", source);
    assertFalse(messages.isEmpty());

    // Info for missing model
    messages.clear();
    dispatcher.execute("assets info \"missing_asset\"", source);
    assertFalse(messages.isEmpty());

    // Validate valid vs invalid model
    messages.clear();
    dispatcher.execute("assets validate \"metin_stone\"", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("assets validate \"non_existent\"", source);
    assertFalse(messages.isEmpty());

    // Import file successfully
    Path validFile = sourceDir.resolve("valid.bbmodel");
    Files.writeString(validFile, "content");
    messages.clear();
    dispatcher.execute("assets import \"valid.bbmodel\"", source);
    awaitMessages();
    assertFalse(messages.isEmpty());

    // Import file with failure
    Path brokenFile = sourceDir.resolve("broken.bbmodel");
    Files.writeString(brokenFile, "content");
    messages.clear();
    dispatcher.execute("assets import \"broken.bbmodel\"", source);
    awaitMessages();
    assertFalse(messages.isEmpty());
  }

  @Test
  void testPlayerSourceReplyLater() throws Exception {
    Player player = PaperCommandTestSupport.createRecordingPlayer(messages, null);
    CommandSourceStack playerSource = PaperCommandTestSupport.createMockSource(player, null);

    messages.clear();
    dispatcher.execute("assets build", playerSource);
    assertFalse(messages.isEmpty());

    Path validFile = sourceDir.resolve("player_import.bbmodel");
    Files.writeString(validFile, "content");
    messages.clear();
    dispatcher.execute("assets import \"player_import.bbmodel\"", playerSource);
    awaitMessages();
    assertFalse(messages.isEmpty());
  }

  @Test
  void testBuildAssetsFailureBranches() throws Exception {
    AssetPipelineService failingPipeline =
        new AssetPipelineService(
            path -> {
              throw new RuntimeException("Simulated build failure");
            },
            new ResourcePackBuilder(outputDir),
            sourceDir,
            AssetTargetProfile.PROFILE_26_1);
    Files.writeString(sourceDir.resolve("failing.bbmodel"), "content");
    AssetCommandHandler failHandler = new AssetCommandHandler(scheduler, failingPipeline, locales);
    CommandDispatcher<CommandSourceStack> failDispatcher =
        PaperCommandTestSupport.createDispatcher();
    failDispatcher.register(failHandler.build());

    messages.clear();
    failDispatcher.execute("assets build", source);
    awaitMessages();
    assertFalse(messages.isEmpty());

    // Wrapped cause with null message
    AssetPipelineService nullMessagePipeline =
        new AssetPipelineService(
            path -> {
              throw new RuntimeException(new NullPointerException());
            },
            new ResourcePackBuilder(outputDir),
            sourceDir,
            AssetTargetProfile.PROFILE_26_1);
    AssetCommandHandler nullMsgHandler =
        new AssetCommandHandler(scheduler, nullMessagePipeline, locales);
    CommandDispatcher<CommandSourceStack> nullMsgDispatcher =
        PaperCommandTestSupport.createDispatcher();
    nullMsgDispatcher.register(nullMsgHandler.build());

    messages.clear();
    nullMsgDispatcher.execute("assets build", source);
    awaitMessages();
    assertFalse(messages.isEmpty());
  }

  @Test
  void testPermissions() throws Exception {
    // Sender with admin permission
    CommandSender adminSender =
        createPermissionSender(messages, Set.of("spectraevents.admin"), false);
    CommandSourceStack adminSource = PaperCommandTestSupport.createMockSource(adminSender, null);
    messages.clear();
    dispatcher.execute("assets list", adminSource);
    assertFalse(messages.isEmpty());

    // Sender with isOp
    CommandSender opSender = createPermissionSender(messages, Set.of(), true);
    CommandSourceStack opSource = PaperCommandTestSupport.createMockSource(opSender, null);
    messages.clear();
    dispatcher.execute("assets list", opSource);
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

  private void awaitMessages() throws InterruptedException {
    long deadline = System.currentTimeMillis() + 1000;
    while (messages.isEmpty() && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
  }

  @Test
  void testNullPipelineServiceUnavailable() throws Exception {
    AssetCommandHandler nullHandler = new AssetCommandHandler(scheduler, null, locales);
    CommandDispatcher<CommandSourceStack> nullDispatcher =
        PaperCommandTestSupport.createDispatcher();
    nullDispatcher.register(nullHandler.build());

    messages.clear();
    nullDispatcher.execute("assets build", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    nullDispatcher.execute("assets list", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    nullDispatcher.execute("assets import \"any\"", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    nullDispatcher.execute("assets info \"any\"", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    nullDispatcher.execute("assets validate \"any\"", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testSuggestions() throws Exception {
    Files.writeString(sourceDir.resolve("metin_stone.bbmodel"), "dummy-content");
    assetPipeline.buildAssets();

    var suggestions =
        dispatcher.getCompletionSuggestions(dispatcher.parse("assets info met", source)).join();
    assertFalse(suggestions.getList().isEmpty());
  }
}
