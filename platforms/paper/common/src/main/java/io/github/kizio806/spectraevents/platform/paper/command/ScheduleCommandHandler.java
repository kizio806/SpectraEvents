package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.schedule.EventScheduleService;
import io.github.kizio806.spectraevents.application.schedule.FileSystemScheduleLoader;
import io.github.kizio806.spectraevents.application.schedule.ScheduleYamlLoader;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Brigadier command handler for schedule subcommands (/event schedule). */
public final class ScheduleCommandHandler {
  private final SpectraEventsApplication application;
  private final Path schedulesFile;
  private final CommandText messages;

  public ScheduleCommandHandler(
      SpectraEventsApplication application, Path schedulesFile, LocaleCatalog locales) {
    this.application = Objects.requireNonNull(application, "application");
    this.schedulesFile = Objects.requireNonNull(schedulesFile, "schedulesFile");
    this.messages = new CommandText(Objects.requireNonNull(locales, "locales"));
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("schedule")
        .requires(source -> hasPerm(source, "spectraevents.schedule"))
        .executes(this::listSchedules)
        .then(Commands.literal("list").executes(this::listSchedules))
        .then(Commands.literal("reload").executes(this::reloadSchedules));
  }

  private int listSchedules(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    EventScheduleService service = requireScheduleService();
    sender.sendMessage(
        messages.component(
            "command.paper.schedules-title",
            NamedTextColor.AQUA,
            Map.of("count", service.schedules().size())));
    for (var schedule : service.schedules()) {
      sender.sendMessage(
          messages.component(
              "command.paper.schedule-entry",
              NamedTextColor.GRAY,
              Map.of(
                  "id", schedule.id(),
                  "definition", schedule.definitionId(),
                  "zone", schedule.zoneId())));
    }
    return 1;
  }

  private int reloadSchedules(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    try {
      var result = new FileSystemScheduleLoader(new ScheduleYamlLoader()).load(schedulesFile);
      requireScheduleService().replace(result.schedules());
      sender.sendMessage(
          messages.component(
              "command.paper.schedules-reloaded",
              result.failures().isEmpty() ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
              Map.of("loaded", result.schedules().size(), "failed", result.failures().size())));
      result
          .failures()
          .forEach(
              failure ->
                  sender.sendMessage(
                      messages.component(
                          "command.paper.diagnostic-entry",
                          NamedTextColor.RED,
                          Map.of("path", failure.path(), "message", failure.message()))));
      return 1;
    } catch (IOException exception) {
      sender.sendMessage(
          messages.component(
              "command.paper.schedules-read-failed",
              NamedTextColor.RED,
              Map.of("reason", exception.getMessage())));
      return 0;
    }
  }

  private EventScheduleService requireScheduleService() {
    if (application.scheduleService() == null) {
      throw new IllegalStateException("Scheduling is unavailable on this platform");
    }
    return application.scheduleService();
  }

  private boolean hasPerm(CommandSourceStack source, String perm) {
    CommandSender sender = source.getSender();
    return sender.hasPermission(perm)
        || sender.hasPermission("spectraevents.admin")
        || sender.isOp();
  }
}
