package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotLocationCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;

  private BukkitEventSettingsStore settingsStore;
  private SpigotCommandHelper helper;
  private SpigotLocationCommandHandler handler;

  @BeforeEach
  void setUp() {
    settingsStore = new BukkitEventSettingsStore(tempDir.resolve("settings.yml"));
    LocaleCatalog locales =
        LocaleCatalog.of(
            Map.of(
                "command.location.usage", "Usage: /spectraevents location <list|set|remove>",
                "command.location.list-title", "Locations (%count%):",
                "command.location.list-entry", "%name% at %world%, %x%, %y%, %z%",
                "command.location.name-required", "Name required: %example%",
                "command.location.saved", "Saved location %name%",
                "command.location.removed", "Removed location %name%",
                "command.location.not-found", "Location %name% not found",
                "command.spigot.player-only", "Players only"));
    helper = new SpigotCommandHelper(locales);
    handler = new SpigotLocationCommandHandler(settingsStore, helper);
  }

  @Test
  void rejectsNullConstructorParameters() {
    assertThrows(NullPointerException.class, () -> new SpigotLocationCommandHandler(null, helper));
    assertThrows(
        NullPointerException.class, () -> new SpigotLocationCommandHandler(settingsStore, null));
  }

  @Test
  void shouldShowUsageOnEmptyOrHelp() {
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    assertDoesNotThrow(
        () -> {
          handler.execute(console, new String[0]);
          handler.execute(console, new String[] {"help"});
          handler.execute(console, new String[] {"unknown_subcommand"});
        });
  }

  @Test
  void shouldListLocations() {
    settingsStore.saveLocation("arena", new EventLocation("world", 10.0, 64.0, 20.0, 0.0f, 0.0f));

    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    handler.execute(console, new String[] {"list"});

    assertEquals(1, settingsStore.locations().size());
  }

  @Test
  void shouldSetLocationFromPlayer() {
    Player player = MockBukkitSetup.createMockPlayer(UUID.randomUUID(), "AdminPlayer");

    // Missing location name
    handler.execute(player, new String[] {"set"});

    // Valid location set
    handler.execute(player, new String[] {"set", "spawn_point"});
    assertTrue(settingsStore.locations().containsKey("spawn_point"));

    // Console cannot set location
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    handler.execute(console, new String[] {"set", "spawn_point"});
  }

  @Test
  void shouldRemoveLocation() {
    settingsStore.saveLocation("temp", new EventLocation("world", 0.0, 64.0, 0.0, 0.0f, 0.0f));

    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    // Missing name
    handler.execute(console, new String[] {"remove"});

    // Remove existing
    handler.execute(console, new String[] {"remove", "temp"});
    assertFalse(settingsStore.locations().containsKey("temp"));

    // Remove non-existing
    handler.execute(console, new String[] {"remove", "temp"});
  }

  @Test
  void shouldTabCompleteCorrectly() {
    ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
    settingsStore.saveLocation("loc1", new EventLocation("world", 0, 0, 0, 0, 0));

    List<String> subcommands = handler.tabComplete(console, new String[] {""});
    assertTrue(subcommands.contains("list"));
    assertTrue(subcommands.contains("set"));
    assertTrue(subcommands.contains("remove"));

    List<String> names = handler.tabComplete(console, new String[] {"remove", ""});
    assertTrue(names.contains("loc1"));

    List<String> nonRemoveTwoArgs = handler.tabComplete(console, new String[] {"set", ""});
    assertTrue(nonRemoveTwoArgs.isEmpty());

    List<String> threeArgs = handler.tabComplete(console, new String[] {"remove", "loc1", "extra"});
    assertTrue(threeArgs.isEmpty());
  }

  @Test
  void enforcesPermission() {
    org.bukkit.command.CommandSender unprivilegedSender =
        (org.bukkit.command.CommandSender)
            java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.command.CommandSender.class.getClassLoader(),
                new Class<?>[] {org.bukkit.command.CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return false;
                  }
                  if ("isOp".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });

    assertThrows(
        IllegalStateException.class,
        () -> handler.execute(unprivilegedSender, new String[] {"list"}));
  }
}
