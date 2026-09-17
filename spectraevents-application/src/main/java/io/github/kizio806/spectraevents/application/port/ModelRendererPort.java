package io.github.kizio806.spectraevents.application.port;

import io.github.kizio806.spectraevents.application.model.runtime.DiscoveredModelEntity;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import java.util.List;

/**
 * Platform port for spawning, updating, removing, and reconciling 3D Display & Interaction model
 * entities.
 */
public interface ModelRendererPort {

  /**
   * Spawns all visual parts and interaction hitboxes of a 3D model definition at the target anchor.
   * Performs atomic spawn rollback if any part spawn fails.
   *
   * @param runtimeId unique model runtime ID
   * @param definition compiled immutable model definition
   * @param anchor spatial anchor position and orientation
   * @param ownerEventId optional owner event instance ID
   * @return neutral handle to the rendered model instance
   */
  RenderedModelHandle spawnModel(
      ModelRuntimeId runtimeId,
      ModelDefinition definition,
      ModelAnchor anchor,
      EventInstanceId ownerEventId);

  /** Updates the spatial anchor of a spawned model. */
  boolean updateModelTransform(RenderedModelHandle handle, ModelAnchor newAnchor);

  /** Updates the spatial anchor of a spawned model with smooth interpolation duration in ticks. */
  default boolean updateModelTransform(
      RenderedModelHandle handle, ModelAnchor newAnchor, int interpolationDurationTicks) {
    return updateModelTransform(handle, newAnchor);
  }

  /** Updates a single part's transform within a spawned model. */
  boolean updatePartTransform(
      RenderedModelHandle handle, ModelPartId partId, ModelTransform newLocalTransform);

  /**
   * Updates a single part's transform within a spawned model with smooth interpolation duration in
   * ticks.
   */
  default boolean updatePartTransform(
      RenderedModelHandle handle,
      ModelPartId partId,
      ModelTransform newLocalTransform,
      int interpolationDurationTicks) {
    return updatePartTransform(handle, partId, newLocalTransform);
  }

  /** Removes all native entities belonging to a rendered model instance. */
  boolean removeModel(RenderedModelHandle handle);

  /** Removes all active tracked model entities across all worlds. */
  void removeAll();

  /** Scans server worlds for persistent 3D model entities. */
  List<DiscoveredModelEntity> reconcileEntities();
}
