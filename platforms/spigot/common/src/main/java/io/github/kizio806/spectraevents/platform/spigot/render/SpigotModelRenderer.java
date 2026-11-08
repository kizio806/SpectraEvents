package io.github.kizio806.spectraevents.platform.spigot.render;

import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.platform.bukkit.render.AbstractBukkitModelRenderer;
import java.util.List;
import org.bukkit.Location;
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
  protected void applyTransformTeleports(
      RenderedModelHandle handle, List<Entity> entities, Location newLoc) {
    for (Entity entity : entities) {
      entity.teleport(newLoc);
    }
  }
}
