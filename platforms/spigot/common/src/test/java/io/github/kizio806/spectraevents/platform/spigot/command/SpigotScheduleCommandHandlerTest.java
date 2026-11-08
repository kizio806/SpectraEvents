package io.github.kizio806.spectraevents.platform.spigot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.FileSystemLocaleLoader;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import io.github.kizio806.spectraevents.application.schedule.EventSchedule;
import io.github.kizio806.spectraevents.platform.bukkit.testutil.MockBukkitSetup;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpigotScheduleCommandHandlerTest {

  @BeforeAll
  static void beforeAll() {
    MockBukkitSetup.ensureInitialized();
  }

  @TempDir Path tempDir;
  private SpigotCommandHelper helper;
  private SpectraEventsApplication application;
  private List<String> messages;
  private CommandSender permittedSender;

  @BeforeEach
  void setUp() throws IOException {
    LocaleCatalog locales = new FileSystemLocaleLoader(tempDir).loadCatalog("en-US");
    helper = new SpigotCommandHelper(locales);

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

    messages = new ArrayList<>();
    permittedSender =
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
    Path schedulesFile = tempDir.resolve("schedules.yml");
    assertThrows(
        NullPointerException.class,
        () -> new SpigotScheduleCommandHandler(null, schedulesFile, helper));
    assertThrows(
        NullPointerException.class,
        () -> new SpigotScheduleCommandHandler(application, null, helper));
    assertThrows(
        NullPointerException.class,
        () -> new SpigotScheduleCommandHandler(application, schedulesFile, null));
  }

  @Test
  void tabCompletesSubcommands() {
    SpigotScheduleCommandHandler handler =
        new SpigotScheduleCommandHandler(application, tempDir.resolve("schedules.yml"), helper);

    List<String> completions = handler.tabComplete(permittedSender, new String[] {""});
    assertEquals(List.of("list", "reload"), completions);

    List<String> nullCompletions = handler.tabComplete(null, new String[] {""});
    assertTrue(nullCompletions.isEmpty());

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
    assertTrue(handler.tabComplete(permittedSender, new String[] {"list", "extra"}).isEmpty());
  }

  @Test
  void testExecuteListSchedulesWithAndWithoutSchedules() {
    SpigotScheduleCommandHandler handler =
        new SpigotScheduleCommandHandler(application, tempDir.resolve("schedules.yml"), helper);

    // Empty list
    messages.clear();
    handler.execute(permittedSender, new String[] {"list"});
    assertFalse(messages.isEmpty());

    messages.clear();
    handler.execute(permittedSender, new String[0]);
    assertFalse(messages.isEmpty());

    // With active schedules
    EventSchedule schedule =
        EventSchedule.parse(
            "daily-metin",
            "metin",
            "0 12 * * *",
            "UTC",
            new EventLocation("world", 0.0, 64.0, 0.0, 0.0f, 0.0f));
    application.scheduleService().replace(List.of(schedule));

    messages.clear();
    handler.execute(permittedSender, new String[] {"list"});
    assertFalse(messages.isEmpty());
    assertTrue(messages.stream().anyMatch(m -> m.contains("daily-metin")));
  }

  @Test
  void testExecuteUnknownSubcommand() {
    SpigotScheduleCommandHandler handler =
        new SpigotScheduleCommandHandler(application, tempDir.resolve("schedules.yml"), helper);

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.execute(permittedSender, new String[] {"invalid_cmd"}));
  }

  @Test
  void testExecuteReloadSuccessAndFailures() throws Exception {
    Path schedulesFile = tempDir.resolve("schedules.yml");
    Files.writeString(
        schedulesFile,
        """
        schema-version: 1
        schedules:
          - id: hourly-boss
            definition: metin
            cron: "0 * * * *"
            timezone: "UTC"
            world: "world"
            x: 10
            y: 64
            z: 20
          - id: broken-entry
            definition: metin
            cron: "INVALID CRON"
            timezone: "UTC"
            world: "world"
            x: 0
            y: 0
            z: 0
        """);

    SpigotScheduleCommandHandler handler =
        new SpigotScheduleCommandHandler(application, schedulesFile, helper);

    messages.clear();
    handler.execute(permittedSender, new String[] {"reload"});
    assertFalse(messages.isEmpty());
    assertEquals(1, application.scheduleService().schedules().size());
  }

  @Test
  void testExecuteReloadMissingFileThrowsIllegalStateException() {
    Path nonExistent = tempDir.resolve("non_existent_schedules.yml");
    SpigotScheduleCommandHandler handler =
        new SpigotScheduleCommandHandler(application, nonExistent, helper);

    assertThrows(
        IllegalStateException.class,
        () -> handler.execute(permittedSender, new String[] {"reload"}));
  }

  @Test
  void testExecuteUnavailableScheduleServiceThrowsIllegalStateException() {
    // Application with scheduler = null has scheduleService() == null
    LifecycleReporter reporter =
        (LifecycleReporter)
            Proxy.newProxyInstance(
                LifecycleReporter.class.getClassLoader(),
                new Class<?>[] {LifecycleReporter.class},
                (proxy, method, args) -> null);

    SpectraEventsApplication noSchedulerApp =
        new SpectraEventsApplication(
            reporter, null, null, new InMemoryEventInstanceRepository(), null, null, null);

    SpigotScheduleCommandHandler handler =
        new SpigotScheduleCommandHandler(noSchedulerApp, tempDir.resolve("schedules.yml"), helper);

    assertThrows(
        IllegalStateException.class, () -> handler.execute(permittedSender, new String[] {"list"}));
    assertThrows(
        IllegalStateException.class,
        () -> handler.execute(permittedSender, new String[] {"reload"}));
  }

  @Test
  void testExecuteWithoutPermissionThrows() {
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

    SpigotScheduleCommandHandler handler =
        new SpigotScheduleCommandHandler(application, tempDir.resolve("schedules.yml"), helper);

    assertThrows(
        IllegalStateException.class, () -> handler.execute(noPermSender, new String[] {"list"}));
  }
}
