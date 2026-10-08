package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command handler for event management subcommands. */
public final class EventCommandHandler {
  private final EventOrchestrationService orchestrationService;
  private final EventInstanceRepository instanceRepository;
  private final EventDefinitionRegistry definitionRegistry;
  private final PaperEventSettingsStore settingsStore;
  private final RegionTaskScheduler regionScheduler;
  private final CommandText messages;

  public EventCommandHandler(
      EventOrchestrationService orchestrationService,
      EventInstanceRepository instanceRepository,
      EventDefinitionRegistry definitionRegistry,
      PaperEventSettingsStore settingsStore,
      RegionTaskScheduler regionScheduler,
      LocaleCatalog locales) {
    this.orchestrationService = orchestrationService;
    this.instanceRepository = instanceRepository;
    this.definitionRegistry = definitionRegistry;
    this.settingsStore = settingsStore;
    this.regionScheduler = regionScheduler;
    this.messages = new CommandText(locales);
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("event")
        .executes(this::eventUsage)
        .then(
            Commands.literal("list")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.list"))
                .executes(this::eventList))
        .then(
            Commands.literal("start")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.start"))
                .executes(ctx -> missingArgument(ctx, "/spectraevents event start <event-id>"))
                .then(startDefinitionArgument()))
        .then(
            Commands.literal("config")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.start"))
                .executes(
                    ctx -> missingArgument(ctx, "/spectraevents event config <event-id> show"))
                .then(
                    Commands.argument("definition", StringArgumentType.word())
                        .suggests(this::suggestDefinitions)
                        .executes(
                            ctx ->
                                missingArgument(ctx, "/spectraevents event config <event-id> show"))
                        .then(Commands.literal("show").executes(this::showConfiguration))
                        .then(
                            Commands.literal("set")
                                .then(
                                    Commands.argument("parameter", StringArgumentType.word())
                                        .suggests(this::suggestParameters)
                                        .then(
                                            Commands.argument("value", StringArgumentType.word())
                                                .executes(this::setConfiguration))))))
        .then(
            Commands.literal("cancel")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.cancel"))
                .executes(ctx -> missingArgument(ctx, "/spectraevents event cancel <instance-id>"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .suggests(this::suggestActiveInstances)
                        .executes(this::eventCancel)))
        .then(
            Commands.literal("trigger")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.trigger"))
                .executes(
                    ctx ->
                        missingArgument(
                            ctx, "/spectraevents event trigger <instance-id> <trigger>"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .suggests(this::suggestActiveInstances)
                        .then(
                            Commands.argument("trigger", StringArgumentType.word())
                                .executes(this::eventTrigger))))
        .then(
            Commands.literal("inspect")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.inspect"))
                .executes(ctx -> missingArgument(ctx, "/spectraevents event inspect <instance-id>"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .suggests(this::suggestActiveInstances)
                        .executes(this::eventInspect)));
  }

  private int eventUsage(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    sender.sendMessage(messages.component("command.events.usage-title", NamedTextColor.AQUA));
    sender.sendMessage(messages.component("command.events.usage-start", NamedTextColor.GRAY));
    sender.sendMessage(messages.component("command.events.usage-manage", NamedTextColor.GRAY));
    sender.sendMessage(messages.component("command.events.usage-config", NamedTextColor.GRAY));
    return 1;
  }

  private RequiredArgumentBuilder<CommandSourceStack, String> startDefinitionArgument() {
    return Commands.argument("definition", StringArgumentType.word())
        .suggests(this::suggestDefinitions)
        .executes(this::eventStart)
        .then(Commands.literal("location").then(locationArgument()));
  }

  private RequiredArgumentBuilder<CommandSourceStack, String> locationArgument() {
    return Commands.argument("location", StringArgumentType.word())
        .suggests(this::suggestLocations)
        .executes(this::eventStart);
  }

  private int missingArgument(CommandContext<CommandSourceStack> ctx, String example) {
    ctx.getSource()
        .getSender()
        .sendMessage(
            messages.component(
                "command.events.value-required", NamedTextColor.RED, Map.of("example", example)));
    return 0;
  }

  private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
      suggestActiveInstances(
          CommandContext<CommandSourceStack> ctx,
          com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
    if (instanceRepository != null) {
      for (EventInstance inst : instanceRepository.findAll()) {
        String idStr = inst.id().toString();
        String defStr = inst.definitionId().value();
        if (idStr.toLowerCase(Locale.ROOT).startsWith(remaining)) {
          builder.suggest(idStr);
        }
        if (defStr.toLowerCase(Locale.ROOT).startsWith(remaining)) {
          builder.suggest(defStr);
        }
      }
    }
    return builder.buildFuture();
  }

  private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
      suggestDefinitions(
          CommandContext<CommandSourceStack> ctx,
          com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
    if (definitionRegistry != null) {
      String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
      definitionRegistry.getAll().stream()
          .map(registered -> registered.definition().id().value())
          .filter(id -> id.startsWith(remaining))
          .forEach(builder::suggest);
    }
    return builder.buildFuture();
  }

  private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
      suggestLocations(
          CommandContext<CommandSourceStack> ctx,
          com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
    if (settingsStore != null) {
      String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
      settingsStore.locations().keySet().stream()
          .filter(name -> name.startsWith(remaining))
          .forEach(builder::suggest);
    }
    return builder.buildFuture();
  }

  private java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
      suggestParameters(
          CommandContext<CommandSourceStack> ctx,
          com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
    if (definitionRegistry != null) {
      String id = StringArgumentType.getString(ctx, "definition");
      definitionRegistry
          .findById(id)
          .map(registered -> registered.sourceSpec())
          .ifPresent(
              spec ->
                  spec.parameters().values().stream()
                      .filter(parameter -> parameter.guiEditable())
                      .map(parameter -> parameter.name())
                      .forEach(builder::suggest));
    }
    return builder.buildFuture();
  }

  private int eventList(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    Collection<EventInstance> instances = instanceRepository.findAll();
    sender.sendMessage(
        messages.component(
            "command.events.list-title", NamedTextColor.GOLD, Map.of("count", instances.size())));
    for (EventInstance inst : instances) {
      sender.sendMessage(
          messages.component(
              "command.events.list-entry",
              NamedTextColor.GRAY,
              Map.of(
                  "id", inst.id(),
                  "definition", inst.definitionId().value(),
                  "state", inst.state().name())));
    }
    return 1;
  }

  @SuppressWarnings(
      "FutureReturnValueIgnored") // Completion sends the platform-thread command reply.
  private int eventStart(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String defIdStr = StringArgumentType.getString(ctx, "definition");

    try {
      Location bukkitLocation = null;
      EventLocation namedLocation = null;
      if (ctx.getNodes().stream().anyMatch(node -> "location".equals(node.getNode().getName()))) {
        if (settingsStore == null) {
          throw new IllegalStateException("Named locations are unavailable.");
        }
        String name = StringArgumentType.getString(ctx, "location");
        namedLocation =
            settingsStore
                .location(name)
                .orElseThrow(
                    () -> new IllegalArgumentException("Location '" + name + "' does not exist."));
      }
      if (sender instanceof Player player) {
        bukkitLocation = player.getLocation();
      } else if (!Bukkit.getWorlds().isEmpty()) {
        bukkitLocation = Bukkit.getWorlds().getFirst().getSpawnLocation();
      }
      if (bukkitLocation == null || bukkitLocation.getWorld() == null) {
        throw new IllegalStateException("No loaded world is available for the event location");
      }
      EventLocation platformLocation =
          namedLocation == null
              ? new EventLocation(
                  bukkitLocation.getWorld().getName(),
                  bukkitLocation.getX(),
                  bukkitLocation.getY(),
                  bukkitLocation.getZ(),
                  bukkitLocation.getYaw(),
                  bukkitLocation.getPitch())
              : namedLocation;
      var registered =
          definitionRegistry
              .findById(defIdStr)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException("Event '" + defIdStr + "' is not registered."));
      Map<String, Object> settings =
          settingsStore == null ? Map.of() : settingsStore.overridesFor(defIdStr);
      orchestrationService
          .executionEngine()
          .startEventAsync(
              new EventDefinitionCompiler()
                  .compile(
                      java.util.Objects.requireNonNull(
                          registered.sourceSpec(), "Definition source is unavailable"),
                      settings),
              platformLocation)
          .whenComplete(
              (instance, failure) ->
                  replyLater(
                      sender,
                      failure == null
                          ? messages.component(
                              "command.events.started",
                              NamedTextColor.GREEN,
                              Map.of(
                                  "event",
                                  defIdStr,
                                  "world",
                                  platformLocation.world(),
                                  "id",
                                  instance.id()))
                          : messages.component(
                              "command.events.start-failed",
                              NamedTextColor.RED,
                              Map.of(
                                  "reason",
                                  safeMessage(
                                      failure.getCause() == null
                                          ? failure
                                          : failure.getCause())))));
    } catch (Exception e) {
      sender.sendMessage(
          messages.component(
              "command.events.start-failed", NamedTextColor.RED, Map.of("reason", e.getMessage())));
    }
    return 1;
  }

  private String safeMessage(Throwable throwable) {
    return throwable.getMessage() == null
        ? throwable.getClass().getSimpleName()
        : throwable.getMessage();
  }

  private void replyLater(CommandSender sender, Component message) {
    if (sender instanceof Player player) {
      regionScheduler.executeFor(player, () -> player.sendMessage(message));
    } else {
      regionScheduler.executeGlobal(() -> sender.sendMessage(message));
    }
  }

  private int showConfiguration(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String definition = StringArgumentType.getString(ctx, "definition");
    try {
      requireSettingsStore();
      sender.sendMessage(
          messages.component(
              "command.events.overrides",
              NamedTextColor.AQUA,
              Map.of("event", definition, "values", settingsStore.overridesFor(definition))));
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          messages.component(
              "command.events.configuration-error",
              NamedTextColor.RED,
              Map.of("reason", exception.getMessage())));
    }
    return 1;
  }

  private int setConfiguration(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    try {
      requireSettingsStore();
      String definition = StringArgumentType.getString(ctx, "definition");
      String parameter = StringArgumentType.getString(ctx, "parameter");
      String value = StringArgumentType.getString(ctx, "value");
      var declaration =
          definitionRegistry
              .findById(definition)
              .orElseThrow(() -> new IllegalArgumentException("Event is not registered."))
              .sourceSpec()
              .parameters()
              .get(parameter);
      if (declaration == null || !declaration.guiEditable()) {
        throw new IllegalArgumentException(
            "Setting is not declared as GUI/command editable in YAML.");
      }
      new EventDefinitionCompiler()
          .compile(
              definitionRegistry.findById(definition).orElseThrow().sourceSpec(),
              Map.of(parameter, value));
      settingsStore.setParameter(definition, parameter, value);
      sender.sendMessage(
          messages.component(
              "command.events.setting-saved",
              NamedTextColor.GREEN,
              Map.of("parameter", parameter, "event", definition)));
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          messages.component(
              "command.events.setting-not-saved",
              NamedTextColor.RED,
              Map.of("reason", exception.getMessage())));
    }
    return 1;
  }

