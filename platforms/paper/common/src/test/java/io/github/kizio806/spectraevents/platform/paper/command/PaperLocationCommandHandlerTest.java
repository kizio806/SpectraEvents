package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.platform.bukkit.config.BukkitEventSettingsStore;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperLocationCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;
  private BukkitEventSettingsStore settingsStore;
  private LocaleCatalog locales;
  private LocationCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;

  @BeforeEach
  void setUp() {
    Path settingsFile = tempDir.resolve("settings.yml");
    settingsStore = new BukkitEventSettingsStore(settingsFile);
    locales =
        LocaleCatalog.of(
            Map.of(
                "command.location.usage", "Usage: /spectraevents location <list|set|remove>",
                "command.location.empty", "No saved locations.",
                "command.location.list-title", "Locations (%count%):",
                "command.location.list-entry", " - %name%: %world% (%x%, %y%, %z%)",
                "command.location.only-players", "Only players can set location.",
                "command.location.set-success", "Set location %name%!",
                "command.location.not-found", "Location %name% not found.",
                "command.location.removed", "Removed location %name%.",
                "command.location.name-required", "Name required: %example%"));
    handler = new LocationCommandHandler(settingsStore, locales);
    messages = new ArrayList<>();
    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
  }

  @Test
  void testUsageAndListEmpty() throws Exception {
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    CommandSourceStack source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher.execute("location", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("location list", source);
    assertFalse(messages.isEmpty());

    // Missing arguments
    messages.clear();
    dispatcher.execute("location set", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("location remove", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testSetAndRemoveLocation() throws Exception {
    World world = MockBukkitSetup.createMockWorld("world");
    Location loc = new Location(world, 100, 64, 200, 45f, 0f);
    Player player = PaperCommandTestSupport.createRecordingPlayer(messages, loc);
    CommandSourceStack playerSource = PaperCommandTestSupport.createMockSource(player, loc);

    // Set location as player
    dispatcher.execute("location set arena", playerSource);
    assertTrue(settingsStore.locations().containsKey("arena"));

    // List locations with 1 location
    messages.clear();
    dispatcher.execute("location list", playerSource);
    assertFalse(messages.isEmpty());

    // Remove location
    messages.clear();
    dispatcher.execute("location remove arena", playerSource);
    assertFalse(settingsStore.locations().containsKey("arena"));

    // Remove non-existent location
    dispatcher.execute("location remove arena", playerSource);
  }

  @Test
  void testConsoleCannotSetLocation() throws Exception {
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    CommandSourceStack source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher.execute("location set arena", source);
    assertFalse(settingsStore.locations().containsKey("arena"));
  }

  @Test
  void testSetLocationWithInvalidNameThrowsExceptionAndSendsMessage() throws Exception {
    World world = MockBukkitSetup.createMockWorld("world");
    Location loc = new Location(world, 100, 64, 200, 45f, 0f);
    Player player = PaperCommandTestSupport.createRecordingPlayer(messages, loc);
    CommandSourceStack playerSource = PaperCommandTestSupport.createMockSource(player, loc);

    messages.clear();
    dispatcher.execute(
        "location set name_that_is_far_too_long_and_exceeds_thirty_two_chars", playerSource);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testSuggestions() {
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    CommandSourceStack source = PaperCommandTestSupport.createMockSource(sender, null);

    var suggestions =
        dispatcher.getCompletionSuggestions(dispatcher.parse("location remove ", source)).join();
    org.junit.jupiter.api.Assertions.assertNotNull(suggestions);
  }
}
