package io.github.kizio806.spectraevents.platform.paper.interaction;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import org.bukkit.entity.Entity;

/** Delegate interface for entity death events tied to an event instance. */
public interface EventEntityDeathDelegate {
  void handleEntityDeath(Entity entity, EventInstance instance);
}
