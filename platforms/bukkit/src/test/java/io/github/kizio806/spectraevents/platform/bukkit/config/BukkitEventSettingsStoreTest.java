package io.github.kizio806.spectraevents.platform.bukkit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BukkitEventSettingsStoreTest {
  @TempDir Path temporaryDirectory;

  @Test
  void writesNewGlobalConfigurationWithEnglishAsTheDefaultLocale() throws Exception {
    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);

    assertEquals("en-US", store.locale());
    assertFalse(Files.readString(temporaryDirectory.resolve("config.yml")).contains("overrides:"));
  }

  @Test
  void writesOnlyTheSelectedEventOverride() throws Exception {
    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);

    store.setParameter("airdrop", "announcement-delay", "16m");

    assertEquals("16m", store.overridesFor("airdrop").get("announcement-delay"));
    assertFalse(Files.readString(temporaryDirectory.resolve("config.yml")).contains("airdrop"));
    assertTrue(
        Files.readString(temporaryDirectory.resolve("events/overrides/airdrop.yml"))
            .contains("Admin Override: airdrop"));
  }

  @Test
  void removesParameterSuccessfully() {
    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);

    store.setParameter("airdrop", "delay", "5m");
    assertEquals("5m", store.overridesFor("airdrop").get("delay"));

    store.removeParameter("airdrop", "delay");
    assertFalse(store.overridesFor("airdrop").containsKey("delay"));
  }

  @Test
  void managesNamedLocationsLifecycle() {
    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);
    assertTrue(store.locations().isEmpty());

    EventLocation location = new EventLocation("world", 100.5, 64.0, -200.5, 90.0f, 0.0f);
    store.saveLocation("arena_center", location);

    Optional<EventLocation> loaded = store.location("arena_center");
    assertTrue(loaded.isPresent());
    assertEquals("world", loaded.get().world());
    assertEquals(100.5, loaded.get().x());
    assertEquals(64.0, loaded.get().y());
    assertEquals(-200.5, loaded.get().z());
    assertEquals(90.0f, loaded.get().yaw());
    assertEquals(0.0f, loaded.get().pitch());

    Map<String, EventLocation> allLocations = store.locations();
    assertEquals(1, allLocations.size());
    assertTrue(allLocations.containsKey("arena_center"));

    assertTrue(store.removeLocation("arena_center"));
    assertFalse(store.removeLocation("arena_center"));
    assertFalse(store.removeLocation("non_existent"));
    assertTrue(store.location("arena_center").isEmpty());
    assertTrue(store.locations().isEmpty());
  }

  @Test
  void handlesOverridesWithNestedSectionOrEmptyDefinition() {
    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);

    assertTrue(store.overridesFor("unknown_event").isEmpty());

    store.setParameter("complex_event", "param1", "val1");
    store.setParameter("complex_event", "param2", "val2");
    Map<String, Object> overrides = store.overridesFor("complex_event");
    assertEquals("val1", overrides.get("param1"));
    assertEquals("val2", overrides.get("param2"));
  }

  @Test
  void validatesDefinitionIdParameterAndLocationNames() {
    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);
    EventLocation loc = new EventLocation("world", 0, 0, 0, 0, 0);

    assertThrows(IllegalArgumentException.class, () -> store.overridesFor(null));
    assertThrows(IllegalArgumentException.class, () -> store.overridesFor("invalid ID!"));
    assertThrows(IllegalArgumentException.class, () -> store.overridesFor(""));

    assertThrows(NullPointerException.class, () -> store.setParameter("event1", null, "val"));
    assertThrows(
        IllegalArgumentException.class, () -> store.setParameter("event1", "123invalid", "val"));
    assertThrows(IllegalArgumentException.class, () -> store.setParameter("event1", "param", null));
    assertThrows(
        IllegalArgumentException.class, () -> store.setParameter("event1", "param", "   "));

    assertThrows(NullPointerException.class, () -> store.saveLocation(null, loc));
    assertThrows(
        IllegalArgumentException.class, () -> store.saveLocation("invalid location!", loc));
    assertThrows(NullPointerException.class, () -> store.location(null));
    assertThrows(IllegalArgumentException.class, () -> store.location("invalid location!"));
    assertThrows(NullPointerException.class, () -> store.removeLocation(null));
    assertThrows(IllegalArgumentException.class, () -> store.removeLocation("invalid location!"));
  }

  @Test
  void respectsExistingGlobalConfiguration() throws Exception {
    Path configFile = temporaryDirectory.resolve("config.yml");
    Files.writeString(
        configFile,
        """
        schema-version: 2
        locale: pl-PL
        """);

    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);
    assertEquals("pl-PL", store.locale());
  }

  @Test
  void throwsExceptionWhenDataDirectoryCannotBePrepared() throws Exception {
    Path blockedFile = temporaryDirectory.resolve("blocked");
    Files.writeString(blockedFile, "not a directory");

    assertThrows(IllegalStateException.class, () -> new BukkitEventSettingsStore(blockedFile));
  }

  @Test
  void initializesFromJavaPluginSuccessfully() {
    io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup.ensureInitialized();
    org.bukkit.plugin.java.JavaPluginLoader loader =
        io.github.kizio806.spectraevents.platform.bukkit.compat.BukkitPlatformBridge
            .createJavaPluginLoader(org.bukkit.Bukkit.getServer());
    org.bukkit.plugin.PluginDescriptionFile description =
        new org.bukkit.plugin.PluginDescriptionFile(
            "TestPlugin", "1.0.0", "io.github.test.TestPlugin");
    org.bukkit.plugin.java.JavaPlugin plugin =
        new org.bukkit.plugin.java.JavaPlugin(
            loader, description, temporaryDirectory.toFile(), temporaryDirectory.toFile()) {};

    BukkitEventSettingsStore store = new BukkitEventSettingsStore(plugin);
    assertNotNull(store);
    assertEquals("en-US", store.locale());
  }

  @Test
  void ignoresInvalidLocationEntriesInLocationsMap() throws Exception {
    Path locFile = temporaryDirectory.resolve("locations.yml");
    String validSerialized = new EventLocation("world", 0, 64, 0, 0, 0).serialize();
    Files.writeString(
        locFile,
        "schema-version: 1\nlocations:\n  valid: \""
            + validSerialized
            + "\"\n  invalid: \"not_a_valid_location\"\n");

    BukkitEventSettingsStore store = new BukkitEventSettingsStore(temporaryDirectory);
    Map<String, EventLocation> locations = store.locations();
    assertEquals(1, locations.size());
    assertTrue(locations.containsKey("valid"));
    assertFalse(locations.containsKey("invalid"));
  }

  @Test
  void validatesNullPluginInConstructor() {
    assertThrows(
        NullPointerException.class,
        () -> new BukkitEventSettingsStore((org.bukkit.plugin.java.JavaPlugin) null));
  }
}
