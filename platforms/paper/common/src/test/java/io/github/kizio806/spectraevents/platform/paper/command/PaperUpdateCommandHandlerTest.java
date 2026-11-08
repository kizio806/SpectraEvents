package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.update.UpdateCheckStatus;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdatePort;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaperUpdateCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private UpdateService updateService;
  private RegionTaskScheduler scheduler;
  private LocaleCatalog locales;
  private UpdateCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private final AtomicReference<CompletableFuture<UpdateInfo>> mockResult = new AtomicReference<>();

  @BeforeEach
  void setUp() {
    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            CompletableFuture<UpdateInfo> future = mockResult.get();
            if (future != null) {
              return future;
            }
            return CompletableFuture.completedFuture(
                new UpdateInfo(
                    version,
                    "2.0.0",
                    true,
                    "https://example.com/dl",
                    "Major update",
                    Instant.now(),
                    UpdateCheckStatus.UPDATE_AVAILABLE));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(true);
          }
        };

    updateService = new UpdateService("1.0.0", port);
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
    localeMap.put("command.update.checking", "Checking for updates...");
    localeMap.put("command.update.available", "Available: %latest%");
    localeMap.put("command.update.failed", "Failed: %reason%");
    localeMap.put("command.update.current", "Current: %version%");
    localeMap.put("command.update.info-title", "Update Info");
    localeMap.put("command.update.current-label", "Current: ");
    localeMap.put("command.update.latest-label", "Latest: ");
    localeMap.put("command.update.status-label", "Status: ");
    localeMap.put("command.update.status-available", "Available");
    localeMap.put("command.update.status-failed", "Failed");
    localeMap.put("command.update.status-not-checked", "Not checked");
    localeMap.put("command.update.status-current", "Current");
    localeMap.put("command.update.notes-label", "Notes: ");

    locales = LocaleCatalog.of(localeMap);

    handler = new UpdateCommandHandler(updateService, scheduler, locales);
    messages = new ArrayList<>();
    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
  }

  @Test
  void testUpdateCheckAndInfoWithConsole() throws Exception {
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    CommandSourceStack source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher.execute("update check", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("update info", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testUpdateCheckWithPlayer() throws Exception {
    Player player = PaperCommandTestSupport.createRecordingPlayer(messages, null);
    CommandSourceStack source = PaperCommandTestSupport.createMockSource(player, null);

    dispatcher.execute("update check", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testUpdateCheckVariants() throws Exception {
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    CommandSourceStack source = PaperCommandTestSupport.createMockSource(sender, null);

    // 1. Check with failure status
    mockResult.set(
        CompletableFuture.completedFuture(
            new UpdateInfo(
                "1.0.0",
                "1.0.0",
                false,
                "",
                "Connection timeout",
                Instant.now(),
                UpdateCheckStatus.FAILED)));
    messages.clear();
    dispatcher.execute("update check", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("update info", source);
    assertFalse(messages.isEmpty());

    // 2. Check with current / up to date
    mockResult.set(
        CompletableFuture.completedFuture(
            new UpdateInfo(
                "1.0.0", "1.0.0", false, "", "", Instant.now(), UpdateCheckStatus.UP_TO_DATE)));
    messages.clear();
    dispatcher.execute("update check", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("update info", source);
    assertFalse(messages.isEmpty());

    // 3. Check with thrown exception
    mockResult.set(CompletableFuture.failedFuture(new RuntimeException("Network down")));
    messages.clear();
    dispatcher.execute("update check", source);
    assertFalse(messages.isEmpty());

    // 4. Info when NOT_CHECKED
    UpdateService freshService =
        new UpdateService(
            "1.0.0",
            new UpdatePort() {
              @Override
              public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
                return CompletableFuture.completedFuture(null);
              }

              @Override
              public CompletableFuture<Boolean> downloadUpdate(String url, String target) {
                return CompletableFuture.completedFuture(true);
              }
            });
    UpdateCommandHandler freshHandler = new UpdateCommandHandler(freshService, scheduler, locales);
    CommandDispatcher<CommandSourceStack> freshDispatcher =
        PaperCommandTestSupport.createDispatcher();
    freshDispatcher.register(freshHandler.build());

    messages.clear();
    freshDispatcher.execute("update info", source);
    assertFalse(messages.isEmpty());
  }
}
