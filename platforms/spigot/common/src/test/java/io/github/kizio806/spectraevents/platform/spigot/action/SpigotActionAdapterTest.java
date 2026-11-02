package io.github.kizio806.spectraevents.platform.spigot.action;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.logging.Logger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotActionAdapterTest {

  @TempDir Path tempDir;

  @Test
  void testSpigotActionAdapterScoreboardAndBossbarValidations() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");

    BukkitScheduler scheduler =
        (BukkitScheduler)
            Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(),
                new Class<?>[] {BukkitScheduler.class},
                (proxy, method, args) -> {
                  if ("runTask".equals(method.getName()) && args != null && args.length >= 2) {
                    if (args[1] instanceof Runnable r) {
                      r.run();
                    }
                  }
                  return null;
                });

    Server server =
        (Server)
            Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> {
                  if ("getScheduler".equals(method.getName())) {
                    return scheduler;
                  }
                  return null;
                });

    Plugin plugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getServer".equals(method.getName())) {
                    return server;
                  }
                  if ("getLogger".equals(method.getName())) {
                    return Logger.getLogger("test");
                  }
                  return null;
                });

    BukkitAudiences adventure =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null);

    SpigotActionAdapter adapter = new SpigotActionAdapter(plugin, adventure, locales);
    assertNotNull(adapter);

    EventInstanceId id = EventInstanceId.generate();
    EventInstance instance = EventInstance.create(id, new EventDefinitionId("test"));
    EventRuntimeState state = new EventRuntimeState(id);

    PlatformActions.UpdateScoreboardAction updateAction =
        new PlatformActions.UpdateScoreboardAction(java.util.List.of("line1"));
    assertTrue(
        adapter
            .handleUpdateScoreboard(instance, state, updateAction, ExecutionContext.EMPTY)
            .join());

    assertThrows(
        NullPointerException.class,
        () -> adapter.handleUpdateScoreboard(null, state, updateAction, ExecutionContext.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleUpdateScoreboard(instance, null, updateAction, ExecutionContext.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleUpdateScoreboard(instance, state, null, ExecutionContext.EMPTY));

    assertThrows(
        NullPointerException.class,
        () ->
            adapter.handleRemoveBossbar(
                instance, null, new PlatformActions.RemoveBossbarAction(), ExecutionContext.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleRemoveBossbar(instance, state, null, ExecutionContext.EMPTY));

    assertThrows(
        NullPointerException.class,
        () ->
            adapter.handleRemoveScoreboard(
                instance,
                null,
                new PlatformActions.RemoveScoreboardAction(),
                ExecutionContext.EMPTY));
    assertThrows(
        NullPointerException.class,
        () -> adapter.handleRemoveScoreboard(instance, state, null, ExecutionContext.EMPTY));
  }
}
