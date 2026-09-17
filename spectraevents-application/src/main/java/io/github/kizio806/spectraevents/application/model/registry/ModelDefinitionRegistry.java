package io.github.kizio806.spectraevents.application.model.registry;

import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe application registry for compiled 3D ModelDefinitions. */
public class ModelDefinitionRegistry {
  private final Map<ModelId, ModelDefinition> registry = new ConcurrentHashMap<>();

  public void register(ModelDefinition definition) {
    Objects.requireNonNull(definition, "definition cannot be null");
    ModelDefinition existing = registry.putIfAbsent(definition.id(), definition);
    if (existing != null) {
      throw new IllegalArgumentException(
          "ModelDefinition with id '" + definition.id().value() + "' is already registered");
    }
  }

  public void replace(ModelDefinition definition) {
    Objects.requireNonNull(definition, "definition cannot be null");
    registry.put(definition.id(), definition);
  }

  public Optional<ModelDefinition> get(ModelId id) {
    Objects.requireNonNull(id, "id cannot be null");
    return Optional.ofNullable(registry.get(id));
  }

  public boolean contains(ModelId id) {
    Objects.requireNonNull(id, "id cannot be null");
    return registry.containsKey(id);
  }

  public Collection<ModelDefinition> all() {
    return List.copyOf(registry.values());
  }

  public void clear() {
    registry.clear();
  }
}
