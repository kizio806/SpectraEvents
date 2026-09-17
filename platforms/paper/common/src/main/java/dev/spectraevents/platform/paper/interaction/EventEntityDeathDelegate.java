package dev.spectraevents.platform.paper.interaction;

import dev.spectraevents.core.event.runtime.EventInstance;
import org.bukkit.entity.Entity;

/** Delegate interface for entity death events tied to an event instance. */
public interface EventEntityDeathDelegate {
  void handleEntityDeath(Entity entity, EventInstance instance);
}
