package io.github.kizio806.spectraevents.platform.spigot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.platform.bukkit.lifecycle.BukkitBootstrapLifecycle;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpectraEventsSpigotPluginTest {

  @TempDir Path tempDir;

  private static final AtomicBoolean DISABLED_PLUGIN_CALLED = new AtomicBoolean(false);
  private static final AtomicBoolean CANCELLED_TASKS_CALLED = new AtomicBoolean(false);

  private PluginDescriptionFile description;

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @BeforeEach
  void setUp() {
    DISABLED_PLUGIN_CALLED.set(false);
    CANCELLED_TASKS_CALLED.set(false);

    PluginManager mockPluginManager =
        (PluginManager)
            Proxy.newProxyInstance(
                PluginManager.class.getClassLoader(),
                new Class<?>[] {PluginManager.class},
                (proxy, method, args) -> {
                  if ("disablePlugin".equals(method.getName())) {
                    DISABLED_PLUGIN_CALLED.set(true);
                  }
                  return null;
                });

    BukkitScheduler mockScheduler =
        (BukkitScheduler)
            Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(),
                new Class<?>[] {BukkitScheduler.class},
                (proxy, method, args) -> {
                  if ("cancelTasks".equals(method.getName())) {
                    CANCELLED_TASKS_CALLED.set(true);
                  }
                  return null;
                });

    MockBukkitSetup.setCustomPluginManager(mockPluginManager);
    MockBukkitSetup.setCustomScheduler(mockScheduler);

    description =
        new PluginDescriptionFile(
            "SpectraEvents", "0.1.0", SpectraEventsSpigotPlugin.class.getName());
  }

  @AfterEach
  void tearDown() {
    MockBukkitSetup.resetCustomDelegates();
  }

  private static void setField(Class<?> clazz, Object target, String name, Object value)
      throws ReflectiveOperationException {
    Field f = clazz.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private SpectraEventsSpigotPlugin createPlugin(
      java.util.function.Function<org.bukkit.plugin.Plugin, BukkitAudiences> audienceFactory,
      java.util.function.BiFunction<
              org.bukkit.plugin.java.JavaPlugin, BukkitAudiences, BukkitBootstrapLifecycle>
          bootstrapFactory) {
    try {
      Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
      Field f = unsafeClass.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      Object unsafe = f.get(null);
      Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
      SpectraEventsSpigotPlugin plugin =
          (SpectraEventsSpigotPlugin)
              allocateInstance.invoke(unsafe, SpectraEventsSpigotPlugin.class);

      File dataFolder = tempDir.resolve("data").toFile();
      File file = tempDir.resolve("test.jar").toFile();
      setField(JavaPlugin.class, plugin, "server", Bukkit.getServer());
      setField(JavaPlugin.class, plugin, "dataFolder", dataFolder);
      setField(JavaPlugin.class, plugin, "file", file);
      setField(JavaPlugin.class, plugin, "description", description);
      setField(
          JavaPlugin.class,
          plugin,
          "classLoader",
          SpectraEventsSpigotPlugin.class.getClassLoader());
      setField(JavaPlugin.class, plugin, "logger", new org.bukkit.plugin.PluginLogger(plugin));
      setField(SpectraEventsSpigotPlugin.class, plugin, "audienceFactory", audienceFactory);
      setField(SpectraEventsSpigotPlugin.class, plugin, "bootstrapFactory", bootstrapFactory);
      return plugin;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private BukkitAudiences createMockAudience(AtomicBoolean closedFlag) {
    return (BukkitAudiences)
        Proxy.newProxyInstance(
            BukkitAudiences.class.getClassLoader(),
            new Class<?>[] {BukkitAudiences.class},
            (proxy, method, args) -> {
              if ("close".equals(method.getName())) {
                closedFlag.set(true);
              }
              return null;
            });
  }

  private static final class TestBootstrapLifecycle implements BukkitBootstrapLifecycle {
    private final Runnable enableAction;
    private final Runnable disableAction;
    final AtomicBoolean enabledCalled = new AtomicBoolean(false);
    final AtomicBoolean disabledCalled = new AtomicBoolean(false);

    TestBootstrapLifecycle(Runnable enableAction, Runnable disableAction) {
      this.enableAction = enableAction;
      this.disableAction = disableAction;
    }

    @Override
    public void onEnable() {
      enabledCalled.set(true);
      if (enableAction != null) {
        enableAction.run();
      }
    }

    @Override
    public void onDisable() {
      disabledCalled.set(true);
      if (disableAction != null) {
        disableAction.run();
      }
    }
  }

  @Test
  void testSuccessfulLifecycleFlow() throws Exception {
    AtomicBoolean audienceClosed = new AtomicBoolean(false);
    try (BukkitAudiences mockAudience = createMockAudience(audienceClosed)) {
      TestBootstrapLifecycle lifecycle = new TestBootstrapLifecycle(null, null);

      SpectraEventsSpigotPlugin plugin = createPlugin(p -> mockAudience, (p, aud) -> lifecycle);

      plugin.onEnable();
      assertTrue(lifecycle.enabledCalled.get());
      assertFalse(DISABLED_PLUGIN_CALLED.get());

      plugin.onDisable();
      assertTrue(lifecycle.disabledCalled.get());
      assertTrue(audienceClosed.get());
      assertNull(plugin.adventure());
      assertNull(plugin.bootstrap());
    }
  }

  @Test
  void testStartupFailureDuringAudienceCreation() {
    SpectraEventsSpigotPlugin plugin =
        createPlugin(
            p -> {
              throw new RuntimeException("Audience creation failed");
            },
            (p, aud) -> null);

    plugin.onEnable();

    assertTrue(DISABLED_PLUGIN_CALLED.get(), "Plugin should be disabled on startup failure");
    assertNull(plugin.adventure());
    assertNull(plugin.bootstrap());
  }

  @Test
  void testStartupFailureDuringBootstrapCreation() throws Exception {
    AtomicBoolean audienceClosed = new AtomicBoolean(false);
    try (BukkitAudiences mockAudience = createMockAudience(audienceClosed)) {
      SpectraEventsSpigotPlugin plugin =
          createPlugin(
              p -> mockAudience,
              (p, aud) -> {
                throw new IllegalStateException("Bootstrap construction failed");
              });

      plugin.onEnable();

      assertTrue(
          DISABLED_PLUGIN_CALLED.get(), "Plugin should be disabled on bootstrap init failure");
      assertTrue(
          audienceClosed.get(), "Audience should be closed following bootstrap init failure");
      assertNull(plugin.adventure());
      assertNull(plugin.bootstrap());
    }
  }

  @Test
  void testStartupFailureDuringBootstrapOnEnable() throws Exception {
    AtomicBoolean audienceClosed = new AtomicBoolean(false);
    try (BukkitAudiences mockAudience = createMockAudience(audienceClosed)) {
      TestBootstrapLifecycle failingLifecycle =
          new TestBootstrapLifecycle(
              () -> {
                throw new RuntimeException("Validation failed in onEnable");
              },
              null);

      SpectraEventsSpigotPlugin plugin =
          createPlugin(p -> mockAudience, (p, aud) -> failingLifecycle);

      plugin.onEnable();

      assertTrue(DISABLED_PLUGIN_CALLED.get(), "Plugin should be disabled on onEnable failure");
      assertTrue(
          failingLifecycle.disabledCalled.get(), "onDisable should be called following failure");
      assertTrue(audienceClosed.get(), "Audience should be closed following onEnable failure");
      assertNull(plugin.adventure());
      assertNull(plugin.bootstrap());
    }
  }

  @Test
  void testTeardownExceptionDoesNotPreventAudienceClose() throws Exception {
    AtomicBoolean audienceClosed = new AtomicBoolean(false);
    try (BukkitAudiences mockAudience = createMockAudience(audienceClosed)) {
      TestBootstrapLifecycle throwingTeardownLifecycle =
          new TestBootstrapLifecycle(
              () -> {
                throw new RuntimeException("Initial startup failure");
              },
              () -> {
                throw new RuntimeException("Teardown error");
              });

      SpectraEventsSpigotPlugin plugin =
          createPlugin(p -> mockAudience, (p, aud) -> throwingTeardownLifecycle);

      plugin.onEnable();

      assertTrue(DISABLED_PLUGIN_CALLED.get(), "Plugin should be disabled");
      assertTrue(audienceClosed.get(), "Audience must be closed even if bootstrap onDisable threw");
      assertNull(plugin.adventure());
      assertNull(plugin.bootstrap());
    }
  }

  @Test
  void testDefaultConstructorThrowsOutsideBukkitLoader() {
    assertThrows(IllegalStateException.class, SpectraEventsSpigotPlugin::new);
  }

  @Test
  void testTwoArgConstructorThrowsOutsideBukkitLoader() {
    assertThrows(
        IllegalStateException.class,
        () -> new SpectraEventsSpigotPlugin(p -> null, (p, a) -> null));
  }

  @Test
  void testConstructorsNullValidation() {
    assertThrows(
        IllegalStateException.class, () -> new SpectraEventsSpigotPlugin(null, (p, a) -> null));
    assertThrows(IllegalStateException.class, () -> new SpectraEventsSpigotPlugin(p -> null, null));
  }

  @Test
  void testDisablePluginThrowsExceptionDuringStartupFailure() {
    PluginManager throwingPluginManager =
        (PluginManager)
            Proxy.newProxyInstance(
                PluginManager.class.getClassLoader(),
                new Class<?>[] {PluginManager.class},
                (proxy, method, args) -> {
                  if ("disablePlugin".equals(method.getName())) {
                    throw new RuntimeException("Forced disable error");
                  }
                  return null;
                });
    MockBukkitSetup.setCustomPluginManager(throwingPluginManager);
    SpectraEventsSpigotPlugin plugin =
        createPlugin(
            p -> {
              throw new RuntimeException("Startup error");
            },
            (p, a) -> null);
    assertDoesNotThrow(plugin::onEnable);
  }

  @Test
  void testCleanupWhenAdventureThrowsException() throws Exception {
    AtomicBoolean throwOnClose = new AtomicBoolean(true);
    try (BukkitAudiences throwingAudience =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> {
                  if ("close".equals(method.getName()) && throwOnClose.getAndSet(false)) {
                    throw new RuntimeException("Audience close failed");
                  }
                  return null;
                })) {
      TestBootstrapLifecycle lifecycle = new TestBootstrapLifecycle(null, null);
      SpectraEventsSpigotPlugin plugin = createPlugin(p -> throwingAudience, (p, a) -> lifecycle);
      plugin.onEnable();
      assertDoesNotThrow(plugin::onDisable);
      assertNull(plugin.adventure());
    }
  }

  @Test
  void testCleanupWhenSchedulerThrowsException() throws Exception {
    BukkitScheduler throwingScheduler =
        (BukkitScheduler)
            Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(),
                new Class<?>[] {BukkitScheduler.class},
                (proxy, method, args) -> {
                  if ("cancelTasks".equals(method.getName())) {
                    throw new RuntimeException("cancelTasks failed");
                  }
                  return null;
                });
    MockBukkitSetup.setCustomScheduler(throwingScheduler);
    AtomicBoolean closed = new AtomicBoolean(false);
    try (BukkitAudiences mockAudience = createMockAudience(closed)) {
      TestBootstrapLifecycle lifecycle = new TestBootstrapLifecycle(null, null);
      SpectraEventsSpigotPlugin plugin = createPlugin(p -> mockAudience, (p, a) -> lifecycle);
      plugin.onEnable();
      assertDoesNotThrow(plugin::onDisable);
    }
  }

  @Test
  void testPluginConstructors() {
    assertThrows(IllegalStateException.class, () -> new SpectraEventsSpigotPlugin());
    assertThrows(
        IllegalStateException.class,
        () -> new SpectraEventsSpigotPlugin(p -> null, (p, a) -> null));
  }

  @Test
  void testGettersWhenEnabled() throws Exception {
    AtomicBoolean closed = new AtomicBoolean(false);
    try (BukkitAudiences mockAudience = createMockAudience(closed)) {
      TestBootstrapLifecycle lifecycle = new TestBootstrapLifecycle(null, null);
      SpectraEventsSpigotPlugin plugin = createPlugin(p -> mockAudience, (p, a) -> lifecycle);
      plugin.onEnable();
      assertSame(mockAudience, plugin.adventure());
      assertSame(lifecycle, plugin.bootstrap());
      plugin.onDisable();
    }
  }
}
