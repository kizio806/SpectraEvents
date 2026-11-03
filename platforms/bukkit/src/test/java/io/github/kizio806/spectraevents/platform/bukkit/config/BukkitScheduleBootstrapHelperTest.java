package io.github.kizio806.spectraevents.platform.bukkit.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.port.EventTaskScheduler;
import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.application.port.PlatformCapabilityQuery;
import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.application.repository.InMemoryEventInstanceRepository;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BukkitScheduleBootstrapHelperTest {

  private static final Logger LOGGER = Logger.getLogger("BukkitScheduleBootstrapHelperTest");

  @Test
  void rejectsNullParameters(@TempDir Path tempDir) {
    SpectraEventsApplication app = createDummyApplication();
    Path file = tempDir.resolve("schedules.yml");

    assertThrows(
        NullPointerException.class,
        () -> BukkitScheduleBootstrapHelper.loadAndRegisterSchedules(null, app, LOGGER));
    assertThrows(
        NullPointerException.class,
        () -> BukkitScheduleBootstrapHelper.loadAndRegisterSchedules(file, null, LOGGER));
    assertThrows(
        NullPointerException.class,
        () -> BukkitScheduleBootstrapHelper.loadAndRegisterSchedules(file, app, null));
  }

  @Test
  void handlesMissingSchedulesFileGracefully(@TempDir Path tempDir) {
    SpectraEventsApplication app = createDummyApplication();
    Path nonExistent = tempDir.resolve("non_existent_schedules.yml");

    assertDoesNotThrow(
        () -> BukkitScheduleBootstrapHelper.loadAndRegisterSchedules(nonExistent, app, LOGGER));
  }

  @Test
  void loadsValidSchedulesFileSuccessfully(@TempDir Path tempDir) throws IOException {
    SpectraEventsApplication app = createDummyApplication();
    Path file = tempDir.resolve("schedules.yml");
    Files.writeString(
        file,
        """
        schedules:
          - id: "daily_boss"
            cron: "0 0 12 * * ?"
            event: "boss_metin"
        """);

    assertDoesNotThrow(
        () -> BukkitScheduleBootstrapHelper.loadAndRegisterSchedules(file, app, LOGGER));
  }

  private SpectraEventsApplication createDummyApplication() {
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
    InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();
    PlatformEntityReconcilerPort reconciler =
        (PlatformEntityReconcilerPort)
            Proxy.newProxyInstance(
                PlatformEntityReconcilerPort.class.getClassLoader(),
                new Class<?>[] {PlatformEntityReconcilerPort.class},
                (proxy, method, args) -> null);
    PlatformCapabilityQuery capability =
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

    return new SpectraEventsApplication(
        reporter, scheduler, actionPort, repository, reconciler, capability, renderer);
  }
}
