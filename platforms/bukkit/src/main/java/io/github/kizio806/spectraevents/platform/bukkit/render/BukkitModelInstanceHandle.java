package io.github.kizio806.spectraevents.platform.bukkit.render;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Platform handle wrapping spawned Bukkit/Paper display and interaction entity UUIDs for an
 * instance.
 */
public record BukkitModelInstanceHandle(
    EventInstanceId instanceId, Map<String, UUID> partEntities, UUID interactionEntityId) {

  public BukkitModelInstanceHandle {
    Objects.requireNonNull(instanceId, "instanceId");
    partEntities = Map.copyOf(Objects.requireNonNull(partEntities, "partEntities"));
  }
}
