package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.update.UpdateCheckStatus;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdatePort;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpigotUpdateCommandHandlerTest {

  private final List<String> sentMessages = new ArrayList<>();
  private LocaleCatalog locales;
  private SpigotCommandHelper helper;
  private CommandSender sender;

  @BeforeEach
  void setUp() {
    sentMessages.clear();
    locales =
        LocaleCatalog.of(
            Map.of(
                "command.update.checking", "Checking for updates...",
                "command.update.up-to-date", "You are running the latest version.",
                "command.update.available", "New version available: %latest%",
                "command.update.check-failed", "Check failed: %reason%",
                "command.update.usage", "Usage: /spectraevents update check"));
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
  }

  @Test
  void rejectsNullConstructorParameters() {
    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(UpdateInfo.upToDate(version));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };
    UpdateService service = new UpdateService("1.0.0", port);
    assertThrows(NullPointerException.class, () -> new SpigotUpdateCommandHandler(null, helper));
    assertThrows(NullPointerException.class, () -> new SpigotUpdateCommandHandler(service, null));
  }

  @Test
  void tabCompletesSubcommand() {
    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(UpdateInfo.upToDate(version));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService service = new UpdateService("1.0.0", port);
    SpigotUpdateCommandHandler handler = new SpigotUpdateCommandHandler(service, helper);

    List<String> completions = handler.tabComplete(sender, new String[] {""});
    assertEquals(List.of("check"), completions);

    List<String> filtered = handler.tabComplete(sender, new String[] {"ch"});
    assertEquals(List.of("check"), filtered);

    List<String> empty = handler.tabComplete(sender, new String[] {"nomatch"});
    assertTrue(empty.isEmpty());

    List<String> extraArgs = handler.tabComplete(sender, new String[] {"check", "extra"});
    assertTrue(extraArgs.isEmpty());
  }

  @Test
  void printsUsageWhenNoArgumentsOrNonCheckArgument() {
    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(UpdateInfo.upToDate(version));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService service = new UpdateService("1.0.0", port);
    SpigotUpdateCommandHandler handler = new SpigotUpdateCommandHandler(service, helper);

    sentMessages.clear();
    handler.execute(sender, new String[0]);
    assertEquals(1, sentMessages.size());
    assertTrue(sentMessages.getFirst().contains("Usage:"));

    sentMessages.clear();
    handler.execute(sender, new String[] {"something_else"});
    assertEquals(1, sentMessages.size());
    assertTrue(sentMessages.getFirst().contains("Usage:"));
  }

  @Test
  void handlesUpToDateResultAndNullResult() {
    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(UpdateInfo.upToDate("1.0.0"));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService service = new UpdateService("1.0.0", port);
    SpigotUpdateCommandHandler handler = new SpigotUpdateCommandHandler(service, helper);

    sentMessages.clear();
    handler.execute(sender, new String[] {"check"});
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("Checking for updates")));
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("latest version")));

    UpdatePort nullPort =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(null);
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };
    UpdateService nullService = new UpdateService("1.0.0", nullPort);
    SpigotUpdateCommandHandler nullHandler = new SpigotUpdateCommandHandler(nullService, helper);

    sentMessages.clear();
    nullHandler.execute(sender, new String[] {"check"});
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("latest version")));
  }

  @Test
  void handlesUpdateAvailableResult() {
    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(
                new UpdateInfo(
                    "1.0.0",
                    "1.2.0",
                    true,
                    "https://example.com/download",
                    "Release notes",
                    Instant.now(),
                    UpdateCheckStatus.UPDATE_AVAILABLE));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService service = new UpdateService("1.0.0", port);
    SpigotUpdateCommandHandler handler = new SpigotUpdateCommandHandler(service, helper);

    sentMessages.clear();
    handler.execute(sender, new String[] {"check"});
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("Checking for updates")));
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("New version available: 1.2.0")));
  }

  @Test
  void handlesFailureWithAndWithoutMessage() {
    UpdatePort failedPort =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.failedFuture(new RuntimeException("Connection timeout"));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService failedService = new UpdateService("1.0.0", failedPort);
    SpigotUpdateCommandHandler failedHandler =
        new SpigotUpdateCommandHandler(failedService, helper);

    sentMessages.clear();
    failedHandler.execute(sender, new String[] {"check"});
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("Connection timeout")));

    UpdatePort nullMsgPort =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.failedFuture(new NullMessageException());
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService nullMsgService = new UpdateService("1.0.0", nullMsgPort);
    SpigotUpdateCommandHandler nullMsgHandler =
        new SpigotUpdateCommandHandler(nullMsgService, helper);

    sentMessages.clear();
    nullMsgHandler.execute(sender, new String[] {"check"});
    assertTrue(sentMessages.stream().anyMatch(msg -> msg.contains("unknown")));
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

    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(UpdateInfo.upToDate("1.0.0"));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService service = new UpdateService("1.0.0", port);
    SpigotUpdateCommandHandler handler = new SpigotUpdateCommandHandler(service, helper);

    assertThrows(
        IllegalStateException.class,
        () -> handler.execute(unprivilegedSender, new String[] {"check"}));
  }

  private static final class NullMessageException extends CompletionException {
    private static final long serialVersionUID = 1L;

    @Override
    public String getMessage() {
      return null;
    }
  }
}
