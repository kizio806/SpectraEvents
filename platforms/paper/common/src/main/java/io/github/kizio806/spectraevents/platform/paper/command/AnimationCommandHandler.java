package io.github.kizio806.spectraevents.platform.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.kizio806.spectraevents.application.model.animation.compiler.AnimationCompiler;
import io.github.kizio806.spectraevents.application.model.animation.registry.AnimationDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.animation.runtime.AnimationPlaybackId;
import io.github.kizio806.spectraevents.application.model.animation.runtime.AnimationPlaybackState;
import io.github.kizio806.spectraevents.application.model.animation.runtime.AnimationRuntimeService;
import io.github.kizio806.spectraevents.application.model.animation.runtime.PlaybackOptions;
import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationDefinition;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationTime;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

/** Brigadier command handler for 3D model animation management (/event animation). */
public final class AnimationCommandHandler {

  private final ModelDefinitionRegistry modelRegistry;
  private final ModelRuntimeService modelRuntimeService;
  private final AnimationDefinitionRegistry animationRegistry;
  private final AnimationRuntimeService animationRuntimeService;
  private final AnimationCompiler animationCompiler = new AnimationCompiler();

  public AnimationCommandHandler(
      ModelDefinitionRegistry modelRegistry,
      ModelRuntimeService modelRuntimeService,
      AnimationDefinitionRegistry animationRegistry,
      AnimationRuntimeService animationRuntimeService) {
    this.modelRegistry = modelRegistry;
    this.modelRuntimeService = modelRuntimeService;
    this.animationRegistry = animationRegistry;
    this.animationRuntimeService = animationRuntimeService;
  }

  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("animation")
        .requires(s -> hasPerm(s, "spectraevents.admin.animation"))
        .then(
            Commands.literal("list")
                .requires(s -> hasPerm(s, "spectraevents.admin.animation.list"))
                .then(
                    Commands.argument("modelId", StringArgumentType.string())
                        .executes(this::listAnimations)))
        .then(
            Commands.literal("play")
                .requires(s -> hasPerm(s, "spectraevents.admin.animation.play"))
                .then(
                    Commands.argument("runtimeId", StringArgumentType.string())
                        .then(
                            Commands.argument("animationId", StringArgumentType.string())
                                .executes(this::playAnimation))))
        .then(
            Commands.literal("pause")
                .requires(s -> hasPerm(s, "spectraevents.admin.animation.pause"))
                .then(
                    Commands.argument("playbackId", StringArgumentType.string())
                        .executes(this::pauseAnimation)))
        .then(
            Commands.literal("resume")
                .requires(s -> hasPerm(s, "spectraevents.admin.animation.resume"))
                .then(
                    Commands.argument("playbackId", StringArgumentType.string())
                        .executes(this::resumeAnimation)))
        .then(
            Commands.literal("stop")
                .requires(s -> hasPerm(s, "spectraevents.admin.animation.stop"))
                .then(
                    Commands.argument("playbackId", StringArgumentType.string())
                        .executes(this::stopAnimation)))
        .then(
            Commands.literal("seek")
                .requires(s -> hasPerm(s, "spectraevents.admin.animation.seek"))
                .then(
                    Commands.argument("playbackId", StringArgumentType.string())
                        .then(
                            Commands.argument("time", StringArgumentType.string())
                                .executes(this::seekAnimation))))
        .then(
            Commands.literal("info")
                .requires(s -> hasPerm(s, "spectraevents.admin.animation.info"))
                .then(
                    Commands.argument("playbackId", StringArgumentType.string())
                        .executes(this::playbackInfo)));
  }

  private boolean hasPerm(CommandSourceStack source, String perm) {
    CommandSender sender = source.getSender();
    return sender.hasPermission(perm)
        || sender.hasPermission("spectraevents.admin")
        || sender.isOp();
  }

  private int listAnimations(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    String modelIdStr = StringArgumentType.getString(ctx, "modelId");
    ModelId modelId = new ModelId(modelIdStr);

    Optional<ModelDefinition> modelOpt = modelRegistry.get(modelId);
    if (modelOpt.isEmpty()) {
      sender.sendMessage(
          Component.text("Model '" + modelIdStr + "' not found.", NamedTextColor.RED));
      return 0;
    }

    ModelDefinition model = modelOpt.get();
    var animations = model.animations();

    sender.sendMessage(
        Component.text("Animations for model ", NamedTextColor.DARK_PURPLE)
            .append(Component.text(modelIdStr, NamedTextColor.GOLD))
            .append(Component.text(" (", NamedTextColor.DARK_PURPLE))
            .append(Component.text(animations.size(), NamedTextColor.LIGHT_PURPLE))
            .append(Component.text(" registered):", NamedTextColor.DARK_PURPLE)));

    for (AnimationDefinition anim : animations.values()) {
      sender.sendMessage(
          Component.text(" - ", NamedTextColor.GRAY)
              .append(Component.text(anim.id().value(), NamedTextColor.YELLOW))
              .append(
                  Component.text(
                      " ["
                          + anim.duration().time().toSeconds()
                          + "s, loop="
                          + anim.loopMode()
                          + ", cues="
                          + anim.cues().size()
                          + "]",
                      NamedTextColor.GRAY)));
    }
    return 1;
  }

  private int playAnimation(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (animationRuntimeService == null || modelRuntimeService == null) {
      sender.sendMessage(
          Component.text("Animation Runtime Service is not available.", NamedTextColor.RED));
      return 0;
    }

    String runtimeIdStr = StringArgumentType.getString(ctx, "runtimeId");
    String animIdStr = StringArgumentType.getString(ctx, "animationId");

    ModelRuntimeId runtimeId = ModelRuntimeId.of(runtimeIdStr);
    Optional<RenderedModelHandle> handleOpt = modelRuntimeService.getHandle(runtimeId);
    if (handleOpt.isEmpty()) {
      sender.sendMessage(
          Component.text(
              "Active model handle '" + runtimeIdStr + "' not found.", NamedTextColor.RED));
      return 0;
    }

    RenderedModelHandle handle = handleOpt.get();
    AnimationId animId = new AnimationId(animIdStr);

    Optional<ModelDefinition> modelOpt = modelRegistry.get(handle.definitionId());
    if (modelOpt.isEmpty()) {
      sender.sendMessage(
          Component.text("Model definition for handle not found.", NamedTextColor.RED));
      return 0;
    }

    Optional<AnimationDefinition> animDefOpt = modelOpt.get().findAnimation(animId);
    if (animDefOpt.isEmpty()) {
      sender.sendMessage(
          Component.text(
              "Animation '"
                  + animIdStr
                  + "' not defined in model '"
                  + handle.definitionId().value()
                  + "'.",
              NamedTextColor.RED));
      return 0;
    }

    try {
      AnimationPlaybackId playbackId =
          animationRuntimeService.play(handle, animId, PlaybackOptions.DEFAULT);
      sender.sendMessage(
          Component.text("Started animation '", NamedTextColor.GREEN)
              .append(Component.text(animIdStr, NamedTextColor.YELLOW))
              .append(Component.text("' (Playback ID: ", NamedTextColor.GREEN))
              .append(Component.text(playbackId.value(), NamedTextColor.GOLD))
              .append(Component.text(")", NamedTextColor.GREEN)));
      return 1;
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Error playing animation: " + e.getMessage(), NamedTextColor.RED));
      return 0;
    }
  }

  private int pauseAnimation(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (animationRuntimeService == null) return 0;

    String playbackIdStr = StringArgumentType.getString(ctx, "playbackId");
    AnimationPlaybackId playbackId = AnimationPlaybackId.of(playbackIdStr);

    if (animationRuntimeService.pause(playbackId)) {
      sender.sendMessage(
          Component.text(
              "Paused animation playback '" + playbackIdStr + "'.", NamedTextColor.GREEN));
      return 1;
    } else {
      sender.sendMessage(
          Component.text("Failed to pause playback '" + playbackIdStr + "'.", NamedTextColor.RED));
      return 0;
    }
  }

  private int resumeAnimation(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (animationRuntimeService == null) return 0;

    String playbackIdStr = StringArgumentType.getString(ctx, "playbackId");
    AnimationPlaybackId playbackId = AnimationPlaybackId.of(playbackIdStr);

    if (animationRuntimeService.resume(playbackId)) {
      sender.sendMessage(
          Component.text(
              "Resumed animation playback '" + playbackIdStr + "'.", NamedTextColor.GREEN));
      return 1;
    } else {
      sender.sendMessage(
          Component.text("Failed to resume playback '" + playbackIdStr + "'.", NamedTextColor.RED));
      return 0;
    }
  }

  private int stopAnimation(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (animationRuntimeService == null) return 0;

    String playbackIdStr = StringArgumentType.getString(ctx, "playbackId");
    AnimationPlaybackId playbackId = AnimationPlaybackId.of(playbackIdStr);

    if (animationRuntimeService.stop(playbackId)) {
      sender.sendMessage(
          Component.text(
              "Stopped animation playback '" + playbackIdStr + "'.", NamedTextColor.GREEN));
      return 1;
    } else {
      sender.sendMessage(
          Component.text("Failed to stop playback '" + playbackIdStr + "'.", NamedTextColor.RED));
      return 0;
    }
  }

  private int seekAnimation(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (animationRuntimeService == null) return 0;

    String playbackIdStr = StringArgumentType.getString(ctx, "playbackId");
    String timeStr = StringArgumentType.getString(ctx, "time");

    AnimationPlaybackId playbackId = AnimationPlaybackId.of(playbackIdStr);
    AnimationTime targetTime = animationCompiler.parseAnimationTime(timeStr, "seek", List.of());

    if (animationRuntimeService.seek(playbackId, targetTime)) {
      sender.sendMessage(
          Component.text(
              "Seeked playback '" + playbackIdStr + "' to " + timeStr + ".", NamedTextColor.GREEN));
      return 1;
    } else {
      sender.sendMessage(
          Component.text("Failed to seek playback '" + playbackIdStr + "'.", NamedTextColor.RED));
      return 0;
    }
  }

  private int playbackInfo(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (animationRuntimeService == null) return 0;

    String playbackIdStr = StringArgumentType.getString(ctx, "playbackId");
    AnimationPlaybackId playbackId = AnimationPlaybackId.of(playbackIdStr);

    Optional<AnimationPlaybackState> stateOpt =
        animationRuntimeService.getPlaybackState(playbackId);
    if (stateOpt.isEmpty()) {
      sender.sendMessage(
          Component.text("Playback ID '" + playbackIdStr + "' not found.", NamedTextColor.RED));
      return 0;
    }

    AnimationPlaybackState state = stateOpt.get();
    sender.sendMessage(
        Component.text("Animation Playback Info: ", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
            .append(Component.text(state.playbackId().value(), NamedTextColor.GOLD)));

    sender.sendMessage(
        Component.text(" Model: ", NamedTextColor.GRAY)
            .append(
                Component.text(state.modelHandle().definitionId().value(), NamedTextColor.YELLOW))
            .append(
                Component.text(
                    " (runtime: " + state.modelHandle().runtimeId().value() + ")",
                    NamedTextColor.GRAY)));
    sender.sendMessage(
        Component.text(" Animation: ", NamedTextColor.GRAY)
            .append(Component.text(state.animationId().value(), NamedTextColor.AQUA)));
    sender.sendMessage(
        Component.text(" State: ", NamedTextColor.GRAY)
            .append(Component.text(state.state().name(), NamedTextColor.GREEN)));
    sender.sendMessage(
        Component.text(" Playhead: ", NamedTextColor.GRAY)
            .append(Component.text(state.currentTime().toSeconds() + "s", NamedTextColor.WHITE)));
    sender.sendMessage(
        Component.text(" Speed: ", NamedTextColor.GRAY)
            .append(Component.text(state.speed() + "x", NamedTextColor.WHITE)));
    sender.sendMessage(
        Component.text(" Loop: ", NamedTextColor.GRAY)
            .append(
                Component.text(
                    state.loopMode().name() + " (current loop: " + state.currentLoop() + ")",
                    NamedTextColor.WHITE)));
    return 1;
  }
}
