package io.github.kizio806.spectraevents.application.model.runtime;

import io.github.kizio806.spectraevents.application.model.registry.ModelDefinitionRegistry;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application service orchestrating 3D model definition lookups, spawning, transform updates,
 * despawning, and active runtime instance tracking.
 */
public class ModelRuntimeService {
  private final ModelDefinitionRegistry definitionRegistry;
  private final ModelRendererPort rendererPort;
  private final Map<ModelRuntimeId, RenderedModelHandle> activeInstances =
      new ConcurrentHashMap<>();

  public ModelRuntimeService(
      ModelDefinitionRegistry definitionRegistry, ModelRendererPort rendererPort) {
    this.definitionRegistry =
        Objects.requireNonNull(definitionRegistry, "definitionRegistry cannot be null");
    this.rendererPort = Objects.requireNonNull(rendererPort, "rendererPort cannot be null");
  }

  /** Spawns a new 3D model instance from a compiled definition ID. */
  public RenderedModelHandle spawnModel(
      ModelId definitionId, ModelAnchor anchor, EventInstanceId ownerEventId) {
    Objects.requireNonNull(definitionId, "definitionId cannot be null");
    Objects.requireNonNull(anchor, "anchor cannot be null");

    ModelDefinition definition =
        definitionRegistry
            .get(definitionId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Model definition '" + definitionId.value() + "' not found in registry"));

    ModelRuntimeId runtimeId = ModelRuntimeId.generate();
    RenderedModelHandle handle =
        rendererPort.spawnModel(runtimeId, definition, anchor, ownerEventId);

    if (handle != null) {
      activeInstances.put(runtimeId, handle);
    }
    return handle;
  }

  /** Updates a model instance spatial anchor location. */
  public boolean updateModelTransform(ModelRuntimeId runtimeId, ModelAnchor newAnchor) {
    Objects.requireNonNull(runtimeId, "runtimeId cannot be null");
    Objects.requireNonNull(newAnchor, "newAnchor cannot be null");

    RenderedModelHandle handle = activeInstances.get(runtimeId);
    if (handle == null) {
      return false;
    }
    boolean updated = rendererPort.updateModelTransform(handle, newAnchor);
    if (updated) {
      activeInstances.put(
          runtimeId,
          new RenderedModelHandle(
              handle.runtimeId(),
              handle.definitionId(),
              handle.ownerEventId(),
              newAnchor,
              handle.parts(),
              handle.interactions()));
    }
    return updated;
  }

  /** Updates a single part's local transform within a active model instance. */
  public boolean updatePartTransform(
      ModelRuntimeId runtimeId, ModelPartId partId, ModelTransform newLocalTransform) {
    Objects.requireNonNull(runtimeId, "runtimeId cannot be null");
    Objects.requireNonNull(partId, "partId cannot be null");
    Objects.requireNonNull(newLocalTransform, "newLocalTransform cannot be null");

    RenderedModelHandle handle = activeInstances.get(runtimeId);
    if (handle == null) {
      return false;
    }
    return rendererPort.updatePartTransform(handle, partId, newLocalTransform);
  }

  /** Idempotently removes a spawned 3D model instance. */
  public boolean removeModel(ModelRuntimeId runtimeId) {
    Objects.requireNonNull(runtimeId, "runtimeId cannot be null");
    RenderedModelHandle handle = activeInstances.remove(runtimeId);
    if (handle == null) {
      return false;
    }
    return rendererPort.removeModel(handle);
  }

  public Optional<RenderedModelHandle> getHandle(ModelRuntimeId runtimeId) {
    Objects.requireNonNull(runtimeId, "runtimeId cannot be null");
    return Optional.ofNullable(activeInstances.get(runtimeId));
  }

  public Collection<RenderedModelHandle> getActiveInstances() {
    return List.copyOf(activeInstances.values());
  }

  public void restoreHandle(RenderedModelHandle handle) {
    Objects.requireNonNull(handle, "handle cannot be null");
    activeInstances.put(handle.runtimeId(), handle);
  }

  public void removeAll() {
    activeInstances.clear();
    rendererPort.removeAll();
  }
}
