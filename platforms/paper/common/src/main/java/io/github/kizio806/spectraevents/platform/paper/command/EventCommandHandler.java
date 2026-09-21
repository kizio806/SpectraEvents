package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
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

  public EventCommandHandler(
      EventOrchestrationService orchestrationService,
      EventInstanceRepository instanceRepository,
      EventDefinitionRegistry definitionRegistry) {
    this.orchestrationService = orchestrationService;
    this.instanceRepository = instanceRepository;
    this.definitionRegistry = definitionRegistry;
  }

  public EventCommandHandler(
      EventOrchestrationService orchestrationService, EventInstanceRepository instanceRepository) {
    this(orchestrationService, instanceRepository, null);
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("event")
        .then(
            Commands.literal("list")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.list"))
                .executes(this::eventList))
        .then(
            Commands.literal("start")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.start"))
                .then(
                    Commands.argument("definition", StringArgumentType.word())
                        .suggests(
                            (ctx, builder) -> {
                              String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                              if (definitionRegistry != null) {
                                for (var def : definitionRegistry.getAll()) {
                                  String id = def.definition().id().value();
                                  if (id.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                                    builder.suggest(id);
                                  }
                                }
                              }
                              return builder.buildFuture();
                            })
                        .executes(this::eventStart)))
        .then(
            Commands.literal("stop")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.stop"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .suggests(this::suggestActiveInstances)
                        .executes(this::eventStop)))
        .then(
            Commands.literal("cancel")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.cancel"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .suggests(this::suggestActiveInstances)
                        .executes(this::eventCancel)))
        .then(
            Commands.literal("trigger")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.trigger"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .suggests(this::suggestActiveInstances)
                        .then(
                            Commands.argument("trigger", StringArgumentType.word())
                                .executes(this::eventTrigger))))
        .then(
            Commands.literal("inspect")
                .requires(s -> s.getSender().hasPermission("spectraevents.event.inspect"))
                .then(
                    Commands.argument("instance", StringArgumentType.word())
                        .executes(this::eventInspect)));
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
      if (sender instanceof Player player) {
        bukkitLocation = player.getLocation();
      } else if (!Bukkit.getWorlds().isEmpty()) {
        bukkitLocation = Bukkit.getWorlds().getFirst().getSpawnLocation();
      }
      if (bukkitLocation == null || bukkitLocation.getWorld() == null) {
        throw new IllegalStateException("No loaded world is available for the event location");
      }
      EventLocation platformLocation =
          new EventLocation(
              bukkitLocation.getWorld().getName(),
              bukkitLocation.getX(),
              bukkitLocation.getY(),
              bukkitLocation.getZ(),
              bukkitLocation.getYaw(),
              bukkitLocation.getPitch());
      var instance = orchestrationService.startDefinition(defIdStr, platformLocation);
      sender.sendMessage(
          Component.text(
              "Started event instance "
                  + instance.id().toString()
                  + " from definition '"
                  + defIdStr
                  + "'",
              NamedTextColor.GREEN));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Failed to start event: " + e.getMessage(), NamedTextColor.RED));
    }
    return 1;
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
    EventInstanceId instanceId =
        new EventInstanceId(UUID.fromString(StringArgumentType.getString(ctx, "instance")));
    String trigger = StringArgumentType.getString(ctx, "trigger");
    ExecutionContext context =
        sender instanceof Player
            ? ExecutionContext.withActor(sender)
            : new ExecutionContext(sender, Map.of());
    boolean handled =
        orchestrationService
            .executionEngine()
            .evaluateTrigger(instanceId, new ConfiguredTriggerDefinition(trigger), context);
    sender.sendMessage(
        Component.text("Trigger " + trigger + " handled=" + handled, NamedTextColor.YELLOW));
    return handled ? 1 : 0;
  }

  private int eventInspect(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    EventInstanceId instanceId =
        new EventInstanceId(UUID.fromString(StringArgumentType.getString(ctx, "instance")));
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
                + diagnostics.claimant().orElse("unclaimed"),
            NamedTextColor.YELLOW));
    if (!diagnostics.runtimeStatePresent()
        || (instance.state().isTerminal()
            && (diagnostics.pendingTasks() != 0 || diagnostics.platformResources() != 0))) {
      sender.sendMessage(
          Component.text(
              "Recovery guidance: run /event doctor, preserve logs, and back up spectraevents.db before restarting.",
              NamedTextColor.RED));
    } else if (diagnostics.claimant().isPresent()) {
      sender.sendMessage(
          Component.text(
              "Claim recorded: reconcile any external reward before granting a manual replacement.",
              NamedTextColor.GOLD));
    }
    return 1;
  }
}
