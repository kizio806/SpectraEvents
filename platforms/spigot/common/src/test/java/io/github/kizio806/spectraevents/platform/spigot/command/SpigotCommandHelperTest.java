package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotCommandHelperTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;
  private LocaleCatalog locales;
  private SpigotCommandHelper helper;

  @BeforeEach
  void setUp() throws IOException {
    locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    helper = new SpigotCommandHelper(locales);
  }

  @Test
  void testLocalesGetter() {
    assertEquals(locales, helper.locales());
  }

  @Test
  void testSendMessages() {
    List<String> messages = new ArrayList<>();
    CommandSender sender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("sendMessage".equals(method.getName()) && args != null && args.length > 0) {
                    messages.add(String.valueOf(args[0]));
                  }
                  return null;
                });

    helper.send(sender, "command.spigot.status");
    assertEquals(1, messages.size());

    helper.send(sender, "command.spigot.status", Map.of("running", 2));
    assertEquals(2, messages.size());
  }

  @Test
  void enforcesPermissionsCorrectly() {
    CommandSender directPermSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return "perm.test".equals(args[0]);
                  }
                  if ("isOp".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });

    CommandSender adminPermSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return "spectraevents.admin".equals(args[0]);
                  }
                  if ("isOp".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });

    CommandSender opSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return false;
                  }
                  if ("isOp".equals(method.getName())) {
                    return true;
                  }
                  return null;
                });

    CommandSender unpermittedSender =
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

    assertDoesNotThrow(() -> helper.require(directPermSender, "perm.test"));
    assertDoesNotThrow(() -> helper.require(adminPermSender, "perm.test"));
    assertDoesNotThrow(() -> helper.require(opSender, "perm.test"));
    assertThrows(IllegalStateException.class, () -> helper.require(unpermittedSender, "perm.test"));
  }

  @Test
  void enforcesArgumentCount() {
    String[] args = new String[] {"one", "two"};
    assertDoesNotThrow(() -> helper.requireArguments(args, 2, "/cmd <one> <two>"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.requireArguments(args, 3, "/cmd <one> <two> <three>"));
  }

  @Test
  void testMessageFor() {
    // 1. Failure without cause, with message
    Throwable ex1 = new RuntimeException("Direct failure");
    assertEquals("Direct failure", helper.messageFor(ex1));

    // 2. Failure with cause, with message
    Throwable ex2 = new RuntimeException("Wrapped", new IllegalStateException("Root cause"));
    assertEquals("Root cause", helper.messageFor(ex2));

    // 3. Failure with cause, without message
    Throwable ex3 = new RuntimeException(new NullPointerException());
    assertEquals("NullPointerException", helper.messageFor(ex3));
  }

  @Test
  void testEventLocation() {
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "LocatorPlayer");
    Location playerLoc = helper.eventLocation(player);
    assertNotNull(playerLoc);

    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    Location consoleLoc = helper.eventLocation(console);
    assertNotNull(consoleLoc);

    try {
      MockBukkitSetup.setCustomWorlds(List.of());
      assertThrows(IllegalStateException.class, () -> helper.eventLocation(console));
    } finally {
      MockBukkitSetup.setCustomWorlds(null);
    }
  }

  @Test
  void filtersTabCompletionsCaseInsensitivePrefix() {
    List<String> result = helper.matches("ap", "apple", "banana", "apricot", "cherry");
    assertEquals(List.of("apple", "apricot"), result);
  }
}
