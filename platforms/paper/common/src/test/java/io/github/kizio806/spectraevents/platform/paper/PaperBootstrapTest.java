package io.github.kizio806.spectraevents.platform.paper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperBootstrapTest {

  @TempDir Path tempDir;

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @Test
  void testBootstrapEnableAndDisable() throws Exception {
    Path dataDir = tempDir.resolve("data");
    Path templatesDir = dataDir.resolve("templates");
    Files.createDirectories(templatesDir);
    Path templateZip = templatesDir.resolve("metin.spectra.zip");
    Files.writeString(templateZip, "mock-zip-content");

    Path eventsDir = dataDir.resolve("events");
    Files.createDirectories(eventsDir);
    Files.writeString(
        eventsDir.resolve("sample.yml"),
        "schema-version: \"1\"\n"
            + "id: \"sample\"\n"
            + "initial-phase: \"idle\"\n"
            + "phases:\n"
            + "  idle:\n"
            + "    transitions: []\n");

    Files.writeString(
        dataDir.resolve("resource-pack.yml"),
        "enabled: true\n"
            + "required: false\n"
            + "url: \"https://example.com/pack.zip\"\n"
            + "sha1: \"0123456789abcdef0123456789abcdef01234567\"\n");

    PluginMeta meta =
        (PluginMeta)
            Proxy.newProxyInstance(
                PluginMeta.class.getClassLoader(),
                new Class<?>[] {PluginMeta.class},
                (proxy, method, args) -> {
                  if ("getVersion".equals(method.getName())) return "1.0.0";
                  if ("getName".equals(method.getName())) return "SpectraEvents";
                  return null;
                });

    io.papermc.paper.command.brigadier.Commands mockCommands =
        (io.papermc.paper.command.brigadier.Commands)
            Proxy.newProxyInstance(
                io.papermc.paper.command.brigadier.Commands.class.getClassLoader(),
                new Class<?>[] {io.papermc.paper.command.brigadier.Commands.class},
                (proxy, method, args) -> java.util.Set.of());

    io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent<?> mockEvent =
        (io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent<?>)
            Proxy.newProxyInstance(
                io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent.class
                },
                (proxy, method, args) -> {
                  if ("registrar".equals(method.getName())) return mockCommands;
                  return null;
                });

    LifecycleEventManager<?> lifecycleManager =
        (LifecycleEventManager<?>)
            Proxy.newProxyInstance(
                LifecycleEventManager.class.getClassLoader(),
                new Class<?>[] {LifecycleEventManager.class},
                (proxy, method, args) -> {
                  if ("registerEventHandler".equals(method.getName()) && args != null) {
                    for (Object arg : args) {
                      if (arg == null) continue;
                      for (Field field : arg.getClass().getDeclaredFields()) {
                        field.setAccessible(true);
                        try {
                          Object val = field.get(arg);
                          if (val != null) {
                            for (Method m : val.getClass().getMethods()) {
                              if ("run".equals(m.getName()) && m.getParameterCount() == 1) {
                                m.invoke(val, mockEvent);
                              }
                            }
                          }
                        } catch (ReflectiveOperationException ignored) {
                          // Ignore reflection exceptions during mock dispatch
                        }
                      }
                      for (Method m : arg.getClass().getMethods()) {
                        if ("run".equals(m.getName()) && m.getParameterCount() == 1) {
                          try {
                            m.invoke(arg, mockEvent);
                          } catch (ReflectiveOperationException ignored) {
                            // Ignore reflection exceptions during mock dispatch
                          }
                        }
                      }
                    }
                  }
                  return null;
                });

    Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
    Field f = unsafeClass.getDeclaredField("theUnsafe");
    f.setAccessible(true);
    Object unsafe = f.get(null);
    Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
    SpectraEventsPlugin plugin =
        (SpectraEventsPlugin) allocateInstance.invoke(unsafe, SpectraEventsPlugin.class);

    setField(JavaPlugin.class, plugin, "server", Bukkit.getServer());
    setField(JavaPlugin.class, plugin, "dataFolder", dataDir.toFile());
    setField(JavaPlugin.class, plugin, "file", tempDir.resolve("plugin.jar").toFile());
    setField(JavaPlugin.class, plugin, "logger", Logger.getLogger("SpectraEvents"));
    setField(JavaPlugin.class, plugin, "pluginMeta", meta);
    setField(
        JavaPlugin.class,
        plugin,
        "description",
        new PluginDescriptionFile("SpectraEvents", "1.0.0", SpectraEventsPlugin.class.getName()));
    setField(JavaPlugin.class, plugin, "lifecycleEventManager", lifecycleManager);

    PaperBootstrap bootstrap = new PaperBootstrap(plugin);

    // Disable before enable (all null paths)
    assertDoesNotThrow(bootstrap::disable);

    // Enable and disable
    assertDoesNotThrow(bootstrap::onEnable);
    assertDoesNotThrow(bootstrap::onDisable);

    // Test disable() fallback: cleaner != null when actionAdapter == null
    setField(PaperBootstrap.class, bootstrap, "actionAdapter", null);
    io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer mockRenderer =
        new io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer(plugin);
    io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner mockCleaner =
        new io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner(
            mockRenderer,
            (io.github.kizio806.spectraevents.application.port.EventTaskScheduler)
                Proxy.newProxyInstance(
                    io.github.kizio806.spectraevents.application.port.EventTaskScheduler.class
                        .getClassLoader(),
                    new Class<?>[] {
                      io.github.kizio806.spectraevents.application.port.EventTaskScheduler.class
                    },
                    (p, m, a) -> null));
    setField(PaperBootstrap.class, bootstrap, "cleaner", mockCleaner);
    assertDoesNotThrow(bootstrap::disable);

    // Test disable() fallback: sqliteRepository != null when application == null
    setField(PaperBootstrap.class, bootstrap, "cleaner", null);
    setField(PaperBootstrap.class, bootstrap, "application", null);
    io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository repo =
        new io.github.kizio806.spectraevents.adapter.storage.sqlite.SQLiteEventInstanceRepository(
            tempDir.resolve("test_db.sqlite"));
    setField(PaperBootstrap.class, bootstrap, "sqliteRepository", repo);
    assertDoesNotThrow(bootstrap::disable);

    // Test remaining lambdas and helpers directly
    io.papermc.paper.threadedregions.scheduler.EntityScheduler mockScheduler =
        (io.papermc.paper.threadedregions.scheduler.EntityScheduler)
            Proxy.newProxyInstance(
                io.papermc.paper.threadedregions.scheduler.EntityScheduler.class.getClassLoader(),
                new Class<?>[] {io.papermc.paper.threadedregions.scheduler.EntityScheduler.class},
                (p, m, a) -> {
                  if ("execute".equals(m.getName())
                      && a != null
                      && a.length > 1
                      && a[1] instanceof Runnable r) {
                    r.run();
                    return true;
                  }
                  return false;
                });
    org.bukkit.entity.Player mockPlayer =
        (org.bukkit.entity.Player)
            Proxy.newProxyInstance(
                org.bukkit.entity.Player.class.getClassLoader(),
                new Class<?>[] {org.bukkit.entity.Player.class},
                (p, m, a) -> {
                  if ("getScheduler".equals(m.getName())) return mockScheduler;
                  return null;
                });

    Method m4 =
        PaperBootstrap.class.getDeclaredMethod(
            "lambda$enable$4", org.bukkit.entity.Player.class, Runnable.class);
    m4.setAccessible(true);
    m4.invoke(bootstrap, mockPlayer, (Runnable) () -> {});

    Method m2 =
        PaperBootstrap.class.getDeclaredMethod(
            "lambda$enable$2",
            io.github.kizio806.spectraevents.application.update.UpdateInfo.class);
    m2.setAccessible(true);
    m2.invoke(
        bootstrap,
        new io.github.kizio806.spectraevents.application.update.UpdateInfo(
            "1.0.0",
            "1.1.0",
            true,
            "https://example.com",
            "notes",
            java.time.Instant.now(),
            io.github.kizio806.spectraevents.application.update.UpdateCheckStatus
                .UPDATE_AVAILABLE));
    m2.invoke(
        bootstrap,
        io.github.kizio806.spectraevents.application.update.UpdateInfo.upToDate("1.0.0"));

    Method m3 = PaperBootstrap.class.getDeclaredMethod("lambda$enable$3", Throwable.class);
    m3.setAccessible(true);
    m3.invoke(bootstrap, new RuntimeException("Simulated update error"));
  }

  private static void setField(Class<?> clazz, Object target, String name, Object value)
      throws ReflectiveOperationException {
    Field f = clazz.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }
}
