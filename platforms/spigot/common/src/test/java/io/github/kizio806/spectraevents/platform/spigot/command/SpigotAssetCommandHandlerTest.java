package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.asset.AssetPipelineService;
import io.github.kizio806.spectraevents.application.asset.AssetTargetProfile;
import io.github.kizio806.spectraevents.application.asset.ResourcePackBuilder;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.port.AssetImportPort;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotAssetCommandHandlerTest {

  @TempDir Path tempDir;
  private final List<String> sentMessages = new ArrayList<>();
  private SpigotCommandHelper helper;
  private CommandSender sender;
  private AssetPipelineService assetPipeline;
  private Path sourceDir;
  private Path outputDir;

  @BeforeEach
  void setUp() throws Exception {
    sentMessages.clear();
    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "command.assets.build-complete", "Resource pack build finished successfully.",
                "command.assets.usage", "Usage: /spectraevents asset build",
                "command.spigot.failed", "Action failed: %reason%"));
    helper = new SpigotCommandHelper(locales);

    sender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return true;
                  }
                  if ("sendMessage".equals(method.getName())) {
                    if (args != null && args.length > 0 && args[0] != null) {
                      sentMessages.add(args[0].toString());
                    }
                    return null;
                  }
                  return null;
                });

    sourceDir = tempDir.resolve("sources");
    outputDir = tempDir.resolve("output");
    Files.createDirectories(sourceDir);
    Files.createDirectories(outputDir);

    AssetImportPort importPort = path -> null;

    assetPipeline =
        new AssetPipelineService(
            importPort,
            new ResourcePackBuilder(outputDir),
            sourceDir,
            AssetTargetProfile.PROFILE_26_1);
  }

  @Test
  void rejectsNullConstructorParameters() {
    assertThrows(NullPointerException.class, () -> new SpigotAssetCommandHandler(null, helper));
    assertThrows(
        NullPointerException.class, () -> new SpigotAssetCommandHandler(assetPipeline, null));
  }

  @Test
  void tabCompletesBuild() {
    SpigotAssetCommandHandler handler = new SpigotAssetCommandHandler(assetPipeline, helper);

    List<String> completions = handler.tabComplete(sender, new String[] {""});
    assertEquals(List.of("build"), completions);

    List<String> filtered = handler.tabComplete(sender, new String[] {"b"});
    assertEquals(List.of("build"), filtered);

    List<String> empty = handler.tabComplete(sender, new String[] {"other"});
    assertTrue(empty.isEmpty());

    List<String> extraArgs = handler.tabComplete(sender, new String[] {"build", "extra"});
    assertTrue(extraArgs.isEmpty());
  }

  @Test
  void printsUsageWhenNoArgsOrNonBuildArg() {
    SpigotAssetCommandHandler handler = new SpigotAssetCommandHandler(assetPipeline, helper);

    sentMessages.clear();
    handler.execute(sender, new String[0]);
    assertEquals(1, sentMessages.size());
    assertTrue(sentMessages.getFirst().contains("Usage:"));

    sentMessages.clear();
    handler.execute(sender, new String[] {"other"});
    assertEquals(1, sentMessages.size());
    assertTrue(sentMessages.getFirst().contains("Usage:"));
  }

  @Test
  void executesBuildSuccessfully() {
    SpigotAssetCommandHandler handler = new SpigotAssetCommandHandler(assetPipeline, helper);

    handler.execute(sender, new String[] {"build"});
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("successfully")));
  }

  @Test
  void executesBuildFailureWithReason() throws Exception {
    Path modelFile = sourceDir.resolve("stone.bbmodel");
    Files.writeString(modelFile, "{}");

    AssetImportPort failingPort =
        path -> {
          throw new IllegalStateException("Corrupt model definition");
        };

    AssetPipelineService failingPipeline =
        new AssetPipelineService(
            failingPort,
            new ResourcePackBuilder(outputDir),
            sourceDir,
            AssetTargetProfile.PROFILE_26_1);

    SpigotAssetCommandHandler handler = new SpigotAssetCommandHandler(failingPipeline, helper);

    sentMessages.clear();
    handler.execute(sender, new String[] {"build"});
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("Corrupt model definition")));
  }

  @Test
  void executesBuildFailureWithNullReason() throws Exception {
    Path modelFile = sourceDir.resolve("stone.bbmodel");
    Files.writeString(modelFile, "{}");

    // Throwing RuntimeException with null message propagates out of buildAssets to
    // SpigotAssetCommandHandler
    AssetImportPort nullPort =
        path -> {
          throw new RuntimeException((String) null);
        };

    AssetPipelineService nullPipeline =
        new AssetPipelineService(
            nullPort,
            new ResourcePackBuilder(outputDir),
            sourceDir,
            AssetTargetProfile.PROFILE_26_1);

    SpigotAssetCommandHandler handler = new SpigotAssetCommandHandler(nullPipeline, helper);

    sentMessages.clear();
    handler.execute(sender, new String[] {"build"});
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("unknown error")));
  }

  @Test
  void enforcesPermission() {
    CommandSender unprivilegedSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return false;
                  }
                  if ("isOp".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });

    SpigotAssetCommandHandler handler = new SpigotAssetCommandHandler(assetPipeline, helper);

    assertThrows(
        IllegalStateException.class,
        () -> handler.execute(unprivilegedSender, new String[] {"build"}));
  }
}
