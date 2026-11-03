package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotRewardsCommandHandlerTest {

  @TempDir Path tempDir;
  private SpigotCommandHelper helper;
  private Plugin dummyPlugin;
  private BukkitAudiences dummyAudiences;
  private SpectraEventsApplication application;
  private SpigotActionAdapter actionAdapter;

  @AfterEach
  void tearDown() {
    if (dummyAudiences != null) {
      dummyAudiences.close();
    }
  }

  @BeforeEach
  void setUp() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    helper = new SpigotCommandHelper(locales);

    dummyPlugin =
        (Plugin)
            Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> {
                  if ("getDataFolder".equals(method.getName())) {
                    return tempDir.toFile();
                  }
                  return null;
                });

    dummyAudiences =
        (BukkitAudiences)
            Proxy.newProxyInstance(
                BukkitAudiences.class.getClassLoader(),
                new Class<?>[] {BukkitAudiences.class},
                (proxy, method, args) -> null);

    actionAdapter = new SpigotActionAdapter(dummyPlugin, dummyAudiences, locales);

    LifecycleReporter reporter =
        (LifecycleReporter)
            Proxy.newProxyInstance(
                LifecycleReporter.class.getClassLoader(),
                new Class<?>[] {LifecycleReporter.class},
                (proxy, method, args) -> null);

    EventTaskScheduler scheduler =
        (EventTaskScheduler)
            Proxy.newProxyInstance(
                EventTaskScheduler.class.getClassLoader(),
                new Class<?>[] {EventTaskScheduler.class},
                (proxy, method, args) -> null);

    PlatformActionPort actionPort =
        (PlatformActionPort)
            Proxy.newProxyInstance(
                PlatformActionPort.class.getClassLoader(),
                new Class<?>[] {PlatformActionPort.class},
                (proxy, method, args) -> null);

    PlatformEntityReconcilerPort reconciler =
        (PlatformEntityReconcilerPort)
            Proxy.newProxyInstance(
                PlatformEntityReconcilerPort.class.getClassLoader(),
                new Class<?>[] {PlatformEntityReconcilerPort.class},
                (proxy, method, args) -> null);

    PlatformCapabilityQuery capabilityQuery =
        (PlatformCapabilityQuery)
            Proxy.newProxyInstance(
                PlatformCapabilityQuery.class.getClassLoader(),
                new Class<?>[] {PlatformCapabilityQuery.class},
                (proxy, method, args) -> null);

    ModelRendererPort renderer =
        (ModelRendererPort)
            Proxy.newProxyInstance(
                ModelRendererPort.class.getClassLoader(),
                new Class<?>[] {ModelRendererPort.class},
                (proxy, method, args) -> null);

    application =
        new SpectraEventsApplication(
            reporter,
            scheduler,
            actionPort,
            new InMemoryEventInstanceRepository(),
            reconciler,
            capabilityQuery,
            renderer);
  }

  @Test
  void rejectsNullConstructorParameters() {
    assertThrows(
        NullPointerException.class,
        () -> new SpigotRewardsCommandHandler(null, null, null, helper));
  }

  @Test
  void tabCompletesSubcommandsForPermittedSender() {
    CommandSender permittedSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return "spectraevents.rewards".equals(args[0]);
                  }
                  return null;
                });

    SpigotRewardsCommandHandler handler =
        new SpigotRewardsCommandHandler(dummyPlugin, application, actionAdapter, helper);

    List<String> completions = handler.tabComplete(permittedSender, new String[] {""});
    assertEquals(List.of("list", "claim"), completions);
  }
}
