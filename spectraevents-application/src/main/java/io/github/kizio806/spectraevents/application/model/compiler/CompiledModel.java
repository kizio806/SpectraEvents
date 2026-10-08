package io.github.kizio806.spectraevents.application.model.compiler;

import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import java.util.Map;
import java.util.Objects;

/** Immutable model compilation result, retaining executable animation plans for registration. */
public record CompiledModel(
    ModelDefinition definition, Map<AnimationId, CompiledAnimation> animations) {
  public CompiledModel {
    Objects.requireNonNull(definition, "definition cannot be null");
    Objects.requireNonNull(animations, "animations cannot be null");
    animations = Map.copyOf(animations);
  }
}
