package io.github.kizio806.spectraevents.platform.bukkit.lifecycle;

import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.metadata.BukkitPdcKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Base reconciler implementing chunk scanning and PDC entity recovery across Bukkit platforms. */
public abstract class AbstractBukkitEntityReconciler implements PlatformEntityReconcilerPort {

  protected final Plugin plugin;

  protected AbstractBukkitEntityReconciler(Plugin plugin) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
  }

  public Plugin plugin() {
    return plugin;
  }

  @Override
  public List<DiscoveredEntity> scanLoadedEntities() {
    List<DiscoveredEntity> discovered = new ArrayList<>();
    if (Bukkit.getServer() == null) {
      return discovered;
    }
    for (World world : Bukkit.getWorlds()) {
      for (Chunk chunk : world.getLoadedChunks()) {
        for (Entity entity : chunk.getEntities()) {
          String raw =
              entity
                  .getPersistentDataContainer()
                  .get(BukkitPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING);
          if (raw == null) {
            continue;
          }
          try {
            EventInstanceId instanceId = new EventInstanceId(UUID.fromString(raw));
            String partId =
                entity
                    .getPersistentDataContainer()
                    .get(BukkitPdcKeys.MODEL_PART_ID, PersistentDataType.STRING);
            String role =
                entity
                    .getPersistentDataContainer()
                    .get(BukkitPdcKeys.RESOURCE_ROLE, PersistentDataType.STRING);
            if (role == null) {
              role = entity.getType().name();
            }
            discovered.add(new DiscoveredEntity(entity.getUniqueId(), instanceId, role, partId));
          } catch (IllegalArgumentException ignored) {
            onCorruptedPdcEntity(entity);
          }
        }
      }
    }
    return discovered;
  }

  @Override
  public void removeEntity(Object platformEntityReference) {
    if (Bukkit.getServer() == null) {
      return;
    }
    if (platformEntityReference instanceof UUID uuid) {
      Entity entity = Bukkit.getEntity(uuid);
      if (entity != null && entity.isValid()) {
        removeEntitySafely(entity);
      }
    }
  }

  protected void onCorruptedPdcEntity(Entity entity) {
    removeEntitySafely(entity);
  }

  public abstract void removeEntitySafely(Entity entity);
}
