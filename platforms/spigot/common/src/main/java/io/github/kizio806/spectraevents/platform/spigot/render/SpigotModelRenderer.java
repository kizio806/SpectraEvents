package io.github.kizio806.spectraevents.platform.spigot.render;

import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedPartHandle;
import io.github.kizio806.spectraevents.platform.bukkit.render.AbstractBukkitModelRenderer;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Production Spigot platform renderer implementing ModelRendererPort using Bukkit Display &
 * Interaction APIs.
 */
public class SpigotModelRenderer extends AbstractBukkitModelRenderer {

  public SpigotModelRenderer(Plugin plugin) {
    super(plugin);
  }

  @Override
  public boolean updateModelTransform(RenderedModelHandle handle, ModelAnchor newAnchor) {
    Objects.requireNonNull(handle, "handle cannot be null");
    Objects.requireNonNull(newAnchor, "newAnchor cannot be null");

    World world = Bukkit.getWorld(newAnchor.worldName());
    if (world == null) return false;

    Location newLoc =
        new Location(
            world, newAnchor.x(), newAnchor.y(), newAnchor.z(), newAnchor.yaw(), newAnchor.pitch());

    for (RenderedPartHandle partHandle : handle.parts().values()) {
      Entity entity = Bukkit.getEntity(partHandle.entityUuid());
      if (entity != null && entity.isValid()) {
        entity.teleport(newLoc);
      }
    }
    for (UUID interactionUuid : handle.interactions().values()) {
      Entity entity = Bukkit.getEntity(interactionUuid);
      if (entity != null && entity.isValid()) {
        entity.teleport(newLoc);
      }
    }
    activeHandles.put(
        handle.runtimeId(),
        new RenderedModelHandle(
            handle.runtimeId(),
            handle.definitionId(),
            handle.ownerEventId(),
            newAnchor,
            handle.parts(),
            handle.interactions()));
    return true;
  }
}
