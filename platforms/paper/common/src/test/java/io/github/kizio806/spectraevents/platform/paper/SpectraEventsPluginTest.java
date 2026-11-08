package io.github.kizio806.spectraevents.platform.paper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.platform.bukkit.lifecycle.BukkitBootstrapLifecycle;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.papermc.paper.ServerBuildInfo;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpectraEventsPluginTest {

  @TempDir Path tempDir;

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  private SpectraEventsPlugin createPluginInstance(
      Function<JavaPlugin, BukkitBootstrapLifecycle> factory) throws Exception {
    return createPluginInstance(factory, Bukkit.getServer());
  }

  private SpectraEventsPlugin createPluginInstance(
      Function<JavaPlugin, BukkitBootstrapLifecycle> factory, Server server) throws Exception {
    Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
    Field f = unsafeClass.getDeclaredField("theUnsafe");
    f.setAccessible(true);
    Object unsafe = f.get(null);
    Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
    SpectraEventsPlugin plugin =
        (SpectraEventsPlugin) allocateInstance.invoke(unsafe, SpectraEventsPlugin.class);

    setField(JavaPlugin.class, plugin, "server", server);
    setField(JavaPlugin.class, plugin, "dataFolder", tempDir.resolve("data").toFile());
    setField(JavaPlugin.class, plugin, "file", tempDir.resolve("plugin.jar").toFile());
    setField(JavaPlugin.class, plugin, "logger", Logger.getLogger("SpectraEvents"));
    setField(SpectraEventsPlugin.class, plugin, "bootstrapFactory", factory);

    return plugin;
  }

  private static void setField(Class<?> clazz, Object target, String name, Object value)
      throws ReflectiveOperationException {
    Field f = clazz.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static void withServerBuildInfo(ServerBuildInfo info, ThrowingRunnable action)
      throws Exception {
    Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
    Field f = unsafeClass.getDeclaredField("theUnsafe");
    f.setAccessible(true);
    Object unsafe = f.get(null);
    Method staticFieldOffset = unsafeClass.getMethod("staticFieldOffset", Field.class);
    Method staticFieldBase = unsafeClass.getMethod("staticFieldBase", Field.class);
    Method getObject = unsafeClass.getMethod("getObject", Object.class, long.class);
    Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);

    Class<?> holder = Class.forName("io.papermc.paper.ServerBuildInfo$1Holder");
    Field field = holder.getDeclaredField("INSTANCE");
    long offset = ((Long) staticFieldOffset.invoke(unsafe, field)).longValue();
    Object base = staticFieldBase.invoke(unsafe, field);
    Object previous = getObject.invoke(unsafe, base, offset);
    try {
      putObject.invoke(unsafe, base, offset, Optional.ofNullable(info));
      action.run();
    } finally {
      putObject.invoke(unsafe, base, offset, previous);
    }
  }

  @FunctionalInterface
  interface ThrowingRunnable {
    void run() throws Exception;
  }

  private static final class MockConfiguredClassLoader extends ClassLoader
      implements io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader {
    MockConfiguredClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    public void init(JavaPlugin plugin) {}

    @Override
    public JavaPlugin getPlugin() {
      return null;
    }

    @Override
    public io.papermc.paper.plugin.configuration.PluginMeta getConfiguration() {
      return null;
    }

    @Override
    public Class<?> loadClass(
        String name, boolean checkGlobal, boolean resolve, boolean checkLibraries)
        throws ClassNotFoundException {
      return loadClass(name, resolve);
    }

    @Override
    public io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup getGroup() {
      return null;
    }

    @Override
    public void close() {}

    Class<?> definePluginClass(String name, byte[] bytes) {
      return defineClass(name, bytes, 0, bytes.length);
    }
  }

  @Test
  void testConstructorsWithPluginClassLoader() throws Exception {
    try (MockConfiguredClassLoader loader =
        new MockConfiguredClassLoader(getClass().getClassLoader())) {
      byte[] bytes;
      try (InputStream is =
          getClass()
              .getResourceAsStream(
                  "/" + SpectraEventsPlugin.class.getName().replace('.', '/') + ".class")) {
        assertNotNull(is);
        bytes = is.readAllBytes();
      }
      Class<?> clazz = loader.definePluginClass(SpectraEventsPlugin.class.getName(), bytes);
      assertNotNull(clazz);

      Constructor<?> ctorWithFactory = clazz.getDeclaredConstructor(Function.class);
      ctorWithFactory.setAccessible(true);
      Object pluginWithFactory =
          ctorWithFactory.newInstance((Function<JavaPlugin, BukkitBootstrapLifecycle>) p -> null);
      assertNotNull(pluginWithFactory);

      Constructor<?> defaultCtor = clazz.getDeclaredConstructor();
      defaultCtor.setAccessible(true);
      Object defaultPlugin = defaultCtor.newInstance();
      assertNotNull(defaultPlugin);
    }
  }

  @Test
  void testFoliaIncompatibleBuildRefusesStartup() throws Exception {
    ServerBuildInfo mockInfo =
        (ServerBuildInfo)
            Proxy.newProxyInstance(
                ServerBuildInfo.class.getClassLoader(),
                new Class<?>[] {ServerBuildInfo.class},
                (proxy, method, args) -> {
                  if ("isBrandCompatible".equals(method.getName())) return true;
                  if ("minecraftVersionId".equals(method.getName())) return "25.4.1";
                  return null;
                });

    withServerBuildInfo(
        mockInfo,
        () -> {
          SpectraEventsPlugin plugin = createPluginInstance(p -> null);
          plugin.onEnable();
          assertNull(plugin.bootstrap());
        });
  }

  @Test
  void testFoliaCompatibleBuildEnablesNormally() throws Exception {
    ServerBuildInfo mockInfo =
        (ServerBuildInfo)
            Proxy.newProxyInstance(
                ServerBuildInfo.class.getClassLoader(),
                new Class<?>[] {ServerBuildInfo.class},
                (proxy, method, args) -> {
                  if ("isBrandCompatible".equals(method.getName())) return true;
                  if ("minecraftVersionId".equals(method.getName())) return "26.2.0";
                  return null;
                });

    AtomicBoolean enabled = new AtomicBoolean(false);
    BukkitBootstrapLifecycle mockBootstrap =
        new BukkitBootstrapLifecycle() {
          @Override
          public void onEnable() {
            enabled.set(true);
          }

          @Override
          public void onDisable() {}
        };

    withServerBuildInfo(
        mockInfo,
        () -> {
          SpectraEventsPlugin plugin = createPluginInstance(p -> mockBootstrap);
          plugin.onEnable();
          assertTrue(enabled.get());
        });
  }

  @Test
  void testOnEnableStartupFailureWithPluginManagerDisableError() throws Exception {
    PluginManager mockPm =
        (PluginManager)
            Proxy.newProxyInstance(
                PluginManager.class.getClassLoader(),
                new Class<?>[] {PluginManager.class},
                (proxy, method, args) -> {
                  if ("disablePlugin".equals(method.getName())) {
                    throw new RuntimeException("Disable plugin failed");
                  }
                  return null;
                });

    Server mockServer =
        (Server)
            Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> {
                  if ("getPluginManager".equals(method.getName())) return mockPm;
                  return null;
                });

    SpectraEventsPlugin plugin =
        createPluginInstance(
            p -> {
              throw new RuntimeException("Startup bootstrap error");
            },
            mockServer);

    plugin.onEnable();
    assertNull(plugin.bootstrap());
  }

  @Test
  void testCleanupWhenSchedulersThrowExceptions() throws Exception {
    io.papermc.paper.threadedregions.scheduler.AsyncScheduler asyncScheduler =
        (io.papermc.paper.threadedregions.scheduler.AsyncScheduler)
            Proxy.newProxyInstance(
                io.papermc.paper.threadedregions.scheduler.AsyncScheduler.class.getClassLoader(),
                new Class<?>[] {io.papermc.paper.threadedregions.scheduler.AsyncScheduler.class},
                (proxy, method, args) -> {
                  if ("cancelTasks".equals(method.getName())) {
                    throw new RuntimeException("AsyncScheduler cancel error");
                  }
                  return null;
                });

    io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler globalScheduler =
        (io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler)
            Proxy.newProxyInstance(
                io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler.class
                },
                (proxy, method, args) -> {
                  if ("cancelTasks".equals(method.getName())) {
                    throw new RuntimeException("GlobalRegionScheduler cancel error");
                  }
                  return null;
                });

    Server mockServer =
        (Server)
            Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> {
                  if ("getAsyncScheduler".equals(method.getName())) return asyncScheduler;
                  if ("getGlobalRegionScheduler".equals(method.getName())) return globalScheduler;
                  return null;
                });

    SpectraEventsPlugin plugin = createPluginInstance(p -> null, mockServer);
    plugin.onDisable();
    assertNull(plugin.bootstrap());
  }

  @Test
  void testPluginCreationAndLifecycle() throws Exception {
    AtomicBoolean enabled = new AtomicBoolean(false);
    AtomicBoolean disabled = new AtomicBoolean(false);

    BukkitBootstrapLifecycle mockBootstrap =
        new BukkitBootstrapLifecycle() {
          @Override
          public void onEnable() {
            enabled.set(true);
          }

          @Override
          public void onDisable() {
            disabled.set(true);
          }
        };

    SpectraEventsPlugin plugin = createPluginInstance(p -> mockBootstrap);
    assertNotNull(plugin);

    plugin.onEnable();
    assertTrue(enabled.get());

    plugin.onDisable();
    assertTrue(disabled.get());
  }

  @Test
  void testOnEnableFailsGracefully() throws Exception {
    SpectraEventsPlugin plugin =
        createPluginInstance(
            p -> {
              throw new IllegalStateException("Simulated startup failure");
            });

    assertNotNull(plugin);
    plugin.onEnable();
    assertNull(plugin.bootstrap());
  }

  @Test
  void testCleanupWhenBootstrapThrowsExceptionOnDisable() throws Exception {
    BukkitBootstrapLifecycle failingBootstrap =
        new BukkitBootstrapLifecycle() {
          @Override
          public void onEnable() {}

          @Override
          public void onDisable() {
            throw new RuntimeException("Teardown error");
          }
        };

    SpectraEventsPlugin plugin = createPluginInstance(p -> failingBootstrap);
    plugin.onEnable();
    plugin.onDisable();
    assertNull(plugin.bootstrap());
  }

  @Test
  void testDefaultConstructorThrowsOutsideBukkitLoader() {
    assertThrows(IllegalStateException.class, SpectraEventsPlugin::new);
  }
}
