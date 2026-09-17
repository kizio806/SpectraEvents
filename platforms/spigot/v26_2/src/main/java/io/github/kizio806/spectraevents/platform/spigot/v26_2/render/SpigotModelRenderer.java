package io.github.kizio806.spectraevents.platform.spigot.v26_2.render;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.Transform;
import io.github.kizio806.spectraevents.platform.spigot.v26_2.metadata.SpigotPdcKeys;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.joml.Matrix4f;

/**
 * Reusable Paper platform renderer for rendering multi-part event models using Display entities.
 */
public class SpigotModelRenderer {
  private final Plugin plugin;
  private final Map<EventInstanceId, SpigotModelInstanceHandle> registry =
      new ConcurrentHashMap<>();

  public SpigotModelRenderer(Plugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Spawns a new multi-part model for the event instance.
   *
   * @param instanceId the event instance domain ID
   * @param model the model definition
   * @param location the target location
   * @return true if successfully spawned, false if an object already exists for this instance
   */
  public boolean spawn(EventInstanceId instanceId, ModelDefinition model, Location location) {
    return spawnWithTranslation(instanceId, model, location, new org.joml.Vector3f(0, 0, 0));
  }

  /**
   * Spawns a new multi-part model with an initial global translation offset.
   *
   * @param instanceId the event instance domain ID
   * @param model the model definition
   * @param location the base target location
   * @param offset the initial translation offset to apply
   * @return true if successfully spawned
   */
  public boolean spawnWithTranslation(
      EventInstanceId instanceId,
      ModelDefinition model,
      Location location,
      org.joml.Vector3f offset) {

    if (registry.containsKey(instanceId)) {
      return false; // Reject duplicate spawn
    }

    Map<String, UUID> spawnedParts = new HashMap<>();

    // Spawn each part as an ItemDisplay
    for (ModelPartDefinition part : model.parts()) {
      ItemDisplay display =
          location
              .getWorld()
              .spawn(
                  location,
                  ItemDisplay.class,
                  entity -> {
                    Material mat = Material.matchMaterial(part.visualId());
                    if (mat == null) mat = Material.MAGMA_BLOCK;

                    entity.setItemStack(new ItemStack(mat));

                    Transform t = part.localTransform();
                    Matrix4f matrix =
                        new Matrix4f()
                            .translation(t.tx() + offset.x, t.ty() + offset.y, t.tz() + offset.z)
                            .rotateX((float) Math.toRadians(t.rx()))
                            .rotateY((float) Math.toRadians(t.ry()))
                            .rotateZ((float) Math.toRadians(t.rz()))
                            .scale(t.sx(), t.sy(), t.sz());
                    entity.setTransformationMatrix(matrix);

                    entity
                        .getPersistentDataContainer()
                        .set(
                            SpigotPdcKeys.instanceId(plugin),
                            PersistentDataType.STRING,
                            instanceId.toString());
                    entity
                        .getPersistentDataContainer()
                        .set(
                            SpigotPdcKeys.partId(plugin), PersistentDataType.STRING, part.partId());
                  });
      spawnedParts.put(part.partId(), display.getUniqueId());
    }

    // Spawn a single Interaction hitbox for the whole model at the root location
    Interaction interaction =
        location
            .getWorld()
            .spawn(
                location,
                Interaction.class,
                entity -> {
                  entity.setInteractionWidth(1.0f);
                  entity.setInteractionHeight(1.0f);
                  entity
                      .getPersistentDataContainer()
                      .set(
                          SpigotPdcKeys.instanceId(plugin),
                          PersistentDataType.STRING,
                          instanceId.toString());
                });

    registry.put(
        instanceId,
        new SpigotModelInstanceHandle(instanceId, spawnedParts, interaction.getUniqueId()));
    return true;
  }

  /**
   * Starts a client-side animation towards the origin transform.
   *
   * @param instanceId the event instance domain ID
   * @param model the definition providing original transforms
   * @param durationTicks the length of the animation in ticks
   */
  public void startTransformAnimation(
      EventInstanceId instanceId, ModelDefinition model, int durationTicks) {
    SpigotModelInstanceHandle handle = registry.get(instanceId);
    if (handle == null) {
      return;
    }

    for (ModelPartDefinition part : model.parts()) {
      UUID entityId = handle.partEntities().get(part.partId());
      if (entityId != null) {
        Entity entity = Bukkit.getEntity(entityId);
        if (entity instanceof ItemDisplay display) {
          org.bukkit.Bukkit.getScheduler()
              .runTask(
                  plugin,
                  () -> {
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(durationTicks);

                    Transform t = part.localTransform();
                    Matrix4f matrix =
                        new Matrix4f()
                            .translation(t.tx(), t.ty(), t.tz())
                            .rotateX((float) Math.toRadians(t.rx()))
                            .rotateY((float) Math.toRadians(t.ry()))
                            .rotateZ((float) Math.toRadians(t.rz()))
                            .scale(t.sx(), t.sy(), t.sz());
                    display.setTransformationMatrix(matrix);
                  });
        }
      }
    }
  }

  /**
   * Moves a spawned model to a new location.
   *
   * @param instanceId the event instance domain ID
   * @param newLocation the target location
   * @return true if moved, false if not found
   */
  public boolean move(EventInstanceId instanceId, Location newLocation) {
    SpigotModelInstanceHandle handle = registry.get(instanceId);
    if (handle == null) {
      return false;
    }

    Entity interaction = Bukkit.getEntity(handle.interactionEntityId());
    if (interaction != null && interaction.isValid()) {
      interaction.teleport(newLocation);
    }

    for (UUID partId : handle.partEntities().values()) {
      Entity part = Bukkit.getEntity(partId);
      if (part != null && part.isValid()) {
        part.teleport(newLocation);
      }
    }
    return true;
  }

  /**
   * Removes the spawned objects for the given event instance.
   *
   * @param instanceId the event instance domain ID
   * @return true if removed, false if not found in registry
   */
  public boolean remove(EventInstanceId instanceId) {
    SpigotModelInstanceHandle handle = registry.remove(instanceId);
    if (handle == null) {
      return false;
    }

    removeEntityByUuid(handle.interactionEntityId());
    for (UUID partId : handle.partEntities().values()) {
      removeEntityByUuid(partId);
    }
    return true;
  }

  /** Gets information about a tracked model. */
  public Optional<SpigotModelInstanceHandle> getInfo(EventInstanceId instanceId) {
    return Optional.ofNullable(registry.get(instanceId));
  }

  /** Cleans up all tracked active event objects. Safe to call during plugin disable. */
  public void removeAll() {
    for (EventInstanceId id : registry.keySet()) {
      remove(id);
    }
  }

  /**
   * Restores an instance handle into the registry from discovered entities.
   *
   * @param instanceId the event instance ID
   * @param spawnedParts the discovered part entities
   * @param interactionEntityId the discovered interaction entity, if any
   */
  public void restore(
      EventInstanceId instanceId, Map<String, UUID> spawnedParts, UUID interactionEntityId) {
    if (!registry.containsKey(instanceId)) {
      registry.put(
          instanceId, new SpigotModelInstanceHandle(instanceId, spawnedParts, interactionEntityId));
    }
  }

  private void removeEntityByUuid(UUID uuid) {
    Entity entity = Bukkit.getEntity(uuid);
    if (entity != null && entity.isValid()) {
      org.bukkit.Bukkit.getScheduler().runTask(plugin, entity::remove);
    }
  }
}
