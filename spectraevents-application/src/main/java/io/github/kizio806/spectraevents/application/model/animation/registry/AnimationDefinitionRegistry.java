package io.github.kizio806.spectraevents.application.model.animation.registry;

import io.github.kizio806.spectraevents.application.model.animation.compiler.CompiledAnimation;
import io.github.kizio806.spectraevents.core.visual.animation.AnimationId;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe registry for compiled model animations. */
public class AnimationDefinitionRegistry {

  private final Map<Key, CompiledAnimation> animations = new ConcurrentHashMap<>();

  public record Key(ModelId modelId, AnimationId animationId) {
    public Key {
      Objects.requireNonNull(modelId, "modelId cannot be null");
      Objects.requireNonNull(animationId, "animationId cannot be null");
    }
  }

  public void register(ModelId modelId, CompiledAnimation compiledAnimation) {
    Objects.requireNonNull(modelId, "modelId cannot be null");
    Objects.requireNonNull(compiledAnimation, "compiledAnimation cannot be null");
    animations.put(new Key(modelId, compiledAnimation.definition().id()), compiledAnimation);
  }

  public Optional<CompiledAnimation> find(ModelId modelId, AnimationId animationId) {
    Objects.requireNonNull(modelId, "modelId cannot be null");
    Objects.requireNonNull(animationId, "animationId cannot be null");
    return Optional.ofNullable(animations.get(new Key(modelId, animationId)));
  }

  public void unregister(ModelId modelId) {
    Objects.requireNonNull(modelId, "modelId cannot be null");
    animations.keySet().removeIf(k -> k.modelId().equals(modelId));
  }

  public void clear() {
    animations.clear();
  }

  public java.util.Collection<CompiledAnimation> all() {
    return animations.values();
  }

  public int count() {
    return animations.size();
  }
}
