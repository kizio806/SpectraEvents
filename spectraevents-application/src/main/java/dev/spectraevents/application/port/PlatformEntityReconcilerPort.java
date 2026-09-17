package dev.spectraevents.application.port;

import dev.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;

public interface PlatformEntityReconcilerPort {
  /** Finds all entities currently loaded in the world that are owned by SpectraEvents. */
  List<DiscoveredEntity> scanLoadedEntities();

  /** Removes an entity from the world (e.g. an orphan). */
  void removeEntity(Object platformEntityReference);

  /**
   * Restores the entity references to the platform's internal tracking (like PaperModelRenderer
   * registry).
   */
  void restoreInstance(EventInstanceId instanceId, List<DiscoveredEntity> entities);

  record DiscoveredEntity(
      Object platformReference, EventInstanceId instanceId, String role, String partId) {}
}