  private void requireSettingsStore() {
    if (settingsStore == null) {
      throw new IllegalStateException("Event settings are unavailable.");
    }
  }

  private int eventCancel(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String instIdStr = StringArgumentType.getString(ctx, "instance");

    try {
      orchestrationService.cancelEvent(instIdStr);
      sender.sendMessage(
          messages.component(
              "command.events.cancelled", NamedTextColor.GREEN, Map.of("id", instIdStr)));
    } catch (Exception e) {
      sender.sendMessage(
          messages.component(
              "command.events.cancel-failed",
              NamedTextColor.RED,
              Map.of("reason", e.getMessage())));
    }
    return 1;
  }

  private int eventTrigger(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    try {
      EventInstanceId instanceId = instanceId(ctx);
      String trigger = StringArgumentType.getString(ctx, "trigger");
      ExecutionContext context =
          sender instanceof Player player
              ? ExecutionContext.withActor(player, player.getUniqueId())
              : new ExecutionContext(sender, Map.of());
      boolean handled =
          orchestrationService
              .executionEngine()
              .evaluateTrigger(instanceId, new ConfiguredTriggerDefinition(trigger), context);
      sender.sendMessage(
          messages.component(
              "command.events.trigger-result",
              NamedTextColor.YELLOW,
              Map.of("trigger", trigger, "handled", handled)));
      return handled ? 1 : 0;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          messages.component(
              "command.events.trigger-failed",
              NamedTextColor.RED,
              Map.of("reason", exception.getMessage())));
      return 0;
    }
  }

  private int eventInspect(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    try {
      EventInstanceId instanceId = instanceId(ctx);
      EventInstance instance = orchestrationService.getEventInfo(instanceId.toString());
      var diagnostics = orchestrationService.executionEngine().diagnostics(instanceId);
      sender.sendMessage(
          messages.component(
              "command.events.inspect",
              NamedTextColor.YELLOW,
              Map.of(
                  "id",
                  instanceId,
                  "state",
                  instance.state(),
                  "runtime",
                  diagnostics.runtimeStatePresent(),
                  "tasks",
                  diagnostics.pendingTasks(),
                  "resources",
                  diagnostics.platformResources(),
                  "claim",
                  diagnostics.claimant() != null
                      ? diagnostics.claimant()
                      : messages.message("command.events.unclaimed"))));
      if (needsRecoveryGuidance(
          instance.state().isTerminal(),
          diagnostics.runtimeStatePresent(),
          diagnostics.pendingTasks(),
          diagnostics.platformResources())) {
        sender.sendMessage(
            messages.component("command.events.recovery-guidance", NamedTextColor.RED));
      } else if (diagnostics.claimant() != null) {
        sender.sendMessage(
            messages.component("command.events.claim-guidance", NamedTextColor.GOLD));
      }
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          messages.component(
              "command.events.inspect-failed",
              NamedTextColor.RED,
              Map.of("reason", exception.getMessage())));
      return 0;
    }
  }

  static boolean needsRecoveryGuidance(
      boolean terminal, boolean runtimeStatePresent, int pendingTasks, int platformResources) {
    return (!terminal && !runtimeStatePresent)
        || (terminal && (pendingTasks != 0 || platformResources != 0));
  }

  private EventInstanceId instanceId(CommandContext<CommandSourceStack> ctx) {
    String value = StringArgumentType.getString(ctx, "instance");
    try {
      return new EventInstanceId(UUID.fromString(value));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Instance ID must be a UUID. Example: /spectraevents event inspect <instance-id>");
    }
  }
}
