package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.spigot.config.SpigotDefinitionConfigBootstrap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Classic Bukkit command adapter exposing the shared SpectraEvents product contract. */
public final class SpigotMainCommand implements CommandExecutor, TabCompleter {
  private final Plugin plugin;
  private final SpectraEventsApplication application;
  private final EventOrchestrationService orchestrationService;
  private final EventInstanceRepository repository;
  private final SpigotDefinitionConfigBootstrap definitions;

  public SpigotMainCommand(
      Plugin plugin,
      SpectraEventsApplication application,
      EventInstanceRepository repository,
      SpigotDefinitionConfigBootstrap definitions) {
    this.plugin = plugin;
    this.application = application;
    this.orchestrationService = application.orchestrationService();
    this.repository = repository;
    this.definitions = definitions;
  }

  @Override
  public boolean onCommand(
      CommandSender sender, Command command, String label, String[] arguments) {
    if (arguments.length == 0 || "help".equalsIgnoreCase(arguments[0])) {
      help(sender);
      return true;
    }
    try {
      return switch (arguments[0].toLowerCase(Locale.ROOT)) {
        case "version" -> version(sender);
        case "status" -> status(sender);
        case "doctor" -> doctor(sender);
        case "event" -> event(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
        case "definition" -> definition(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
        case "assets" -> unavailableAssets(sender);
        case "admin" -> unsupported(sender, "Inventory GUI", "Paper family");
        case "update" -> unsupported(sender, "Update commands", "Paper family");
        default -> {
          help(sender);
          yield true;
        }
      };
    } catch (RuntimeException exception) {
      sender.sendMessage("[SpectraEvents] Command failed: " + exception.getMessage());
      plugin.getLogger().warning("Command failed: " + exception.getMessage());
      return true;
    }
  }

  private boolean event(CommandSender sender, String[] arguments) {
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      require(sender, "spectraevents.event.list");
      List<EventInstance> running =
          repository.findAll().stream()
              .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
              .toList();
      sender.sendMessage("[SpectraEvents] Active events: " + running.size());
      for (EventInstance instance : running) {
        sender.sendMessage(
            " - "
                + instance.id()
                + " "
                + instance.definitionId().value()
                + " phase="
                + instance.currentPhase().map(phase -> phase.value()).orElse("none"));
      }
      return true;
    }

    return switch (arguments[0].toLowerCase(Locale.ROOT)) {
      case "start" -> {
        require(sender, "spectraevents.event.start");
        requireArguments(arguments, 2, "/event event start <definition>");
        Location bukkitLocation = eventLocation(sender);
        EventLocation location =
            new EventLocation(
                bukkitLocation.getWorld().getName(),
                bukkitLocation.getX(),
                bukkitLocation.getY(),
                bukkitLocation.getZ(),
                bukkitLocation.getYaw(),
                bukkitLocation.getPitch());
        EventInstance started = orchestrationService.startDefinition(arguments[1], location);
        sender.sendMessage("[SpectraEvents] Started event instance " + started.id());
        yield true;
      }
      case "stop", "cancel" -> {
        require(sender, "spectraevents.event.cancel");
        requireArguments(arguments, 2, "/event event cancel <instance>");
        EventInstance cancelled = orchestrationService.cancelEvent(arguments[1]);
        sender.sendMessage("[SpectraEvents] Cancelled event instance " + cancelled.id());
        yield true;
      }
      case "trigger" -> {
        require(sender, "spectraevents.event.trigger");
        requireArguments(arguments, 3, "/event event trigger <instance> <trigger>");
        EventInstanceId id = new EventInstanceId(UUID.fromString(arguments[1]));
        ExecutionContext context =
            sender instanceof Player
                ? ExecutionContext.withActor(sender)
                : new ExecutionContext(sender, java.util.Map.of());
        boolean handled =
            application
                .executionEngine()
                .evaluateTrigger(id, new ConfiguredTriggerDefinition(arguments[2]), context);
        sender.sendMessage("[SpectraEvents] Trigger " + arguments[2] + " handled=" + handled);
        yield true;
      }
      case "inspect" -> {
        require(sender, "spectraevents.event.inspect");
        requireArguments(arguments, 2, "/event event inspect <instance>");
        EventInstanceId id = new EventInstanceId(UUID.fromString(arguments[1]));
        EventInstance instance = orchestrationService.getEventInfo(id.toString());
        var diagnostics = application.executionEngine().diagnostics(id);
        sender.sendMessage(
            "[SpectraEvents] Event "
                + id
                + " state="
                + instance.state()
                + " runtimeState="
                + diagnostics.runtimeStatePresent()
                + " tasks="
                + diagnostics.pendingTasks()
                + " resources="
                + diagnostics.platformResources()
                + " claim="
                + diagnostics.claimant().orElse("unclaimed"));
        if (!diagnostics.runtimeStatePresent()
            || (instance.state().isTerminal()
                && (diagnostics.pendingTasks() != 0 || diagnostics.platformResources() != 0))) {
          sender.sendMessage(
              "[SpectraEvents] Recovery guidance: run /event doctor, preserve logs, and back up spectraevents.db before restarting.");
        } else if (diagnostics.claimant().isPresent()) {
          sender.sendMessage(
              "[SpectraEvents] Claim recorded: reconcile any external reward before granting a manual replacement.");
        }
        yield true;
      }
      default -> throw new IllegalArgumentException("Unknown event subcommand: " + arguments[0]);
    };
  }

  private boolean definition(CommandSender sender, String[] arguments) {
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      require(sender, "spectraevents.definition.list");
      sender.sendMessage(
          "[SpectraEvents] Definitions: " + application.definitionRegistry().getAll().size());
      for (var registered : application.definitionRegistry().getAll()) {
        sender.sendMessage(" - " + registered.definition().id().value());
      }
      return true;
    }
    if ("reload".equalsIgnoreCase(arguments[0]) || "validate".equalsIgnoreCase(arguments[0])) {
      require(sender, "spectraevents.definition.reload");
      try {
        boolean validateOnly = "validate".equalsIgnoreCase(arguments[0]);
        DefinitionLoadResult result =
            validateOnly ? definitions.validateFromDisk() : definitions.reloadFromDisk();
        definitions.logLoadResult(result);
        sender.sendMessage(
            "[SpectraEvents] Definitions "
                + (validateOnly ? "validated=" : "loaded=")
                + result.loaded().size()
                + " failed="
                + result.failures().size());
        for (var failure : result.failures()) {
          sender.sendMessage(
              " - "
                  + failure.sourceFile()
                  + ": "
                  + definitions.formatDiagnostics(failure.diagnostics()));
        }
      } catch (java.io.IOException exception) {
        throw new IllegalStateException(
            "Cannot read definitions: " + exception.getMessage(), exception);
      }
      return true;
    }
    throw new IllegalArgumentException("Unknown definition subcommand: " + arguments[0]);
  }

