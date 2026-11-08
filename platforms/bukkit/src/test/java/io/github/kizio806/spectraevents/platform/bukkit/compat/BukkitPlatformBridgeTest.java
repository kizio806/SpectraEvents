package io.github.kizio806.spectraevents.platform.bukkit.compat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;

class BukkitPlatformBridgeTest {

  @Test
  void createsPlayerJoinAndQuitEventsSafely() {
    UUID uuid = UUID.randomUUID();
    Player player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("getUniqueId".equals(method.getName())) {
                    return uuid;
                  }
                  if ("getName".equals(method.getName())) {
                    return "PlayerTest";
                  }
                  return null;
                });

    PlayerJoinEvent joinEvent = BukkitPlatformBridge.createPlayerJoinEvent(player);
    assertNotNull(joinEvent);
    org.junit.jupiter.api.Assertions.assertSame(player, joinEvent.getPlayer());

    PlayerQuitEvent quitEvent = BukkitPlatformBridge.createPlayerQuitEvent(player);
    assertNotNull(quitEvent);
    org.junit.jupiter.api.Assertions.assertSame(player, quitEvent.getPlayer());
  }

  @Test
  void dispatchesSendResourcePackGracefully() {
    AtomicReference<String> dispatchedUrl = new AtomicReference<>();
    Player player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("setResourcePack".equals(method.getName())) {
                    dispatchedUrl.set((String) args[0]);
                    return null;
                  }
                  if ("getName".equals(method.getName())) {
                    return "PlayerTest";
                  }
                  return null;
                });

    byte[] hash = new byte[] {0x12, 0x34};
    BukkitPlatformBridge.sendResourcePack(
        player, "https://example.com/pack.zip", hash, "Please accept", true, null);

    assertEquals("https://example.com/pack.zip", dispatchedUrl.get());
  }

  @Test
  void dispatchesKickPlayerGracefully() {
    AtomicReference<String> kickReason = new AtomicReference<>();
    Player player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("kickPlayer".equals(method.getName()) || "kick".equals(method.getName())) {
                    kickReason.set(String.valueOf(args[0]));
                    return null;
                  }
                  if ("getName".equals(method.getName())) {
                    return "PlayerTest";
                  }
                  return null;
                });

    BukkitPlatformBridge.kickPlayer(player, "Legacy Kick", Component.text("Adventure Kick"), null);
    assertNotNull(kickReason.get());
  }

  @Test
  void dispatchesSetCustomNameGracefully() {
    AtomicReference<String> customName = new AtomicReference<>();
    Entity entity =
        (Entity)
            Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[] {Entity.class},
                (proxy, method, args) -> {
                  if ("setCustomName".equals(method.getName())
                      || "customName".equals(method.getName())) {
                    customName.set(String.valueOf(args[0]));
                    return null;
                  }
                  if ("getUniqueId".equals(method.getName())) {
                    return UUID.randomUUID();
                  }
                  return null;
                });

    BukkitPlatformBridge.setCustomName(entity, "BossName", Component.text("AdventureBoss"), null);
    assertNotNull(customName.get());
  }

  @Test
  void dispatchesSetTextGracefully() {
    AtomicReference<String> renderedText = new AtomicReference<>();
    TextDisplay textDisplay =
        (TextDisplay)
            Proxy.newProxyInstance(
                TextDisplay.class.getClassLoader(),
                new Class<?>[] {TextDisplay.class},
                (proxy, method, args) -> {
                  if ("setText".equals(method.getName()) || "text".equals(method.getName())) {
                    renderedText.set(String.valueOf(args[0]));
                    return null;
                  }
                  if ("getUniqueId".equals(method.getName())) {
                    return UUID.randomUUID();
                  }
                  return null;
                });

    BukkitPlatformBridge.setText(
        textDisplay, "Floating Text", Component.text("Floating Adventure"), null);
    assertNotNull(renderedText.get());
  }

  @Test
  void rejectsNullRequiredParameters() {
    assertThrows(
        NullPointerException.class, () -> BukkitPlatformBridge.createPlayerJoinEvent(null));
    assertThrows(
        NullPointerException.class, () -> BukkitPlatformBridge.createPlayerQuitEvent(null));
    assertThrows(
        NullPointerException.class, () -> BukkitPlatformBridge.kickPlayer(null, "msg", null, null));
    assertThrows(
        NullPointerException.class,
        () -> BukkitPlatformBridge.setCustomName(null, "name", null, null));
    assertThrows(
        NullPointerException.class, () -> BukkitPlatformBridge.setText(null, "text", null, null));
    assertThrows(
        NullPointerException.class,
        () -> BukkitPlatformBridge.sendResourcePack(null, "url", new byte[0], "p", false, null));
    assertThrows(
        NullPointerException.class, () -> BukkitPlatformBridge.createJavaPluginLoader(null));
  }

  @Test
  void createsInventorySafely() {
    MockBukkitSetup.ensureInitialized();
    Inventory inv =
        BukkitPlatformBridge.createInventory(
            null, 27, "Legacy Inv Title", Component.text("Adventure Inv Title"));
    assertNotNull(inv);
    assertEquals(27, inv.getSize());
  }

  @Test
  void handlesJavaPluginLoaderInstantiation() {
    MockBukkitSetup.ensureInitialized();
    assertDoesNotThrow(
        () -> {
          try {
            BukkitPlatformBridge.createJavaPluginLoader(org.bukkit.Bukkit.getServer());
          } catch (LinkageError ignored) {
            // Expected if JavaPluginLoader constructor signature differs or is deprecated in
            // runtime mock
          }
        });
  }

  @Test
  void handlesFallbacksAndErrorsInPlatformBridge() {
    assertDoesNotThrow(
        () -> {
          Entity failingEntity =
              (Entity)
                  Proxy.newProxyInstance(
                      Entity.class.getClassLoader(),
                      new Class<?>[] {Entity.class},
                      (proxy, method, args) -> {
                        if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                        throw new RuntimeException("Simulated entity error");
                      });
          BukkitPlatformBridge.setCustomName(failingEntity, "name", null, null);

          Player failingPlayer =
              (Player)
                  Proxy.newProxyInstance(
                      Player.class.getClassLoader(),
                      new Class<?>[] {Player.class},
                      (proxy, method, args) -> {
                        if ("getName".equals(method.getName())) return "FailingPlayer";
                        throw new RuntimeException("Simulated player error");
                      });
          BukkitPlatformBridge.kickPlayer(failingPlayer, "kick", null, null);
          BukkitPlatformBridge.sendResourcePack(
              failingPlayer, "https://url.com", new byte[0], "prompt", false, null);

          TextDisplay failingDisplay =
              (TextDisplay)
                  Proxy.newProxyInstance(
                      TextDisplay.class.getClassLoader(),
                      new Class<?>[] {TextDisplay.class},
                      (proxy, method, args) -> {
                        if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                        throw new RuntimeException("Simulated text display error");
                      });
          BukkitPlatformBridge.setText(failingDisplay, "text", null, null);
        });
  }

  @Test
  void dispatchesSendResourcePackLegacyFallback() {
    AtomicReference<String> dispatchedUrl = new AtomicReference<>();
    Player player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("setResourcePack".equals(method.getName())) {
                    if (args.length == 4) {
                      throw new UnsupportedOperationException("Modern not supported");
                    }
                    dispatchedUrl.set((String) args[0]);
                    return null;
                  }
                  if ("getName".equals(method.getName())) {
                    return "PlayerTest";
                  }
                  return null;
                });

    byte[] hash = new byte[] {0x12, 0x34};
    BukkitPlatformBridge.sendResourcePack(
        player, "https://example.com/fallback.zip", hash, "Please accept", true, null);

    assertEquals("https://example.com/fallback.zip", dispatchedUrl.get());
  }

  @Test
  void createsInventoryWithLegacyTitleOnly() {
    MockBukkitSetup.ensureInitialized();
    Inventory inv = BukkitPlatformBridge.createInventory(null, 9, "Legacy Only Title", null);
    assertNotNull(inv);
    assertEquals(9, inv.getSize());

    Inventory invNull = BukkitPlatformBridge.createInventory(null, 9, null, null);
    assertNotNull(invNull);
  }

  @Test
  void kicksPlayerWithLegacyMessageOrNull() {
    AtomicReference<String> kickMsg = new AtomicReference<>();
    Player player =
        (Player)
            Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                  if ("kickPlayer".equals(method.getName())) {
                    kickMsg.set((String) args[0]);
                    return null;
                  }
                  if ("getName".equals(method.getName())) return "PlayerTest";
                  return null;
                });

    BukkitPlatformBridge.kickPlayer(player, null, null, null);
    assertEquals("", kickMsg.get());

    BukkitPlatformBridge.kickPlayer(player, "Manual Kick", null, null);
    assertEquals("Manual Kick", kickMsg.get());
  }

  @Test
  void setsCustomNameWithLegacyNameOrNull() {
    AtomicReference<String> name = new AtomicReference<>();
    Entity entity =
        (Entity)
            Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                new Class<?>[] {Entity.class},
                (proxy, method, args) -> {
                  if ("setCustomName".equals(method.getName())) {
                    name.set((String) args[0]);
                    return null;
                  }
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  return null;
                });

    BukkitPlatformBridge.setCustomName(entity, null, null, null);
    assertEquals("", name.get());

    BukkitPlatformBridge.setCustomName(entity, "Custom Entity", null, null);
    assertEquals("Custom Entity", name.get());
  }

  @Test
  void setsTextOnTextDisplayWithLegacyTextOrNull() {
    AtomicReference<String> txt = new AtomicReference<>();
    TextDisplay display =
        (TextDisplay)
            Proxy.newProxyInstance(
                TextDisplay.class.getClassLoader(),
                new Class<?>[] {TextDisplay.class},
                (proxy, method, args) -> {
                  if ("setText".equals(method.getName())) {
                    txt.set((String) args[0]);
                    return null;
                  }
                  if ("getUniqueId".equals(method.getName())) return UUID.randomUUID();
                  return null;
                });

    BukkitPlatformBridge.setText(display, null, null, null);
    assertEquals("", txt.get());

    BukkitPlatformBridge.setText(display, "§aGreen Text", null, null);
    assertEquals("§aGreen Text", txt.get());
  }

  @Test
  void privateConstructorCanBeInvoked() throws Exception {
    java.lang.reflect.Constructor<BukkitPlatformBridge> ctor =
        BukkitPlatformBridge.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    assertNotNull(ctor.newInstance());
  }
}
