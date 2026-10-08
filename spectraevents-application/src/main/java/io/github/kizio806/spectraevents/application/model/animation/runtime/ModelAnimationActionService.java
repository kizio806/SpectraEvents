package io.github.kizio806.spectraevents.application.model.animation.runtime;

import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.animation.LoopMode;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Executes event actions that start or stop animations on event-owned models. */
public final class ModelAnimationActionService {
  private final ModelRuntimeService modelRuntimeService;
  private final AnimationRuntimeService animationRuntimeService;

  public ModelAnimationActionService(
      ModelRuntimeService modelRuntimeService, AnimationRuntimeService animationRuntimeService) {
    this.modelRuntimeService = Objects.requireNonNull(modelRuntimeService, "modelRuntimeService");
    this.animationRuntimeService =
        Objects.requireNonNull(animationRuntimeService, "animationRuntimeService");
  }

  /** Starts an animation on every matching model owned by the event instance. */
  public int play(EventInstanceId eventId, Map<String, Object> parameters) {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(parameters, "parameters");

    AnimationId animationId = new AnimationId(requiredString(parameters, "animation"));
    ModelId modelId = optionalModelId(parameters);
    PlaybackOptions options = playbackOptions(parameters);

    List<RenderedModelHandle> matchingHandles =
        modelRuntimeService.getActiveInstances().stream()
            .filter(handle -> eventId.equals(handle.ownerEventId()))
            .filter(handle -> modelId == null || modelId.equals(handle.definitionId()))
            .toList();

    if (matchingHandles.isEmpty()) {
      String target =
          modelId == null ? "owned by the event" : "for model '" + modelId.value() + "'";
      throw new FatalActionException("play_animation found no active model " + target);
    }

    for (RenderedModelHandle handle : matchingHandles) {
      try {
        animationRuntimeService.play(handle, animationId, options);
      } catch (RuntimeException exception) {
        throw new FatalActionException(
            "Could not play animation '"
                + animationId.value()
                + "' on model '"
                + handle.definitionId().value()
                + "'",
            exception);
      }
    }
    return matchingHandles.size();
  }

  /** Stops all animations currently attached to models owned by the event instance. */
  public void stopForEvent(EventInstanceId eventId) {
    Objects.requireNonNull(eventId, "eventId");
    for (RenderedModelHandle handle : modelRuntimeService.getActiveInstances()) {
      if (eventId.equals(handle.ownerEventId())) {
        animationRuntimeService.stopAll(handle);
      }
    }
  }

  /** Stops all animations attached to one model before it is removed. */
  public void stopForModel(RenderedModelHandle handle) {
    animationRuntimeService.stopAll(Objects.requireNonNull(handle, "handle"));
  }

  private static PlaybackOptions playbackOptions(Map<String, Object> parameters) {
    float speed = number(parameters, "speed", 1.0f);
    LoopMode loopMode = loopMode(parameters.get("loop"));
    int maxLoops = integer(parameters, "max-loops", -1);
    if (maxLoops < -1) {
      throw new FatalActionException("play_animation.max-loops must be -1 or greater");
    }
    return new PlaybackOptions(speed, loopMode, maxLoops, null, null);
  }

  private static ModelId optionalModelId(Map<String, Object> parameters) {
    Object value = parameters.get("model");
    if (value == null) {
      return null;
    }
    if (!(value instanceof String model) || model.isBlank()) {
      throw new FatalActionException("play_animation.model must be a non-empty string");
    }
    return new ModelId(model);
  }

  private static String requiredString(Map<String, Object> parameters, String name) {
    Object value = parameters.get(name);
    if (!(value instanceof String string) || string.isBlank()) {
      throw new FatalActionException("play_animation." + name + " must be a non-empty string");
    }
    return string;
  }

  private static float number(Map<String, Object> parameters, String name, float defaultValue) {
    Object value = parameters.get(name);
    if (value == null) {
      return defaultValue;
    }
    if (!(value instanceof Number number)) {
      throw new FatalActionException("play_animation." + name + " must be a number");
    }
    float result = number.floatValue();
    if (!Float.isFinite(result) || result <= 0.0f) {
      throw new FatalActionException("play_animation." + name + " must be greater than zero");
    }
    return result;
  }

  private static int integer(Map<String, Object> parameters, String name, int defaultValue) {
    Object value = parameters.get(name);
    if (value == null) {
      return defaultValue;
    }
    if (!(value instanceof Number number)) {
      throw new FatalActionException("play_animation." + name + " must be an integer");
    }
    try {
      return new BigDecimal(number.toString()).intValueExact();
    } catch (NumberFormatException | ArithmeticException exception) {
      throw new FatalActionException("play_animation." + name + " must be an integer", exception);
    }
  }

  private static LoopMode loopMode(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean loop) {
      return loop ? LoopMode.LOOP : LoopMode.ONCE;
    }
    if (!(value instanceof String string) || string.isBlank()) {
      throw new FatalActionException("play_animation.loop must be a boolean or loop mode");
    }
    try {
      return LoopMode.valueOf(string.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new FatalActionException(
          "play_animation.loop must be ONCE, LOOP, or PING_PONG", exception);
    }
  }
}
