package io.github.kizio806.spectraevents.platform.bukkit.render;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Collections;
import java.util.HashMap;
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
    partEntities =
        Collections.unmodifiableMap(
            new HashMap<>(Objects.requireNonNull(partEntities, "partEntities")));
  }

  @Override
  public Map<String, UUID> partEntities() {
    return Collections.unmodifiableMap(new HashMap<>(partEntities));
  }
}
