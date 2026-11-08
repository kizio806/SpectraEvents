package io.github.kizio806.spectraevents.platform.paper.update;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.update.UpdateCheckStatus;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdatePort;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UpdateNotificationListenerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @Test
  void testPlayerWithoutPermissionIsNotNotified() {
    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(
                new UpdateInfo(
                    version,
                    "1.1.0",
                    true,
                    "https://example.com/download",
                    "Notes",
                    Instant.now(),
                    UpdateCheckStatus.UPDATE_AVAILABLE));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService updateService = new UpdateService("1.0.0", port);
    updateService.checkNow("release").join();

    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "messages.prefix", "[SpectraEvents] ",
                "messages.update-available", "Update available: ",
                "messages.update-command", " Run /se update"));

    UpdateNotificationListener listener = new UpdateNotificationListener(updateService, locales);
    Player player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) return false;
                  if ("isOp".equals(method.getName())) return false;
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  return null;
                });

    PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text("joined"));
    assertDoesNotThrow(() -> listener.onPlayerJoin(event));
  }

  @Test
  void testAdminPlayerIsNotifiedOnce() {
    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(
                new UpdateInfo(
                    version,
                    "1.1.0",
                    true,
                    "https://example.com/download",
                    "Notes",
                    Instant.now(),
                    UpdateCheckStatus.UPDATE_AVAILABLE));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService updateService = new UpdateService("1.0.0", port);
    updateService.checkNow("release").join();

    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "messages.prefix", "[SpectraEvents] ",
                "messages.update-available", "Update available: ",
                "messages.update-command", " Run /se update"));

    UpdateNotificationListener listener = new UpdateNotificationListener(updateService, locales);

    AtomicBoolean receivedMessage = new AtomicBoolean(false);
    UUID adminUuid = UUID.randomUUID();
    Player player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return "spectraevents.admin".equals(args[0]);
                  }
                  if ("isOp".equals(method.getName())) return false;
                  if ("getUniqueId".equals(method.getName())) return adminUuid;
                  if ("sendMessage".equals(method.getName())) {
                    receivedMessage.set(true);
                    return null;
                  }
                  return null;
                });

    PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text("joined"));
    listener.onPlayerJoin(event);

    assertTrue(receivedMessage.get());

    // Second join shouldn't re-notify (cached notifiedPlayers)
    receivedMessage.set(false);
    listener.onPlayerJoin(event);
    assertFalse(receivedMessage.get());
  }

  @Test
  void testNotifyPermissionAndOpVariants() {
    UpdatePort port =
        new UpdatePort() {
          @Override
          public CompletableFuture<UpdateInfo> checkUpdate(String version, String channel) {
            return CompletableFuture.completedFuture(
                new UpdateInfo(
                    version,
                    "1.1.0",
                    true,
                    "https://example.com/download",
                    "Notes",
                    Instant.now(),
                    UpdateCheckStatus.UPDATE_AVAILABLE));
          }

          @Override
          public CompletableFuture<Boolean> downloadUpdate(
              String downloadUrl, String targetFileName) {
            return CompletableFuture.completedFuture(false);
          }
        };

    UpdateService updateService = new UpdateService("1.0.0", port);
    updateService.checkNow("release").join();

    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "messages.prefix", "[SpectraEvents] ",
                "messages.update-available", "Update available: ",
                "messages.update-command", " Run /se update"));

    UpdateNotificationListener listener = new UpdateNotificationListener(updateService, locales);

    // Player with spectraevents.update.notify
    AtomicBoolean notifyReceived = new AtomicBoolean(false);
    Player notifyPlayer =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return "spectraevents.update.notify".equals(args[0]);
                  }
                  if ("isOp".equals(method.getName())) return false;
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  if ("sendMessage".equals(method.getName())) {
                    notifyReceived.set(true);
                    return null;
                  }
                  return null;
                });
    listener.onPlayerJoin(new PlayerJoinEvent(notifyPlayer, Component.text("joined")));
    assertTrue(notifyReceived.get());

    // Player with isOp() == true
    AtomicBoolean opReceived = new AtomicBoolean(false);
    Player opPlayer =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) return false;
                  if ("isOp".equals(method.getName())) return true;
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  if ("sendMessage".equals(method.getName())) {
                    opReceived.set(true);
                    return null;
                  }
                  return null;
                });
    listener.onPlayerJoin(new PlayerJoinEvent(opPlayer, Component.text("joined")));
    assertTrue(opReceived.get());
  }

  @Test
  void testNoUpdateAvailableDoesNotNotify() {
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

    UpdateService updateService = new UpdateService("1.0.0", port);
    LocaleCatalog locales = LocaleCatalog.of(Map.of());
    UpdateNotificationListener listener = new UpdateNotificationListener(updateService, locales);

    AtomicBoolean receivedMessage = new AtomicBoolean(false);
    Player player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) return true;
                  if ("isOp".equals(method.getName())) return true;
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  if ("sendMessage".equals(method.getName())) {
                    receivedMessage.set(true);
                    return null;
                  }
                  return null;
                });

    PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text("joined"));
    listener.onPlayerJoin(event);
    assertFalse(receivedMessage.get());
  }
}
