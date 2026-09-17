package io.github.kizio806.spectraevents.platform.sponge.lifecycle;

import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.sponge.SpongeBootstrap;
import io.github.kizio806.spectraevents.platform.sponge.render.SpongeModelRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.server.ServerWorld;

public class SpongeEntityReconciler implements PlatformEntityReconcilerPort {
  private final SpongeBootstrap plugin;
  private final SpongeModelRenderer renderer;

  public SpongeEntityReconciler(SpongeBootstrap plugin, SpongeModelRenderer renderer) {
    this.plugin = plugin;
    this.renderer = renderer;
  }

  @Override
  public List<DiscoveredEntity> scanLoadedEntities() {
    List<DiscoveredEntity> discovered = new ArrayList<>();
    return discovered;
  }

  @Override
  public void removeEntity(Object platformEntityReference) {
    if (platformEntityReference instanceof UUID uuid) {
      if (Sponge.isServerAvailable()) {
        for (ServerWorld world : Sponge.server().worldManager().worlds()) {
          world.entity(uuid).ifPresent(Entity::remove);
        }
      }
    }
  }

  @Override
  public void restoreInstance(EventInstanceId instanceId, List<DiscoveredEntity> entities) {
    Map<String, UUID> spawnedParts = new HashMap<>();
    UUID interactionEntityId = null;

    for (DiscoveredEntity de : entities) {
      if (de.platformReference() instanceof UUID uuid) {
        if ("INTERACTION".equals(de.role())) {
          interactionEntityId = uuid;
        } else if (de.partId() != null) {
          spawnedParts.put(de.partId(), uuid);
        }
      }
    }

    if (interactionEntityId != null || !spawnedParts.isEmpty()) {
      renderer.restore(instanceId, spawnedParts, interactionEntityId);
    }
  }
}
