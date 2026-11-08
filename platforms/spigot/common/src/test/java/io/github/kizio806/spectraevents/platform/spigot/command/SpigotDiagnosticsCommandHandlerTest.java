package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinitionId;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotDiagnosticsCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;
  private SpigotCommandHelper helper;
  private Plugin dummyPlugin;
  private SpectraEventsApplication application;
  private InMemoryEventInstanceRepository repository;
  private List<String> messages;
  private CommandSender sender;

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
                  if ("getDescription".equals(method.getName())) {
                    return new PluginDescriptionFile("SpectraEvents", "1.2.3", "SpigotBootstrap");
                  }
                  return null;
                });

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

    repository = new InMemoryEventInstanceRepository();

    application =
        new SpectraEventsApplication(
            reporter, scheduler, actionPort, repository, reconciler, capabilityQuery, renderer);

    messages = new ArrayList<>();
    sender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("sendMessage".equals(method.getName()) && args != null && args.length > 0) {
                    messages.add(String.valueOf(args[0]));
                    return null;
                  }
                  if ("hasPermission".equals(method.getName())) {
                    return true;
                  }
                  if ("isOp".equals(method.getName())) {
                    return true;
                  }
                  return null;
                });
  }

  @Test
  void rejectsNullConstructorParameters() {
    assertThrows(
        NullPointerException.class,
        () -> new SpigotDiagnosticsCommandHandler(null, application, repository, helper));
    assertThrows(
        NullPointerException.class,
        () -> new SpigotDiagnosticsCommandHandler(dummyPlugin, null, repository, helper));
    assertThrows(
        NullPointerException.class,
        () -> new SpigotDiagnosticsCommandHandler(dummyPlugin, application, null, helper));
    assertThrows(
        NullPointerException.class,
        () -> new SpigotDiagnosticsCommandHandler(dummyPlugin, application, repository, null));
  }

  @Test
  void testStatusExecutionWithNoArgsAndExplicitArg() {
    SpigotDiagnosticsCommandHandler handler =
        new SpigotDiagnosticsCommandHandler(dummyPlugin, application, repository, helper);

    handler.execute(sender, new String[] {});
    assertFalse(messages.isEmpty());

    messages.clear();
    handler.execute(sender, new String[] {"status"});
    assertFalse(messages.isEmpty());
  }

  @Test
  void testVersionExecution() {
    SpigotDiagnosticsCommandHandler handler =
        new SpigotDiagnosticsCommandHandler(dummyPlugin, application, repository, helper);

    handler.execute(sender, new String[] {"version"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("1.2.3")));
  }

  @Test
  void testDoctorExecutionWithInstances() {
    SpigotDiagnosticsCommandHandler handler =
        new SpigotDiagnosticsCommandHandler(dummyPlugin, application, repository, helper);

    EventInstanceId id1 = EventInstanceId.generate();
    EventInstance instance1 =
        EventInstance.reconstitute(
            id1, new EventDefinitionId("metin"), EventLifecycleState.RUNNING, new PhaseId("start"));
    EventRuntimeState state1 = new EventRuntimeState(id1);
    state1.tryClaim("PlayerOne");

    EventInstanceId id2 = EventInstanceId.generate();
    EventInstance instance2 =
        EventInstance.reconstitute(
            id2, new EventDefinitionId("metin"), EventLifecycleState.RUNNING, new PhaseId("start"));
    EventRuntimeState state2 = new EventRuntimeState(id2);

    EventInstanceId id3 = EventInstanceId.generate();
    EventInstance instance3 =
        EventInstance.reconstitute(
            id3,
            new EventDefinitionId("metin"),
            EventLifecycleState.COMPLETED,
            new PhaseId("start"));

    repository.save(instance1);
    repository.saveState(state1);
    repository.save(instance2);
    repository.saveState(state2);
    repository.save(instance3);

    handler.execute(sender, new String[] {"doctor"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("PlayerOne")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("unclaimed")));
  }

  @Test
  void testUnknownSubcommandThrowsIllegalArgumentException() {
    SpigotDiagnosticsCommandHandler handler =
        new SpigotDiagnosticsCommandHandler(dummyPlugin, application, repository, helper);

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(sender, new String[] {"unknownSubcommand"}));
  }

  @Test
  void testPermissionCheckThrows() {
    SpigotDiagnosticsCommandHandler handler =
        new SpigotDiagnosticsCommandHandler(dummyPlugin, application, repository, helper);

    CommandSender noPermSender =
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

    assertThrows(
        IllegalStateException.class, () -> handler.execute(noPermSender, new String[] {"status"}));
    assertThrows(
        IllegalStateException.class, () -> handler.execute(noPermSender, new String[] {"doctor"}));
  }

  @Test
  void testTabComplete() {
    SpigotDiagnosticsCommandHandler handler =
        new SpigotDiagnosticsCommandHandler(dummyPlugin, application, repository, helper);

    assertTrue(handler.tabComplete(null, new String[] {""}).isEmpty());

    CommandSender noPermSender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> {
                  if ("hasPermission".equals(method.getName())) {
                    return false;
                  }
                  return null;
                });

    assertTrue(handler.tabComplete(noPermSender, new String[] {""}).isEmpty());
    assertTrue(handler.tabComplete(sender, new String[] {""}).isEmpty());
  }
}
