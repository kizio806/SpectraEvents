package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.template.SpectraBundleInstaller;
import io.github.kizio806.spectraevents.platform.paper.config.PaperDefinitionConfigBootstrap;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/** Installs bundled event templates without requiring filesystem copy steps from an operator. */
final class TemplateCommandHandler {
  private static final List<String> BUNDLED_TEMPLATES = List.of("metin");

  private final Plugin plugin;
  private final SpectraEventsApplication application;
  private final PaperDefinitionConfigBootstrap definitions;
  private final RegionTaskScheduler scheduler;

  TemplateCommandHandler(
      Plugin plugin,
      SpectraEventsApplication application,
      PaperDefinitionConfigBootstrap definitions,
      RegionTaskScheduler scheduler) {
    this.plugin = plugin;
    this.application = application;
    this.definitions = definitions;
    this.scheduler = scheduler;
  }

  LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("template")
        .requires(source -> allowed(source.getSender()))
        .then(Commands.literal("list").executes(this::list))
        .then(
            Commands.literal("install")
                .then(
                    Commands.argument("id", StringArgumentType.word())
                        .suggests(
                            (context, builder) -> {
                              BUNDLED_TEMPLATES.forEach(builder::suggest);
                              return builder.buildFuture();
                            })
                        .executes(this::install)));
  }

  private int list(CommandContext<CommandSourceStack> context) {
    context
        .getSource()
        .getSender()
        .sendMessage(Component.text("Bundled templates: metin", NamedTextColor.AQUA));
    return 1;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private int install(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();
    String id = StringArgumentType.getString(context, "id");
    if (!BUNDLED_TEMPLATES.contains(id)) {
      sender.sendMessage(Component.text("Unknown bundled template: " + id, NamedTextColor.RED));
      return 0;
    }
    sender.sendMessage(Component.text("Installing template " + id + "…", NamedTextColor.YELLOW));
    CompletableFuture.runAsync(
            () -> {
              try {
                Path archive =
                    plugin
                        .getDataFolder()
                        .toPath()
                        .resolve("templates")
                        .resolve(id + ".spectra.zip");
                new SpectraBundleInstaller(
                        plugin.getDataFolder().toPath(), plugin.getPluginMeta().getVersion())
                    .install(archive);
                application.assetPipelineService().buildAssets();
                var result = definitions.reloadFromDisk();
                if (!result.failures().isEmpty()) {
                  throw new IllegalStateException("Definition reload failed: " + result.failures());
                }
              } catch (Exception exception) {
                throw new IllegalStateException("Could not install template " + id, exception);
              }
            })
        .whenComplete(
            (ignored, failure) ->
                scheduler.executeGlobal(
                    () ->
                        sender.sendMessage(
                            failure == null
                                ? Component.text(
                                    "Template " + id + " is installed and validated.",
                                    NamedTextColor.GREEN)
                                : Component.text(
                                    "Template installation failed: " + message(failure),
                                    NamedTextColor.RED))));
    return 1;
  }

  private static boolean allowed(CommandSender sender) {
    return sender.hasPermission("spectraevents.template")
        || sender.hasPermission("spectraevents.admin")
        || sender.isOp();
  }

  private static String message(Throwable failure) {
    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }
}
