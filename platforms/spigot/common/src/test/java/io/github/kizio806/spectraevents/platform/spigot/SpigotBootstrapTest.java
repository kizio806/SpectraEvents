package io.github.kizio806.spectraevents.platform.spigot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.compat.BukkitPlatformBridge;
import io.github.kizio806.spectraevents.platform.bukkit.loot.BukkitSharedLootListener;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import io.github.kizio806.spectraevents.platform.spigot.lifecycle.SpigotEntityReconciler;
import io.github.kizio806.spectraevents.platform.spigot.render.SpigotModelRenderer;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotBootstrapTest {

  @TempDir Path tempDir;

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSetup.resetCustomDelegates();
  }

  private static void setField(Object target, String fieldName, Object value)
      throws ReflectiveOperationException {
    Field field = SpigotBootstrap.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private SpectraEventsSpigotPlugin createPlugin(Path dataDir, BukkitAudiences mockAudiences)
      throws Exception {
    JavaPluginLoader pluginLoader = BukkitPlatformBridge.createJavaPluginLoader(Bukkit.getServer());
    String pluginYml =
        """
        name: SpectraEvents
        version: 1.0.0
        main: io.github.kizio806.spectraevents.platform.spigot.SpectraEventsSpigotPlugin
        commands:
          spectraevents:
            description: SpectraEvents command
        """;
    PluginDescriptionFile description =
        new PluginDescriptionFile(
            new java.io.ByteArrayInputStream(pluginYml.getBytes(StandardCharsets.UTF_8)));
    File dataFolder = dataDir.toFile();
    File file = tempDir.resolve("plugin.jar").toFile();

    return new SpectraEventsSpigotPlugin(
        pluginLoader, description, dataFolder, file, p -> mockAudiences, SpigotBootstrap::new);
  }

  private BukkitAudiences createMockAudiences() {
    return (BukkitAudiences)
        Proxy.newProxyInstance(
            BukkitAudiences.class.getClassLoader(),
            new Class<?>[] {BukkitAudiences.class},
            (p, m, a) -> null);
  }

  @Test
  void testBootstrapEnableAndDisable() throws Exception {
    Path dataDir = tempDir.resolve("data");
    Path templatesDir = dataDir.resolve("templates");
    Files.createDirectories(templatesDir);
    Path templateZip = templatesDir.resolve("metin.spectra.zip");
    Files.writeString(templateZip, "mock-zip-content");

    try (BukkitAudiences mockAudiences = createMockAudiences()) {
      SpectraEventsSpigotPlugin plugin = createPlugin(dataDir, mockAudiences);
      MockBukkitSetup.createMockCommand("spectraevents", plugin);

      SpigotBootstrap bootstrap = new SpigotBootstrap(plugin, mockAudiences);

      assertDoesNotThrow(bootstrap::onDisable);
      assertDoesNotThrow(bootstrap::onEnable);
      assertDoesNotThrow(bootstrap::onDisable);
    }
  }

  @Test
  void testBootstrapEnableFailsWhenDataDirectoryPreparationFails() throws Exception {
    Path dataDir = tempDir.resolve("data_file");
    Files.writeString(dataDir, "not a directory");

    try (BukkitAudiences mockAudiences = createMockAudiences()) {
      SpectraEventsSpigotPlugin plugin = createPlugin(dataDir, mockAudiences);
      SpigotBootstrap bootstrap = new SpigotBootstrap(plugin, mockAudiences);

      IllegalStateException ex = assertThrows(IllegalStateException.class, bootstrap::onEnable);
      assertTrue(ex.getMessage().contains("Could not prepare the SpectraEvents data directory"));
    }
  }

  @Test
  void testBootstrapEnableFailsWhenLocaleCatalogLoadFails() throws Exception {
    Path dataDir = tempDir.resolve("data_broken_locale");
    Files.createDirectories(dataDir);
    Path configFile = dataDir.resolve("config.yml");
    Files.createDirectories(configFile);

    try (BukkitAudiences mockAudiences = createMockAudiences()) {
      SpectraEventsSpigotPlugin plugin = createPlugin(dataDir, mockAudiences);
      SpigotBootstrap bootstrap = new SpigotBootstrap(plugin, mockAudiences);

      IllegalStateException ex = assertThrows(IllegalStateException.class, bootstrap::onEnable);
      assertTrue(ex.getMessage().contains("Could not prepare the SpectraEvents locale catalog"));
    }
  }

  @Test
  void testBootstrapEnableFailsWhenCommandMissing() throws Exception {
    Path dataDir = tempDir.resolve("data_no_cmd");
    Files.createDirectories(dataDir.resolve("templates"));
    Files.writeString(dataDir.resolve("templates").resolve("metin.spectra.zip"), "mock-zip");

    try (BukkitAudiences mockAudiences = createMockAudiences()) {
      SpectraEventsSpigotPlugin plugin = createPlugin(dataDir, mockAudiences);
      SpigotBootstrap bootstrap = new SpigotBootstrap(plugin, mockAudiences);

      IllegalStateException ex = assertThrows(IllegalStateException.class, bootstrap::onEnable);
      assertTrue(
          ex.getMessage().contains("plugin.yml does not declare the /spectraevents command"));
    }
  }

  @Test
  void testBootstrapEnableFailsWhenDefinitionLoadFails() throws Exception {
    Path dataDir = tempDir.resolve("data_broken_events");
    Files.createDirectories(dataDir.resolve("templates"));
    Files.writeString(dataDir.resolve("templates").resolve("metin.spectra.zip"), "mock-zip");
    Files.createDirectories(dataDir.resolve("events"));
    Files.writeString(dataDir.resolve("events").resolve("presets"), "presets is a file");

    try (BukkitAudiences mockAudiences = createMockAudiences()) {
      SpectraEventsSpigotPlugin plugin = createPlugin(dataDir, mockAudiences);
      MockBukkitSetup.createMockCommand("spectraevents", plugin);
      SpigotBootstrap bootstrap = new SpigotBootstrap(plugin, mockAudiences);

      IllegalStateException ex = assertThrows(IllegalStateException.class, bootstrap::onEnable);
      assertTrue(ex.getMessage().contains("Failed to load event definitions"));
    }
  }

  @Test
  void testBootstrapEnableWithLoadedDefinitionsAndResourcePackDelivery() throws Exception {
    Path dataDir = tempDir.resolve("data_full");
    Files.createDirectories(dataDir.resolve("events"));

    try (InputStream in = getClass().getClassLoader().getResourceAsStream("events/metin.yml")) {
      if (in != null) {
        Files.copy(in, dataDir.resolve("events").resolve("metin.yml"));
      } else {
        String minimalEvent =
            """
            id: metin
            name: "Metin Stone"
            phases:
              - id: spawn
                transitions: []
            """;
        Files.writeString(dataDir.resolve("events").resolve("metin.yml"), minimalEvent);
      }
    }

    String resourcePackYml =
        """
        enabled: true
        required: false
        url: "https://example.com/pack.zip"
        sha1: "0123456789abcdef0123456789abcdef01234567"
        """;
    Files.writeString(dataDir.resolve("resource-pack.yml"), resourcePackYml);

    MockBukkitSetup.setCustomBukkitVersion("26.1.0");

    try (BukkitAudiences mockAudiences = createMockAudiences()) {
      SpectraEventsSpigotPlugin plugin = createPlugin(dataDir, mockAudiences);
      MockBukkitSetup.createMockCommand("spectraevents", plugin);

      SpigotBootstrap bootstrap = new SpigotBootstrap(plugin, mockAudiences);

      assertDoesNotThrow(bootstrap::onEnable);

      Listener sharedLootListener = null;
      for (Listener registered : MockBukkitSetup.getRegisteredListeners()) {
        if (registered instanceof BukkitSharedLootListener bsl) {
          sharedLootListener = bsl;
          break;
        }
      }
      if (sharedLootListener != null) {
        Field runnerField = BukkitSharedLootListener.class.getDeclaredField("playerTaskRunner");
        runnerField.setAccessible(true);
        Object runnerObj = runnerField.get(sharedLootListener);
        assertNotNull(runnerObj);

        Player mockPlayer =
            (Player)
                Proxy.newProxyInstance(
                    Player.class.getClassLoader(),
                    new Class<?>[] {Player.class},
                    (p, m, a) -> null);
        AtomicBoolean taskExecuted = new AtomicBoolean(false);
        Method acceptMethod = BiConsumer.class.getMethod("accept", Object.class, Object.class);
        acceptMethod.invoke(runnerObj, mockPlayer, (Runnable) () -> taskExecuted.set(true));
      }

      assertDoesNotThrow(bootstrap::onDisable);
    }
  }

  @Test
  void testOnDisableFallbackWhenApplicationIsNullAndRepositoryNotNull() throws Exception {
    Path dataDir = tempDir.resolve("data_repo_only");
    Files.createDirectories(dataDir);
    try (BukkitAudiences mockAudiences = createMockAudiences()) {
      SpectraEventsSpigotPlugin plugin = createPlugin(dataDir, mockAudiences);
      SpigotBootstrap bootstrap = new SpigotBootstrap(plugin, mockAudiences);

      SQLiteEventInstanceRepository repo =
          new SQLiteEventInstanceRepository(dataDir.resolve("test.db"));
      setField(bootstrap, "repository", repo);

      assertDoesNotThrow(bootstrap::onDisable);
    }
  }

  @Test
  void testOnDisableHandlesExceptionsGracefully() throws Exception {
    Path dataDir = tempDir.resolve("data_errors");
    io.github.kizio806.spectraevents.application.config.DataDirectoryLayout.prepare(dataDir);
    try (BukkitAudiences mockAudiences = createMockAudiences()) {
      SpectraEventsSpigotPlugin plugin = createPlugin(dataDir, mockAudiences);
      SpigotBootstrap bootstrap = new SpigotBootstrap(plugin, mockAudiences);

      LocaleCatalog locales =
          new FileSystemLocaleLoader(dataDir).loadConfiguredCatalog(dataDir.resolve("config.yml"));
      SpigotActionAdapter actionAdapter = new SpigotActionAdapter(plugin, mockAudiences, locales);

      Field ownedField = SpigotActionAdapter.class.getDeclaredField("ownedEntities");
      ownedField.setAccessible(true);
      Object ownedMapObj = ownedField.get(actionAdapter);
      if (ownedMapObj instanceof Map<?, ?> rawMap) {
        Method putMethod = Map.class.getMethod("put", Object.class, Object.class);
        putMethod.invoke(
            rawMap, new EventInstanceId(UUID.randomUUID()), List.of(UUID.randomUUID()));
      }

      World throwingWorld =
          (World)
              Proxy.newProxyInstance(
                  World.class.getClassLoader(),
                  new Class<?>[] {World.class},
                  (proxy, method, args) -> {
                    if ("getEntities".equals(method.getName())) {
                      throw new RuntimeException("Simulated world entity iteration failure");
                    }
                    return null;
                  });
      MockBukkitSetup.setCustomWorlds(List.of(throwingWorld));

      setField(bootstrap, "actionAdapter", actionAdapter);

      LifecycleReporter throwingReporter =
          new LifecycleReporter() {
            @Override
            public void started() {}

            @Override
            public void stopped() {
              throw new RuntimeException("Simulated application stop failure");
            }
          };

      SQLiteEventInstanceRepository repo =
          new SQLiteEventInstanceRepository(dataDir.resolve("test_app.db"));
      SpectraEventsApplication application =
          new SpectraEventsApplication(
              throwingReporter,
              new SpigotEventTaskScheduler(plugin),
              actionAdapter,
              repo,
              new SpigotEntityReconciler(plugin, actionAdapter),
              new SpigotCapabilityQuery(),
              new SpigotModelRenderer(plugin));

      setField(bootstrap, "application", application);

      BukkitScheduler throwingScheduler =
          (BukkitScheduler)
              Proxy.newProxyInstance(
                  BukkitScheduler.class.getClassLoader(),
                  new Class<?>[] {BukkitScheduler.class},
                  (proxy, method, args) -> {
                    if ("cancelTasks".equals(method.getName())) {
                      throw new RuntimeException("Scheduler cancel tasks failed");
                    }
                    return null;
                  });
      MockBukkitSetup.setCustomScheduler(throwingScheduler);

      assertDoesNotThrow(bootstrap::onDisable);
    }
  }
}
