package io.github.kizio806.spectraevents.platform.bukkit.config;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.schedule.FileSystemScheduleLoader;
import io.github.kizio806.spectraevents.application.schedule.ScheduleLoadResult;
import io.github.kizio806.spectraevents.application.schedule.ScheduleYamlLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

/** Shared helper for loading and attaching event schedules across Bukkit platform families. */
public final class BukkitScheduleBootstrapHelper {

  private BukkitScheduleBootstrapHelper() {}

  public static void loadAndRegisterSchedules(
      Path schedulesFile, SpectraEventsApplication application, Logger logger) {
    Objects.requireNonNull(schedulesFile, "schedulesFile");
    Objects.requireNonNull(application, "application");
    Objects.requireNonNull(logger, "logger");

    try {
      ScheduleLoadResult schedules =
          new FileSystemScheduleLoader(new ScheduleYamlLoader()).load(schedulesFile);
      schedules
          .failures()
          .forEach(
              failure ->
                  logger.warning(
                      "Ignoring invalid schedule " + failure.path() + ": " + failure.message()));
      application.startSchedules(schedules.schedules());
      logger.info("Loaded schedules=" + schedules.schedules().size());
    } catch (IOException exception) {
      logger.warning(
          "Could not load schedules.yml; scheduling remains disabled: " + exception.getMessage());
    }
  }
}
