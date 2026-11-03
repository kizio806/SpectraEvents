package io.github.kizio806.spectraevents.platform.spigot.command;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import io.github.kizio806.spectraevents.platform.bukkit.util.BukkitPluginVersions;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Handles `/spectraevents status`, `/spectraevents doctor`, and `/spectraevents version` on Spigot.
 */
public final class SpigotDiagnosticsCommandHandler implements SpigotSubcommandHandler {

  private final Plugin plugin;
  private final SpectraEventsApplication application;
  private final EventInstanceRepository repository;
  private final SpigotCommandHelper helper;

  public SpigotDiagnosticsCommandHandler(
      Plugin plugin,
      SpectraEventsApplication application,
      EventInstanceRepository repository,
      SpigotCommandHelper helper) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.application = Objects.requireNonNull(application, "application");
    this.repository = Objects.requireNonNull(repository, "repository");
    this.helper = Objects.requireNonNull(helper, "helper");
  }

  @Override
  public void execute(CommandSender sender, String[] arguments) {
    if (arguments.length == 0) {
      status(sender);
      return;
    }
    switch (arguments[0].toLowerCase(java.util.Locale.ROOT)) {
      case "status" -> status(sender);
      case "doctor" -> doctor(sender);
      case "version" -> version(sender);
      default ->
          throw new IllegalArgumentException("Unknown diagnostics subcommand: " + arguments[0]);
    }
  }

  public void status(CommandSender sender) {
    helper.require(sender, "spectraevents.status");
    sendStatus(sender);
  }

  public void doctor(CommandSender sender) {
    helper.require(sender, "spectraevents.doctor");
    sendStatus(sender);
    for (EventInstance instance : repository.findAll()) {
      if (instance.state() == EventLifecycleState.RUNNING) {
        String claim =
            repository
                .findState(instance.id())
                .map(state -> state.claimant())
                .filter(Objects::nonNull)
                .orElse("unclaimed");
        helper.send(
            sender,
            "command.spigot.doctor-instance",
            Map.of("id", instance.id(), "state", instance.state(), "claim", claim));
      }
    }
    helper.send(sender, "command.spigot.reward-guarantee");
    helper.send(sender, "command.spigot.recovery-guidance");
  }

  public void version(CommandSender sender) {
    String version = BukkitPluginVersions.resolveVersion(plugin);
    helper.send(sender, "command.spigot.version", Map.of("version", version));
  }

  private void sendStatus(CommandSender sender) {
    long running =
        repository.findAll().stream()
            .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
            .count();
    helper.send(
        sender,
        "command.spigot.status",
        Map.of(
            "definitions", application.definitionRegistry().getAll().size(), "running", running));
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String[] arguments) {
    if (sender == null || !sender.hasPermission("spectraevents.status")) {
      return List.of();
    }
    return List.of();
  }
}