  private boolean status(CommandSender sender) {
    require(sender, "spectraevents.status");
    long running =
        repository.findAll().stream()
            .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
            .count();
    sender.sendMessage(
        "[SpectraEvents] READY platform=spigot definitions="
            + application.definitionRegistry().getAll().size()
            + " running="
            + running);
    return true;
  }

  private boolean doctor(CommandSender sender) {
    require(sender, "spectraevents.doctor");
    status(sender);
    for (EventInstance instance : repository.findAll()) {
      if (instance.state() == EventLifecycleState.RUNNING) {
        String claim =
            repository
                .findState(instance.id())
                .flatMap(state -> state.claimant())
                .orElse("unclaimed");
        sender.sendMessage(
            "[SpectraEvents] instance="
                + instance.id()
                + " state="
                + instance.state()
                + " claim="
                + claim);
      }
    }
    sender.sendMessage(
        "[SpectraEvents] Reward guarantee: in-process at-most-once; crash ambiguity requires operator reconciliation.");
    sender.sendMessage(
        "[SpectraEvents] Recovery guidance: inspect active instances, preserve logs, and back up spectraevents.db before manual recovery.");
    return true;
  }

  private boolean version(CommandSender sender) {
    sender.sendMessage(
        "[SpectraEvents] " + plugin.getDescription().getVersion() + " (Spigot/Bukkit)");
    return true;
  }

  private boolean unavailableAssets(CommandSender sender) {
    sender.sendMessage(
        "[SpectraEvents] Asset import and resource-pack generation are unavailable in this release.");
    return true;
  }

  private boolean unsupported(CommandSender sender, String feature, String supportedPlatform) {
    sender.sendMessage(
        "[SpectraEvents] "
            + feature
            + " is unsupported on Spigot/Bukkit; use the "
            + supportedPlatform
            + " JAR if this feature is required.");
    return true;
  }

  private void help(CommandSender sender) {
    sender.sendMessage("[SpectraEvents] /event status|doctor|version");
    sender.sendMessage("[SpectraEvents] /event event list|start|trigger|inspect|cancel");
    sender.sendMessage("[SpectraEvents] /event definition list|reload|validate");
  }

  private Location eventLocation(CommandSender sender) {
    if (sender instanceof Player player) {
      return player.getLocation();
    }
    if (Bukkit.getWorlds().isEmpty()) {
      throw new IllegalStateException("No loaded world is available for the event location");
    }
    return Bukkit.getWorlds().getFirst().getSpawnLocation();
  }

  private void require(CommandSender sender, String permission) {
    if (!sender.hasPermission(permission)
        && !sender.hasPermission("spectraevents.admin")
        && !sender.isOp()) {
      throw new IllegalStateException("Missing permission: " + permission);
    }
  }

  private void requireArguments(String[] arguments, int count, String usage) {
    if (arguments.length < count) {
      throw new IllegalArgumentException("Usage: " + usage);
    }
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] arguments) {
    if (arguments.length == 1) {
      return matches(
          arguments[0], "help", "version", "status", "doctor", "event", "definition", "assets");
    }
    if (arguments.length == 2 && "event".equalsIgnoreCase(arguments[0])) {
      return matches(arguments[1], "list", "start", "trigger", "inspect", "cancel");
    }
    if (arguments.length == 3
        && "event".equalsIgnoreCase(arguments[0])
        && "start".equalsIgnoreCase(arguments[1])) {
      return application.definitionRegistry().getAll().stream()
          .map(registered -> registered.definition().id().value())
          .filter(value -> value.startsWith(arguments[2].toLowerCase(Locale.ROOT)))
          .toList();
    }
    if (arguments.length == 2 && "definition".equalsIgnoreCase(arguments[0])) {
      return matches(arguments[1], "list", "reload", "validate");
    }
    return List.of();
  }

  private List<String> matches(String prefix, String... candidates) {
    String normalized = prefix.toLowerCase(Locale.ROOT);
    return Arrays.stream(candidates)
        .filter(candidate -> candidate.startsWith(normalized))
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
