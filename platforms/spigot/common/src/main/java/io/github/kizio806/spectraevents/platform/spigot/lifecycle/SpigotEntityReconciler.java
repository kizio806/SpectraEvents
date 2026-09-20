package io.github.kizio806.spectraevents.platform.spigot.lifecycle;

import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import io.github.kizio806.spectraevents.platform.spigot.metadata.SpigotPdcKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class SpigotEntityReconciler implements PlatformEntityReconcilerPort {
  private final SpigotActionAdapter actionAdapter;

  public SpigotEntityReconciler(Plugin plugin, SpigotActionAdapter actionAdapter) {
    this.actionAdapter = actionAdapter;
  }

  @Override
  public List<DiscoveredEntity> scanLoadedEntities() {
    List<DiscoveredEntity> discovered = new ArrayList<>();
    for (World world : Bukkit.getWorlds()) {
      for (Chunk chunk : world.getLoadedChunks()) {
        for (Entity entity : chunk.getEntities()) {
          String raw =
              entity
                  .getPersistentDataContainer()
                  .get(SpigotPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING);
          if (raw == null) {
            continue;
          }
          try {
            String role =
                entity
                    .getPersistentDataContainer()
                    .get(SpigotPdcKeys.RESOURCE_ROLE, PersistentDataType.STRING);
            String part =
                entity
                    .getPersistentDataContainer()
                    .get(SpigotPdcKeys.MODEL_PART_ID, PersistentDataType.STRING);
            discovered.add(
                new DiscoveredEntity(
                    entity.getUniqueId(),
                    new EventInstanceId(UUID.fromString(raw)),
                    role == null ? entity.getType().name() : role,
                    part));
          } catch (IllegalArgumentException ignored) {
            entity.remove();
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
        entity.remove();
      }
    }
  }

  @Override
  public void restoreInstance(EventInstanceId instanceId, List<DiscoveredEntity> entities) {
    for (DiscoveredEntity discovered : entities) {
      Object reference = discovered.platformReference();
      if (reference instanceof UUID uuid) {
        actionAdapter.registerRecoveredEntity(instanceId, uuid);
      }
    }
  }
}
