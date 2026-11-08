package io.github.kizio806.spectraevents.platform.paper.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperScheduleCommandHandlerTest {

  @TempDir Path tempDir;
  private SpectraEventsApplication application;
  private Path schedulesFile;
  private LocaleCatalog locales;
  private ScheduleCommandHandler handler;
  private List<Component> messages;
  private CommandDispatcher<CommandSourceStack> dispatcher;
  private CommandSourceStack source;

  @BeforeEach
  void setUp() throws IOException {
    schedulesFile = tempDir.resolve("schedules.yml");
    Files.writeString(
        schedulesFile,
        """
        schema-version: 1
        schedules:
          - id: hourly-boss
            definition: metin-stone
            cron: "0 * * * *"
            timezone: "UTC"
            world: "world"
            x: 100.0
            y: 64.0
            z: 200.0
        """);

    io.github.kizio806.spectraevents.application.port.LifecycleReporter mockReporter =
        (io.github.kizio806.spectraevents.application.port.LifecycleReporter)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.LifecycleReporter.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.LifecycleReporter.class
                },
                (p, m, a) -> null);

    io.github.kizio806.spectraevents.application.port.EventTaskScheduler mockScheduler =
        (io.github.kizio806.spectraevents.application.port.EventTaskScheduler)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.EventTaskScheduler.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.EventTaskScheduler.class
                },
                (p, m, a) -> null);

    io.github.kizio806.spectraevents.application.port.PlatformActionPort mockActionPort =
        (io.github.kizio806.spectraevents.application.port.PlatformActionPort)
            Proxy.newProxyInstance(
                io.github.kizio806.spectraevents.application.port.PlatformActionPort.class
                    .getClassLoader(),
                new Class<?>[] {
                  io.github.kizio806.spectraevents.application.port.PlatformActionPort.class
                },
                (p, m, a) -> java.util.concurrent.CompletableFuture.completedFuture(true));

    io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository
        instanceRepo =
            new io.github.kizio806.spectraevents.application.repository
                .InMemoryEventInstanceRepository();

    application =
        new SpectraEventsApplication(
            mockReporter, mockScheduler, mockActionPort, instanceRepo, null, null, null);

    locales =
        LocaleCatalog.of(
            Map.of(
                "command.paper.schedules-title", "Schedules (%count%):",
                "command.paper.schedule-entry", " - %id% (%definition% @ %zone%)",
                "command.paper.schedules-reloaded", "Reloaded %loaded% schedules (%failed% failed)",
                "command.paper.diagnostic-entry", "[%path%] %message%",
                "command.paper.schedules-read-failed", "Failed to read schedules: %reason%"));

    handler = new ScheduleCommandHandler(application, schedulesFile, locales);
    messages = new ArrayList<>();
    CommandSender sender = PaperCommandTestSupport.createRecordingSender(messages);
    source = PaperCommandTestSupport.createMockSource(sender, null);

    dispatcher = PaperCommandTestSupport.createDispatcher();
    dispatcher.register(handler.build());
  }

  @Test
  void testListAndReloadSchedules() throws Exception {
    // Reload first to populate scheduleService with the schedule from schedulesFile
    messages.clear();
    dispatcher.execute("schedule reload", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("schedule", source);
    assertFalse(messages.isEmpty());

    messages.clear();
    dispatcher.execute("schedule list", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testPermissions() throws Exception {
    CommandSender adminSender =
        createPermissionSender(messages, java.util.Set.of("spectraevents.admin"), false);
    CommandSourceStack adminSource = PaperCommandTestSupport.createMockSource(adminSender, null);
    messages.clear();
    dispatcher.execute("schedule list", adminSource);
    assertFalse(messages.isEmpty());

    CommandSender opSender = createPermissionSender(messages, java.util.Set.of(), true);
    CommandSourceStack opSource = PaperCommandTestSupport.createMockSource(opSender, null);
    messages.clear();
    dispatcher.execute("schedule list", opSource);
    assertFalse(messages.isEmpty());

    CommandSender noPermSender = createPermissionSender(messages, java.util.Set.of(), false);
    CommandSourceStack noPermSource = PaperCommandTestSupport.createMockSource(noPermSender, null);
    messages.clear();
    assertThrows(
        com.mojang.brigadier.exceptions.CommandSyntaxException.class,
        () -> dispatcher.execute("schedule list", noPermSource));
  }

  private static CommandSender createPermissionSender(
      List<Component> recordedMessages, java.util.Set<String> permissions, boolean isOp) {
    return (CommandSender)
        Proxy.newProxyInstance(
            CommandSender.class.getClassLoader(),
            new Class<?>[] {CommandSender.class},
            (p, m, a) -> {
              if ("sendMessage".equals(m.getName())) {
                if (a != null && a.length > 0 && a[0] instanceof Component c) {
                  recordedMessages.add(c);
                }
                return null;
              }
              if ("hasPermission".equals(m.getName()) && a != null && a.length > 0) {
                return permissions.contains(String.valueOf(a[0]));
              }
              if ("getName".equals(m.getName())) {
                return "PermSender";
              }
              if ("isOp".equals(m.getName())) {
                return isOp;
              }
              return null;
            });
  }

  @Test
  void testReloadWithFailuresAndIoException() throws Exception {
    // 1. Write invalid cron schedule to test failure diagnostic path
    Files.writeString(
        schedulesFile,
        """
        schema-version: 1
        schedules:
          - id: broken_schedule
            definition: metin_stone
            cron: "invalid-cron-format"
            timezone: "UTC"
            world: "world"
            x: 0.0
            y: 0.0
            z: 0.0
        """);
    messages.clear();
    dispatcher.execute("schedule reload", source);
    assertFalse(messages.isEmpty());

    // 2. Non-existent file (IOException)
    Path missingFile = tempDir.resolve("missing_dir").resolve("schedules.yml");
    ScheduleCommandHandler missingHandler =
        new ScheduleCommandHandler(application, missingFile, locales);
    CommandDispatcher<CommandSourceStack> missingDispatcher =
        PaperCommandTestSupport.createDispatcher();
    missingDispatcher.register(missingHandler.build());

    messages.clear();
    missingDispatcher.execute("schedule reload", source);
    assertFalse(messages.isEmpty());
  }

  @Test
  void testScheduleServiceUnavailableThrows() {
    // Directly test when scheduleService is null on concrete application
    // Using reflection to null out scheduleService
    try {
      java.lang.reflect.Field field =
          SpectraEventsApplication.class.getDeclaredField("scheduleService");
      field.setAccessible(true);
      field.set(application, null);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    assertThrows(Exception.class, () -> dispatcher.execute("schedule list", source));
  }
}
