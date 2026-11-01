package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Handles `/spectraevents event` subcommands on Spigot. */
public final class SpigotEventCommandHandler implements SpigotSubcommandHandler {

  private final Plugin plugin;
  private final SpectraEventsApplication application;
  private final EventOrchestrationService orchestrationService;
  private final EventInstanceRepository repository;
  private final SpigotCommandHelper helper;

  public SpigotEventCommandHandler(
      Plugin plugin,
      SpectraEventsApplication application,
      EventInstanceRepository repository,
      SpigotCommandHelper helper) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.application = Objects.requireNonNull(application, "application");
    this.orchestrationService = application.orchestrationService();
    this.repository = Objects.requireNonNull(repository, "repository");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public boolean execute(CommandSender sender, String[] arguments) {
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      return list(sender);
    }
    return switch (arguments[0].toLowerCase(Locale.ROOT)) {
      case "start" -> start(sender, arguments);
      case "cancel" -> cancel(sender, arguments);
      case "trigger" -> trigger(sender, arguments);
      case "inspect" -> inspect(sender, arguments);
      default -> throw new IllegalArgumentException("Unknown event subcommand: " + arguments[0]);
    };
  }

  private boolean list(CommandSender sender) {
    helper.require(sender, "spectraevents.event.list");
    List<EventInstance> running =
        repository.findAll().stream()
            .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
            .toList();
    helper.send(sender, "command.spigot.events-title", Map.of("count", running.size()));
    for (EventInstance instance : running) {
      helper.send(
          sender,
          "command.spigot.event-entry",
          Map.of(
              "id", instance.id(),
              "definition", instance.definitionId().value(),
              "phase", instance.currentPhase().map(phase -> phase.value()).orElse("none")));
    }
    return true;
  }

  private boolean start(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.event.start");
    helper.requireArguments(arguments, 2, "/spectraevents event start <definition>");
    Location bukkitLocation = helper.eventLocation(sender);
    EventLocation location =
        new EventLocation(
            bukkitLocation.getWorld().getName(),
            bukkitLocation.getX(),
            bukkitLocation.getY(),
            bukkitLocation.getZ(),
            bukkitLocation.getYaw(),
            bukkitLocation.getPitch());
    var _ =
        orchestrationService
            .startDefinitionAsync(arguments[1], location)
            .whenComplete(
                (started, failure) ->
                    Bukkit.getScheduler()
                        .runTask(
                            plugin,
                            () -> {
                              if (failure != null) {
                                helper.send(
                                    sender,
                                    "command.spigot.failed",
                                    Map.of("reason", helper.messageFor(failure)));
                                return;
                              }
                              helper.send(
                                  sender,
                                  "command.spigot.event-started",
                                  Map.of("id", started.id()));
                            }));
    return true;
  }

  private boolean cancel(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.event.cancel");
    helper.requireArguments(arguments, 2, "/spectraevents event cancel <instance>");
    EventInstance cancelled = orchestrationService.cancelEvent(arguments[1]);
    helper.send(sender, "command.spigot.event-cancelled", Map.of("id", cancelled.id()));
    return true;
  }

  private boolean trigger(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.event.trigger");
    helper.requireArguments(arguments, 3, "/spectraevents event trigger <instance> <trigger>");
    EventInstanceId id = new EventInstanceId(UUID.fromString(arguments[1]));
    ExecutionContext context =
        sender instanceof Player player
            ? ExecutionContext.withActor(player, player.getUniqueId())
            : new ExecutionContext(sender, Map.of());
    boolean handled =
        application
            .executionEngine()
            .evaluateTrigger(id, new ConfiguredTriggerDefinition(arguments[2]), context);
    helper.send(
        sender,
        "command.spigot.trigger-result",
        Map.of("trigger", arguments[2], "handled", handled));
    return true;
  }

  private boolean inspect(CommandSender sender, String[] arguments) {
    helper.require(sender, "spectraevents.event.inspect");
    helper.requireArguments(arguments, 2, "/spectraevents event inspect <instance>");
    EventInstanceId id = new EventInstanceId(UUID.fromString(arguments[1]));
    EventInstance instance = orchestrationService.getEventInfo(id.toString());
    var diagnostics = application.executionEngine().diagnostics(id);
    helper.send(
        sender,
        "command.spigot.inspect",
        Map.of(
            "id", id,
            "state", instance.state(),
            "runtime", diagnostics.runtimeStatePresent(),
            "tasks", diagnostics.pendingTasks(),
            "resources", diagnostics.platformResources(),
            "claim", diagnostics.claimant() != null ? diagnostics.claimant() : "unclaimed"));
    if ((!instance.state().isTerminal() && !diagnostics.runtimeStatePresent())
        || (instance.state().isTerminal()
            && (diagnostics.pendingTasks() != 0 || diagnostics.platformResources() != 0))) {
      helper.send(sender, "command.spigot.inspect-recovery-guidance");
    } else if (diagnostics.claimant() != null) {
      helper.send(sender, "command.spigot.inspect-claim-guidance");
    }
    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] arguments) {
    if (arguments.length == 1) {
      return helper.matches(arguments[0], "list", "start", "trigger", "inspect", "cancel");
    }
    if (arguments.length == 2 && "start".equalsIgnoreCase(arguments[0])) {
      return application.definitionRegistry().getAll().stream()
          .map(registered -> registered.definition().id().value())
          .filter(value -> value.startsWith(arguments[1].toLowerCase(Locale.ROOT)))
          .toList();
    }
    return List.of();
  }
}
