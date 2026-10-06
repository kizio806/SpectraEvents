package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.loader.DefinitionLoadResult;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.validation.ValidationDiagnostic;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.application.validation.OperationalReadinessValidator;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import io.github.kizio806.spectraevents.platform.spigot.asset.delivery.SpigotResourcePackDeliveryBootstrap;
import io.github.kizio806.spectraevents.platform.spigot.config.SpigotDefinitionConfigBootstrap;
import java.io.IOException;
import java.nio.file.Path;
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
  private final SpigotActionAdapter actionAdapter;
  private final Path schedulesFile;
  private final LocaleCatalog locales;

  public SpigotMainCommand(
      Plugin plugin,
      SpectraEventsApplication application,
      EventInstanceRepository repository,
      SpigotDefinitionConfigBootstrap definitions,
      SpigotActionAdapter actionAdapter,
      LocaleCatalog locales) {
    this.plugin = plugin;
    this.application = application;
    this.orchestrationService = application.orchestrationService();
    this.repository = repository;
    this.definitions = definitions;
    this.actionAdapter = actionAdapter;
    this.schedulesFile = plugin.getDataFolder().toPath().resolve("schedules.yml");
    this.locales = locales;
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
        case "validate" -> definition(sender, new String[] {"validate"});
        case "template" -> template(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
        case "schedule" -> schedule(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
        case "rewards" -> rewards(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
        case "assets" -> unavailableAssets(sender);
        case "admin" -> unsupported(sender, "Inventory GUI", "Paper family");
        case "update" -> unsupported(sender, "Update commands", "Paper family");
        default -> {
          help(sender);
          yield true;
        }
      };
    } catch (RuntimeException exception) {
      send(sender, "command.spigot.failed", java.util.Map.of("reason", exception.getMessage()));
      plugin.getLogger().warning("Command failed: " + exception.getMessage());
      return true;
    }
  }

  private boolean schedule(CommandSender sender, String[] arguments) {
    require(sender, "spectraevents.schedule");
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      var service = requireScheduleService();
      send(
          sender,
          "command.spigot.schedules-title",
          java.util.Map.of("count", service.schedules().size()));
      for (var schedule : service.schedules()) {
        send(
            sender,
            "command.spigot.schedule-entry",
            java.util.Map.of(
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
      var result =
          new io.github.kizio806.spectraevents.application.schedule.FileSystemScheduleLoader(
                  new io.github.kizio806.spectraevents.application.schedule.ScheduleYamlLoader())
              .load(schedulesFile);
      requireScheduleService().replace(result.schedules());
      send(
          sender,
          "command.spigot.schedules-reloaded",
          java.util.Map.of(
              "loaded", result.schedules().size(), "failed", result.failures().size()));
      for (var failure : result.failures()) {
        send(
            sender,
            "command.spigot.diagnostic-entry",
            java.util.Map.of("path", failure.path(), "message", failure.message()));
      }
      return true;
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot read schedules.yml: " + exception.getMessage(), exception);
    }
  }

  private io.github.kizio806.spectraevents.application.schedule.EventScheduleService
      requireScheduleService() {
    if (application.scheduleService() == null) {
      throw new IllegalStateException("Scheduling is unavailable on this platform");
    }
    return application.scheduleService();
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private boolean rewards(CommandSender sender, String[] arguments) {
    if (arguments.length > 0 && "reconcile".equalsIgnoreCase(arguments[0])) {
      return reconcileRewards(sender, Arrays.copyOfRange(arguments, 1, arguments.length));
    }
    require(sender, "spectraevents.rewards.claim");
    if (!(sender instanceof Player player)) {
      throw new IllegalStateException("Rewards are available to in-game players only");
    }
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      application
          .rewardMailboxService()
          .pendingClaims(player.getUniqueId())
          .whenComplete(
              (claims, failure) ->
                  Bukkit.getScheduler()
                      .runTask(
                          plugin,
                          () -> {
                            if (failure != null) {
                              send(
                                  sender,
                                  "command.spigot.failed",
                                  java.util.Map.of("reason", messageFor(failure)));
                              return;
                            }
                            send(
                                sender,
                                "command.spigot.rewards-title",
                                java.util.Map.of("count", claims.size()));
                            for (var claim : claims) {
                              send(
                                  sender,
                                  "command.spigot.reward-entry",
                                  java.util.Map.of(
                                      "id", claim.id(), "count", claim.items().size()));
                            }
                          }));
      return true;
    }
    if (!"claim".equalsIgnoreCase(arguments[0])) {
      throw new IllegalArgumentException("Unknown rewards subcommand: " + arguments[0]);
    }
    requireArguments(arguments, 2, "/spectraevents rewards claim <claim-id>");
    UUID claimId = UUID.fromString(arguments[1]);
    application
        .rewardMailboxService()
        .claim(
            player.getUniqueId(),
            claimId,
            (ignored, items) -> actionAdapter.deliverRewardItems(player, items))
        .whenComplete(
            (result, failure) ->
                Bukkit.getScheduler()
                    .runTask(
                        plugin,
                        () -> {
                          if (failure != null) {
                            send(
                                sender,
                                "command.spigot.failed",
                                java.util.Map.of("reason", messageFor(failure)));
                            return;
                          }
                          send(
                              sender,
                              switch (result.status()) {
                                case DELIVERED -> "messages.reward-delivered";
                                case RETAINED -> "messages.reward-retained";
                                case BUSY -> "command.spigot.reward-busy";
                                case NOT_AVAILABLE -> "command.spigot.reward-unavailable";
                              });
                        }));
    return true;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private boolean reconcileRewards(CommandSender sender, String[] arguments) {
    require(sender, "spectraevents.rewards.reconcile");
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      application
          .rewardMailboxService()
          .deliveringClaims()
          .whenComplete(
              (claims, failure) ->
                  Bukkit.getScheduler()
                      .runTask(
                          plugin,
                          () -> {
                            if (failure != null) {
                              send(
                                  sender,
                                  "command.spigot.failed",
                                  java.util.Map.of("reason", messageFor(failure)));
                              return;
                            }
                            send(
                                sender,
                                "command.rewards.reconcile-title",
                                java.util.Map.of("count", claims.size()));
                            for (var claim : claims) {
                              send(
                                  sender,
                                  "command.rewards.reconcile-entry",
                                  java.util.Map.of(
                                      "id",
                                      claim.id(),
                                      "player",
                                      claim.playerId(),
                                      "count",
                                      claim.items().size()));
                            }
                          }));
      return true;
    }
    if (arguments.length != 2
        || (!"mark-delivered".equalsIgnoreCase(arguments[0])
            && !"return-pending".equalsIgnoreCase(arguments[0]))) {
      throw new IllegalArgumentException(
          "Usage: /spectraevents rewards reconcile <list|mark-delivered|return-pending> [claim-id]");
    }
    UUID claimId = UUID.fromString(arguments[1]);
    var decision =
        "mark-delivered".equalsIgnoreCase(arguments[0])
            ? io.github.kizio806.spectraevents.application.service.RewardMailboxService
                .ReconciliationDecision.MARK_DELIVERED
            : io.github.kizio806.spectraevents.application.service.RewardMailboxService
                .ReconciliationDecision.RETURN_TO_PENDING;
    application
        .rewardMailboxService()
        .reconcile(claimId, decision)
        .whenComplete(
            (resolved, failure) ->
                Bukkit.getScheduler()
                    .runTask(
                        plugin,
                        () -> {
                          if (failure != null) {
                            send(
                                sender,
                                "command.spigot.failed",
                                java.util.Map.of("reason", messageFor(failure)));
                            return;
                          }
                          send(
                              sender,
                              resolved
                                  ? "command.rewards.reconcile-resolved"
                                  : "command.rewards.reconcile-not-found",
                              java.util.Map.of("id", claimId, "decision", decision.name()));
                        }));
    return true;
  }

  @SuppressWarnings(
      "FutureReturnValueIgnored") // Completion sends the platform-thread command reply.
  private boolean event(CommandSender sender, String[] arguments) {
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      require(sender, "spectraevents.event.list");
      List<EventInstance> running =
          repository.findAll().stream()
              .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
              .toList();
      send(sender, "command.spigot.events-title", java.util.Map.of("count", running.size()));
      for (EventInstance instance : running) {
        send(
            sender,
            "command.spigot.event-entry",
            java.util.Map.of(
                "id", instance.id(),
                "definition", instance.definitionId().value(),
                "phase", instance.currentPhase().map(phase -> phase.value()).orElse("none")));
      }
      return true;
    }

    return switch (arguments[0].toLowerCase(Locale.ROOT)) {
      case "start" -> {
        require(sender, "spectraevents.event.start");
        requireArguments(arguments, 2, "/spectraevents event start <definition>");
        Location bukkitLocation = eventLocation(sender);
        EventLocation location =
            new EventLocation(
                bukkitLocation.getWorld().getName(),
                bukkitLocation.getX(),
                bukkitLocation.getY(),
                bukkitLocation.getZ(),
                bukkitLocation.getYaw(),
                bukkitLocation.getPitch());
        orchestrationService
            .startDefinitionAsync(arguments[1], location)
            .whenComplete(
                (started, failure) ->
                    Bukkit.getScheduler()
                        .runTask(
                            plugin,
                            () -> {
                              if (failure != null) {
                                send(
                                    sender,
                                    "command.spigot.failed",
                                    java.util.Map.of("reason", messageFor(failure)));
                                return;
                              }
                              send(
                                  sender,
                                  "command.spigot.event-started",
                                  java.util.Map.of("id", started.id()));
                            }));
        yield true;
      }
      case "cancel" -> {
        require(sender, "spectraevents.event.cancel");
        requireArguments(arguments, 2, "/spectraevents event cancel <instance>");
        EventInstance cancelled = orchestrationService.cancelEvent(arguments[1]);
        send(sender, "command.spigot.event-cancelled", java.util.Map.of("id", cancelled.id()));
        yield true;
      }
      case "trigger" -> {
        require(sender, "spectraevents.event.trigger");
        requireArguments(arguments, 3, "/spectraevents event trigger <instance> <trigger>");
        EventInstanceId id = new EventInstanceId(UUID.fromString(arguments[1]));
        ExecutionContext context =
            sender instanceof Player player
                ? ExecutionContext.withActor(player, player.getUniqueId())
                : new ExecutionContext(sender, java.util.Map.of());
        boolean handled =
            application
                .executionEngine()
                .evaluateTrigger(id, new ConfiguredTriggerDefinition(arguments[2]), context);
        send(
            sender,
            "command.spigot.trigger-result",
            java.util.Map.of("trigger", arguments[2], "handled", handled));
        yield true;
      }
      case "inspect" -> {
        require(sender, "spectraevents.event.inspect");
        requireArguments(arguments, 2, "/spectraevents event inspect <instance>");
        EventInstanceId id = new EventInstanceId(UUID.fromString(arguments[1]));
        EventInstance instance = orchestrationService.getEventInfo(id.toString());
        var diagnostics = application.executionEngine().diagnostics(id);
        send(
            sender,
            "command.spigot.inspect",
            java.util.Map.of(
                "id", id,
                "state", instance.state(),
                "runtime", diagnostics.runtimeStatePresent(),
                "tasks", diagnostics.pendingTasks(),
                "resources", diagnostics.platformResources(),
                "claim", diagnostics.claimant() != null ? diagnostics.claimant() : "unclaimed"));
        if ((!instance.state().isTerminal() && !diagnostics.runtimeStatePresent())
            || (instance.state().isTerminal()
                && (diagnostics.pendingTasks() != 0 || diagnostics.platformResources() != 0))) {
          send(sender, "command.spigot.inspect-recovery-guidance");
        } else if (diagnostics.claimant() != null) {
          send(sender, "command.spigot.inspect-claim-guidance");
        }
        yield true;
      }
      default -> throw new IllegalArgumentException("Unknown event subcommand: " + arguments[0]);
    };
  }

  private String messageFor(Throwable failure) {
    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }

  private boolean definition(CommandSender sender, String[] arguments) {
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      require(sender, "spectraevents.definition.list");
      send(
          sender,
          "command.spigot.definitions-title",
          java.util.Map.of("count", application.definitionRegistry().getAll().size()));
      for (var registered : application.definitionRegistry().getAll()) {
        send(
            sender,
            "command.spigot.definition-entry",
            java.util.Map.of("definition", registered.definition().id().value()));
      }
      return true;
    }
    if ("reload".equalsIgnoreCase(arguments[0]) || "validate".equalsIgnoreCase(arguments[0])) {
      try {
        boolean validateOnly = "validate".equalsIgnoreCase(arguments[0]);
        require(
            sender,
            validateOnly ? "spectraevents.definition.validate" : "spectraevents.definition.reload");
        if (validateOnly) {
          application.assetPipelineService().buildAssets();
        }
        DefinitionLoadResult result =
            validateOnly ? definitions.validateFromDisk() : definitions.reloadFromDisk();
        var referenceDiagnostics =
            validateOnly
                ? new io.github.kizio806.spectraevents.application.validation
                        .EventReferenceValidator(
                        application.modelDefinitionRegistry(),
                        application.animationDefinitionRegistry())
                    .validate(result)
                : List
                    .<io.github.kizio806.spectraevents.application.config.validation
                            .ValidationDiagnostic>
                        of();
        List<ValidationDiagnostic> readinessDiagnostics = readinessDiagnostics(validateOnly);
        List<ValidationDiagnostic> diagnostics = new ArrayList<>(referenceDiagnostics);
        diagnostics.addAll(readinessDiagnostics);
        long diagnosticErrors = countErrors(diagnostics);
        definitions.logLoadResult(result);
        send(
            sender,
            validateOnly
                ? "command.spigot.definitions-validated"
                : "command.spigot.definitions-reloaded",
            java.util.Map.of(
                "loaded",
                result.loaded().size(),
                "failed",
                result.failures().size() + diagnosticErrors));
        sendDefinitionFailures(sender, result);
        sendDiagnostics(sender, diagnostics);
      } catch (java.io.IOException exception) {
        throw new IllegalStateException(
            "Cannot read definitions: " + exception.getMessage(), exception);
      }
      return true;
    }
    throw new IllegalArgumentException("Unknown definition subcommand: " + arguments[0]);
  }

  private List<ValidationDiagnostic> readinessDiagnostics(boolean validateOnly) throws IOException {
    if (!validateOnly) {
      return List.of();
    }
    return new OperationalReadinessValidator()
        .validateResourcePack(
            SpigotResourcePackDeliveryBootstrap.readSettings(plugin.getDataFolder().toPath()));
  }

  private static long countErrors(List<ValidationDiagnostic> diagnostics) {
    return diagnostics.stream()
        .filter(diagnostic -> diagnostic.severity() == ValidationDiagnostic.Severity.ERROR)
        .count();
  }

  private void sendDefinitionFailures(CommandSender sender, DefinitionLoadResult result) {
    for (var failure : result.failures()) {
      String formattedDiagnostics = definitions.formatDiagnostics(failure.diagnostics());
      send(
          sender,
          "command.spigot.diagnostic-entry",
          java.util.Map.of("path", failure.sourceFile(), "message", formattedDiagnostics));
    }
  }

  private void sendDiagnostics(CommandSender sender, List<ValidationDiagnostic> diagnostics) {
    for (ValidationDiagnostic diagnostic : diagnostics) {
      send(
          sender,
          "command.spigot.diagnostic-entry",
          java.util.Map.of("path", diagnostic.path(), "message", diagnostic.message()));
    }
  }

  private boolean template(CommandSender sender, String[] arguments) {
    require(sender, "spectraevents.template");
    if (arguments.length == 0 || "list".equalsIgnoreCase(arguments[0])) {
      sender.sendMessage("Bundled templates: metin");
      return true;
    }
    if (arguments.length != 2 || !"install".equalsIgnoreCase(arguments[0])) {
      throw new IllegalArgumentException("Usage: /spectraevents template <list|install metin>");
    }
    if (!"metin".equalsIgnoreCase(arguments[1])) {
      throw new IllegalArgumentException("Unknown bundled template: " + arguments[1]);
    }
    try {
      Path archive =
          plugin.getDataFolder().toPath().resolve("templates").resolve("metin.spectra.zip");
      new io.github.kizio806.spectraevents.application.template.SpectraBundleInstaller(
              plugin.getDataFolder().toPath(), plugin.getDescription().getVersion())
          .install(archive);
      application.assetPipelineService().buildAssets();
      DefinitionLoadResult result = definitions.reloadFromDisk();
      definitions.logLoadResult(result);
      if (!result.failures().isEmpty()) {
        throw new IllegalStateException(
            "Installed template did not validate: " + result.failures());
      }
      sender.sendMessage("Template metin is installed and validated.");
    } catch (IOException exception) {
      throw new IllegalStateException("Could not install template metin", exception);
    }
    return true;
  }

  private boolean status(CommandSender sender) {
    require(sender, "spectraevents.status");
    sendStatus(sender);
    return true;
  }

  private void sendStatus(CommandSender sender) {
    long running =
        repository.findAll().stream()
            .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
            .count();
    send(
        sender,
        "command.spigot.status",
        java.util.Map.of(
            "definitions", application.definitionRegistry().getAll().size(), "running", running));
  }

  private boolean doctor(CommandSender sender) {
    require(sender, "spectraevents.doctor");
    sendStatus(sender);
    for (EventInstance instance : repository.findAll()) {
      if (instance.state() == EventLifecycleState.RUNNING) {
        String claim =
            repository
                .findState(instance.id())
                .map(state -> state.claimant())
                .filter(c -> c != null)
                .orElse("unclaimed");
        send(
            sender,
            "command.spigot.doctor-instance",
            java.util.Map.of("id", instance.id(), "state", instance.state(), "claim", claim));
      }
    }
    send(sender, "command.spigot.reward-guarantee");
    send(sender, "command.spigot.recovery-guidance");
    return true;
  }

  private boolean version(CommandSender sender) {
    send(
        sender,
        "command.spigot.version",
        java.util.Map.of("version", plugin.getDescription().getVersion()));
    return true;
  }

  private boolean unavailableAssets(CommandSender sender) {
    send(sender, "command.spigot.assets-unavailable");
    return true;
  }

  private boolean unsupported(CommandSender sender, String feature, String supportedPlatform) {
    send(
        sender,
        "command.spigot.unsupported",
        java.util.Map.of("feature", feature, "platform", supportedPlatform));
    return true;
  }

  private void help(CommandSender sender) {
    send(sender, "command.spigot.help-general");
    send(sender, "command.spigot.help-events");
    send(sender, "command.spigot.help-rewards");
    send(sender, "command.spigot.help-definitions");
    send(sender, "command.spigot.help-schedules");
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

  private void send(CommandSender sender, String key) {
    sender.sendMessage(locales.message(key));
  }

  private void send(CommandSender sender, String key, java.util.Map<String, ?> placeholders) {
    sender.sendMessage(locales.message(key, placeholders));
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
          arguments[0],
          "help",
          "version",
          "status",
          "doctor",
          "event",
          "definition",
          "validate",
          "template",
          "schedule",
          "rewards",
          "assets");
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
    if (arguments.length == 2 && "template".equalsIgnoreCase(arguments[0])) {
      return matches(arguments[1], "list", "install");
    }
    if (arguments.length == 3
        && "template".equalsIgnoreCase(arguments[0])
        && "install".equalsIgnoreCase(arguments[1])) {
      return matches(arguments[2], "metin");
    }
    if (arguments.length == 2 && "rewards".equalsIgnoreCase(arguments[0])) {
      return matches(arguments[1], "list", "claim");
    }
    if (arguments.length == 2 && "schedule".equalsIgnoreCase(arguments[0])) {
      return matches(arguments[1], "list", "reload");
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
