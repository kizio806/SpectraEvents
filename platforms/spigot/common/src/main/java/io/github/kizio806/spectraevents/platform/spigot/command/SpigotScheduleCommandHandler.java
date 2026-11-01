package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.schedule.EventScheduleService;
import io.github.kizio806.spectraevents.application.schedule.FileSystemScheduleLoader;
import io.github.kizio806.spectraevents.application.schedule.ScheduleYamlLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.CommandSender;

/** Handles `/spectraevents schedule` subcommands on Spigot. */
public final class SpigotScheduleCommandHandler implements SpigotSubcommandHandler {

  private final SpectraEventsApplication application;
  private final Path schedulesFile;
  private final SpigotCommandHelper helper;

  public SpigotScheduleCommandHandler(
      SpectraEventsApplication application, Path schedulesFile, SpigotCommandHelper helper) {
    this.application = Objects.requireNonNull(application, "application");
    this.schedulesFile = Objects.requireNonNull(schedulesFile, "schedulesFile");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public boolean execute(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.schedule");
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      EventScheduleService service = requireScheduleService();
      helper.send(
          sender, "command.spigot.schedules-title", Map.of("count", service.schedules().size()));
      for (var schedule : service.schedules()) {
        helper.send(
            sender,
            "command.spigot.schedule-entry",
            Map.of(
                "id",
                schedule.id(),
                "definition",
                schedule.definitionId(),
                "zone",
                schedule.zoneId()));
      }
      return true;
    }
    if (!"reload".equalsIgnoreCase(arguments[0])) {
      throw new IllegalArgumentException("Unknown schedule subcommand: " + arguments[0]);
    }
    try {
      var result = new FileSystemScheduleLoader(new ScheduleYamlLoader()).load(schedulesFile);
      requireScheduleService().replace(result.schedules());
      helper.send(
          sender,
          "command.spigot.schedules-reloaded",
          Map.of("loaded", result.schedules().size(), "failed", result.failures().size()));
      for (var failure : result.failures()) {
        helper.send(
            sender,
            "command.spigot.diagnostic-entry",
            Map.of("path", failure.path(), "message", failure.message()));
      }
      return true;
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot read schedules.yml: " + exception.getMessage(), exception);
    }
  }

  private EventScheduleService requireScheduleService() {
    if (application.scheduleService() == null) {
      throw new IllegalStateException("Scheduling is unavailable on this platform");
    }
    return application.scheduleService();
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] arguments) {
    if (arguments.length == 1) {
      return helper.matches(arguments[0], "list", "reload");
    }
    return List.of();
  }
}
