package io.github.kizio806.spectraevents.platform.paper.lifecycle;

import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.paper.metadata.SpectraPdcKeys;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class PaperEntityReconciler implements PlatformEntityReconcilerPort {
  private final Plugin plugin;
  private final PaperModelRenderer renderer;

  public PaperEntityReconciler(Plugin plugin, PaperModelRenderer renderer) {
    this.plugin = plugin;
    this.renderer = renderer;
  }

  @Override
  public List<DiscoveredEntity> scanLoadedEntities() {
    List<DiscoveredEntity> discovered = new ArrayList<>();
    for (World world : Bukkit.getWorlds()) {
      for (Chunk chunk : world.getLoadedChunks()) {
        for (Entity entity : chunk.getEntities()) {
          String instanceIdStr =
              entity
                  .getPersistentDataContainer()
                  .get(SpectraPdcKeys.INSTANCE_ID, PersistentDataType.STRING);
          if (instanceIdStr != null) {
            try {
              EventInstanceId instanceId = new EventInstanceId(UUID.fromString(instanceIdStr));
              String partId =
                  entity
                      .getPersistentDataContainer()
                      .get(SpectraPdcKeys.partId(plugin), PersistentDataType.STRING);

              String role =
                  entity
                      .getType()
                      .name(); // Use entity type as role for now if no specific role set

              discovered.add(new DiscoveredEntity(entity.getUniqueId(), instanceId, role, partId));
            } catch (IllegalArgumentException ignored) {
              // Invalid UUID format
            }
          }
        }
      }
    }
    return discovered;
  }

  @Override
  public void removeEntity(Object platformEntityReference) {
    if (platformEntityReference instanceof UUID uuid) {
      Entity entity = Bukkit.getEntity(uuid);
      if (entity != null && entity.isValid()) {
        entity.getScheduler().execute(plugin, entity::remove, null, 1);
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

    // We only restore if we have something, but handle interaction missing
    if (interactionEntityId != null || !spawnedParts.isEmpty()) {
      renderer.restore(instanceId, spawnedParts, interactionEntityId);
    }
  }
}
