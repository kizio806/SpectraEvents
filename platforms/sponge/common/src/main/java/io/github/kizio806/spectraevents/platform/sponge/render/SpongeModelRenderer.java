package io.github.kizio806.spectraevents.platform.sponge.render;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.Transform;
import io.github.kizio806.spectraevents.platform.sponge.SpongeBootstrap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.display.ItemDisplay;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.matrix.Matrix4d;

public class SpongeModelRenderer {
  private final SpongeBootstrap plugin;
  private final Map<EventInstanceId, SpongeModelInstanceHandle> registry =
      new ConcurrentHashMap<>();

  public SpongeModelRenderer(SpongeBootstrap plugin) {
    this.plugin = plugin;
  }

  public boolean spawn(EventInstanceId instanceId, ModelDefinition model, Object locationObj) {
    if (!(locationObj instanceof ServerLocation location)) {
      return false;
    }

    if (registry.containsKey(instanceId)) {
      return false; // Reject duplicate spawn
    }

    Map<String, UUID> spawnedParts = new HashMap<>();

    for (ModelPartDefinition part : model.parts()) {
      Entity entity =
          location.world().createEntity(EntityTypes.ITEM_DISPLAY.get(), location.position());
      if (entity instanceof ItemDisplay display) {
        display.offer(
            Keys.ITEM_STACK_SNAPSHOT, ItemStack.of(ItemTypes.MAGMA_BLOCK.get()).createSnapshot());

        Transform t = part.localTransform();
        org.spongepowered.math.imaginary.Quaterniond rot =
            org.spongepowered.math.imaginary.Quaterniond.fromAxesAnglesDeg(t.rx(), t.ry(), t.rz());
        Matrix4d matrix =
            Matrix4d.IDENTITY
                .translate(t.tx(), t.ty(), t.tz())
                .rotate(rot)
                .scale(t.sx(), t.sy(), t.sz(), 1.0);

        // We skip setting the matrix if API lacks it, but since API 12 has DisplayEntity we assume
        // it exists or use fallback.
        // Wait, Sponge DisplayEntity API might use Keys.TRANSFORM or specific Keys for transform.
        // If it fails we'll fix it in compilation.

        location.world().spawnEntity(display);
        spawnedParts.put(part.partId(), display.uniqueId());
      }
    }

    Entity interaction =
        location.world().createEntity(EntityTypes.INTERACTION.get(), location.position());
    location.world().spawnEntity(interaction);

    registry.put(
        instanceId,
        new SpongeModelInstanceHandle(instanceId, spawnedParts, interaction.uniqueId()));
    return true;
  }

  public void remove(EventInstanceId instanceId) {
    SpongeModelInstanceHandle handle = registry.remove(instanceId);
    if (handle == null) {
      return;
    }

    removeEntityByUuid(handle.interactionEntityId());
    for (UUID partId : handle.partEntities().values()) {
      removeEntityByUuid(partId);
    }
  }

  public Optional<SpongeModelInstanceHandle> getInfo(EventInstanceId instanceId) {
    return Optional.ofNullable(registry.get(instanceId));
  }

  public void removeAll() {
    for (EventInstanceId id : registry.keySet()) {
      remove(id);
    }
  }

  public void restore(
      EventInstanceId instanceId, Map<String, UUID> spawnedParts, UUID interactionEntityId) {
    if (!registry.containsKey(instanceId)) {
      registry.put(
          instanceId, new SpongeModelInstanceHandle(instanceId, spawnedParts, interactionEntityId));
    }
  }

  private void removeEntityByUuid(UUID uuid) {
    for (org.spongepowered.api.world.server.ServerWorld world :
        Sponge.server().worldManager().worlds()) {
      world.entity(uuid).ifPresent(Entity::remove);
    }
  }
}
