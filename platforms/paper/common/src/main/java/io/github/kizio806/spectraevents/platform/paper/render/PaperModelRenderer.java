package io.github.kizio806.spectraevents.platform.paper.render;

import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedPartHandle;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.core.visual.model.TextAssetRef;
import io.github.kizio806.spectraevents.platform.bukkit.integration.item.CustomItemProvider;
import io.github.kizio806.spectraevents.platform.bukkit.render.AbstractBukkitModelRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

/**
 * Production Paper platform renderer implementing ModelRendererPort using native Display &
 * Interaction entities with Folia region scheduling and asynchronous teleportation.
 */
public class PaperModelRenderer extends AbstractBukkitModelRenderer {

  private final Map<ModelRuntimeId, CompletableFuture<Void>> inFlightTransforms =
      new ConcurrentHashMap<>();

  public PaperModelRenderer(Plugin plugin, CustomItemProvider customItemProvider) {
    super(plugin, customItemProvider);
  }

  public PaperModelRenderer(Plugin plugin) {
    this(plugin, null);
  }

  @Override
  protected void configureTextDisplay(TextDisplay entity, TextAssetRef textRef) {
    if (textRef.text() != null && !textRef.text().isBlank()) {
      entity.text(MiniMessage.miniMessage().deserialize(textRef.text()));
    }
  }

  @Override
  protected void applyTransformTeleports(
      RenderedModelHandle handle, List<Entity> entities, Location newLoc) {
    List<CompletableFuture<?>> teleportFutures = new ArrayList<>();
    for (Entity entity : entities) {
      teleportFutures.add(entity.teleportAsync(newLoc));
    }
    CompletableFuture<Void> transformFuture =
        CompletableFuture.allOf(teleportFutures.toArray(CompletableFuture<?>[]::new))
            .exceptionally(
                exception -> {
                  plugin
                      .getLogger()
                      .warning(
                          "Failed to move model "
                              + handle.runtimeId()
                              + ": "
                              + exception.getMessage());
                  return null;
                });
    inFlightTransforms.put(handle.runtimeId(), transformFuture);
  }

  @Override
  public boolean updatePartTransform(
      RenderedModelHandle handle,
      ModelPartId partId,
      ModelTransform newLocalTransform,
      int interpolationDurationTicks) {
    Objects.requireNonNull(handle, "handle cannot be null");
    Objects.requireNonNull(partId, "partId cannot be null");
    Objects.requireNonNull(newLocalTransform, "newLocalTransform cannot be null");

    RenderedPartHandle partHandle = handle.parts().get(partId);
    if (partHandle == null) return false;

    Entity entity = Bukkit.getEntity(partHandle.entityUuid());
    if (entity instanceof Display display && entity.isValid()) {
      display
          .getScheduler()
          .execute(
              plugin,
              () -> {
                if (interpolationDurationTicks >= 0) {
                  display.setInterpolationDuration(interpolationDurationTicks);
                }
                display.setTransformationMatrix(toMatrix4f(newLocalTransform));
              },
              null,
              1);
      return true;
    }
    return false;
  }

  @Override
  public boolean removeModel(RenderedModelHandle handle) {
    Objects.requireNonNull(handle, "handle cannot be null");
    boolean removed = activeHandles.remove(handle.runtimeId()) != null;
    inFlightTransforms.remove(handle.runtimeId());

    World world = Bukkit.getWorld(handle.anchor().worldName());
    if (world != null) {
      Location loc =
          new Location(world, handle.anchor().x(), handle.anchor().y(), handle.anchor().z());
      if (!plugin.isEnabled()) {
        removeEntities(handle);
        return removed;
      }
      Bukkit.getRegionScheduler().execute(plugin, loc, () -> removeEntities(handle));
      return removed;
    }
    return false;
  }

  @Override
  public void removeAll() {
    super.removeAll();
    inFlightTransforms.clear();
  }
}
