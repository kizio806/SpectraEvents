package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.compiler.EventDefinitionCompiler;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
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

  public EventCommandHandler(
      EventOrchestrationService orchestrationService,
      EventInstanceRepository instanceRepository,
      EventDefinitionRegistry definitionRegistry,
      PaperEventSettingsStore settingsStore) {
    this.orchestrationService = orchestrationService;
    this.instanceRepository = instanceRepository;
    this.definitionRegistry = definitionRegistry;
    this.settingsStore = settingsStore;
  }

  public EventCommandHandler(
      EventOrchestrationService orchestrationService, EventInstanceRepository instanceRepository) {
    this(orchestrationService, instanceRepository, null, null);
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
            Commands.literal("stop")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.stop"))
                .executes(ctx -> missingArgument(ctx, "/spectraevents event stop <instance-id>"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .suggests(this::suggestActiveInstances)
                        .executes(this::eventStop)))
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
    sender.sendMessage(Component.text("Event commands:", NamedTextColor.AQUA));
    sender.sendMessage(
        Component.text("/spectraevents event start <event-id>", NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text(
            "/spectraevents event list | inspect <instance-id> | cancel <instance-id>",
            NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text("/spectraevents event config <event-id> show", NamedTextColor.GRAY));
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
            Component.text("A required value is missing. Example: " + example, NamedTextColor.RED));
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
        Component.text("Active Event Instances (" + instances.size() + "):", NamedTextColor.GOLD));
    for (EventInstance inst : instances) {
      sender.sendMessage(
          Component.text(" - ID: ", NamedTextColor.GRAY)
              .append(Component.text(inst.id().toString(), NamedTextColor.AQUA))
              .append(Component.text(" Definition: ", NamedTextColor.GRAY))
              .append(Component.text(inst.definitionId().value(), NamedTextColor.YELLOW))
              .append(Component.text(" State: ", NamedTextColor.GRAY))
              .append(Component.text(inst.state().name(), NamedTextColor.GREEN)));
    }
    return 1;
  }

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
      var instance =
          orchestrationService
              .executionEngine()
              .startEvent(
                  new EventDefinitionCompiler()
                      .compile(
                          java.util.Objects.requireNonNull(
                              registered.sourceSpec(), "Definition source is unavailable"),
                          settings),
                  platformLocation);
      sender.sendMessage(
          Component.text(
              "Started "
                  + defIdStr
                  + " at "
                  + platformLocation.world()
                  + ". Instance: "
                  + instance.id(),
              NamedTextColor.GREEN));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Failed to start event: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }

  private int showConfiguration(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String definition = StringArgumentType.getString(ctx, "definition");
    try {
      requireSettingsStore();
      sender.sendMessage(
          Component.text(
              definition + " overrides=" + settingsStore.overridesFor(definition),
              NamedTextColor.AQUA));
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          Component.text("Configuration error: " + exception.getMessage(), NamedTextColor.RED));
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
          Component.text("Saved " + parameter + " for " + definition + ".", NamedTextColor.GREEN));
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          Component.text("Setting was not saved: " + exception.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }

  private void requireSettingsStore() {
    if (settingsStore == null) {
      throw new IllegalStateException("Event settings are unavailable.");
    }
  }

  private int eventStop(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String instIdStr = StringArgumentType.getString(ctx, "instance");

    try {
      orchestrationService.cancelEvent(instIdStr);
      sender.sendMessage(
          Component.text("Stopped event instance " + instIdStr, NamedTextColor.GREEN));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Failed to stop event: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
  }

  private int eventCancel(CommandContext<CommandSourceStack> ctx) {
    return eventStop(ctx);
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
          Component.text("Trigger " + trigger + " handled=" + handled, NamedTextColor.YELLOW));
      return handled ? 1 : 0;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          Component.text("Trigger was not sent: " + exception.getMessage(), NamedTextColor.RED));
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
          Component.text(
              "Event "
                  + instanceId
                  + " state="
                  + instance.state()
                  + " runtimeState="
                  + diagnostics.runtimeStatePresent()
                  + " tasks="
                  + diagnostics.pendingTasks()
                  + " resources="
                  + diagnostics.platformResources()
                  + " claim="
                  + (diagnostics.claimant() != null ? diagnostics.claimant() : "unclaimed"),
              NamedTextColor.YELLOW));
      if (!diagnostics.runtimeStatePresent()
          || (instance.state().isTerminal()
              && (diagnostics.pendingTasks() != 0 || diagnostics.platformResources() != 0))) {
        sender.sendMessage(
            Component.text(
                "Recovery guidance: run /spectraevents doctor, preserve logs, and back up spectraevents.db before restarting.",
                NamedTextColor.RED));
      } else if (diagnostics.claimant() != null) {
        sender.sendMessage(
            Component.text(
                "Claim recorded: reconcile any external reward before granting a manual replacement.",
                NamedTextColor.GOLD));
      }
      return 1;
    } catch (IllegalArgumentException | IllegalStateException exception) {
      sender.sendMessage(
          Component.text("Could not inspect event: " + exception.getMessage(), NamedTextColor.RED));
      return 0;
    }
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
