package io.github.kizio806.spectraevents.platform.paper.render;

import io.github.kizio806.spectraevents.application.model.runtime.DiscoveredModelEntity;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeId;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedPartHandle;
import io.github.kizio806.spectraevents.application.model.runtime.ResourceRole;
import io.github.kizio806.spectraevents.application.port.ModelRendererPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.BillboardMode;
import io.github.kizio806.spectraevents.core.visual.model.BlockAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.DisplayTransformMode;
import io.github.kizio806.spectraevents.core.visual.model.InteractionDefinition;
import io.github.kizio806.spectraevents.core.visual.model.InteractionId;
import io.github.kizio806.spectraevents.core.visual.model.ItemAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartDefinition;
import io.github.kizio806.spectraevents.core.visual.model.ModelPartId;
import io.github.kizio806.spectraevents.core.visual.model.ModelRenderProperties;
import io.github.kizio806.spectraevents.core.visual.model.ModelTransform;
import io.github.kizio806.spectraevents.core.visual.model.Quaternion;
import io.github.kizio806.spectraevents.core.visual.model.TextAlignment;
import io.github.kizio806.spectraevents.core.visual.model.TextAssetRef;
import io.github.kizio806.spectraevents.core.visual.model.Vector3;
import io.github.kizio806.spectraevents.platform.paper.integration.item.CustomItemProvider;
import io.github.kizio806.spectraevents.platform.paper.metadata.SpectraPdcKeys;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Production Paper platform renderer implementing ModelRendererPort using native Display &
 * Interaction entities.
 */
public class PaperModelRenderer implements ModelRendererPort {
  private final Plugin plugin;
  private final CustomItemProvider customItemProvider;
  private final Map<ModelRuntimeId, RenderedModelHandle> activeHandles = new ConcurrentHashMap<>();

  public PaperModelRenderer(Plugin plugin, CustomItemProvider customItemProvider) {
    this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    this.customItemProvider = customItemProvider;
  }

  public PaperModelRenderer(Plugin plugin) {
    this(plugin, null);
  }

  @Override
  public RenderedModelHandle spawnModel(
      ModelRuntimeId runtimeId,
      ModelDefinition definition,
      ModelAnchor anchor,
      EventInstanceId ownerEventId) {

    Objects.requireNonNull(runtimeId, "runtimeId cannot be null");
    Objects.requireNonNull(definition, "definition cannot be null");
    Objects.requireNonNull(anchor, "anchor cannot be null");

    World world = Bukkit.getWorld(anchor.worldName());
    if (world == null) {
      return null;
    }

    Location rootLoc =
        new Location(world, anchor.x(), anchor.y(), anchor.z(), anchor.yaw(), anchor.pitch());

    Map<ModelPartId, RenderedPartHandle> spawnedParts = new HashMap<>();
    Map<InteractionId, UUID> spawnedInteractions = new HashMap<>();
    List<Entity> spawnedBatch = new ArrayList<>();

    try {
      // 1. Spawn visual parts
      for (ModelPartDefinition part : definition.parts()) {
        Entity partEntity =
            spawnPartEntity(world, rootLoc, runtimeId, definition.id(), ownerEventId, part);
        if (partEntity == null) {
          throw new IllegalStateException(
              "Failed to spawn part entity for " + part.partId().value());
        }
        spawnedBatch.add(partEntity);
        spawnedParts.put(
            part.partId(), new RenderedPartHandle(part.partId(), partEntity.getUniqueId()));
      }

      // 2. Spawn interaction hitboxes
      for (InteractionDefinition interaction : definition.interactions()) {
        Vector3 offset = interaction.composedOffset();
        Location interactionLoc = rootLoc.clone().add(offset.x(), offset.y(), offset.z());

        Interaction interactionEntity =
            world.spawn(
                interactionLoc,
                Interaction.class,
                entity -> {
                  entity.setInteractionWidth(interaction.width());
                  entity.setInteractionHeight(interaction.height());

                  PersistentDataContainer pdc = entity.getPersistentDataContainer();
                  pdc.set(
                      SpectraPdcKeys.MODEL_INSTANCE_ID,
                      PersistentDataType.STRING,
                      runtimeId.value());
                  pdc.set(
                      SpectraPdcKeys.MODEL_DEFINITION_ID,
                      PersistentDataType.STRING,
                      definition.id().value());
                  pdc.set(
                      SpectraPdcKeys.MODEL_PART_ID,
                      PersistentDataType.STRING,
                      interaction.interactionId().value());
                  pdc.set(
                      SpectraPdcKeys.RESOURCE_ROLE,
                      PersistentDataType.STRING,
                      ResourceRole.INTERACTION_HITBOX.name());
                  if (ownerEventId != null) {
                    pdc.set(
                        SpectraPdcKeys.EVENT_INSTANCE_ID,
                        PersistentDataType.STRING,
                        ownerEventId.toString());
                  }
                });

        if (interactionEntity == null) {
          throw new IllegalStateException(
              "Failed to spawn interaction hitbox " + interaction.interactionId().value());
        }
        spawnedBatch.add(interactionEntity);
        spawnedInteractions.put(interaction.interactionId(), interactionEntity.getUniqueId());
      }

      RenderedModelHandle handle =
          new RenderedModelHandle(
              runtimeId, definition.id(), ownerEventId, anchor, spawnedParts, spawnedInteractions);
      activeHandles.put(runtimeId, handle);
      return handle;

    } catch (Exception e) {
      // Atomic Spawn Rollback: cleanup any already-spawned entities in this batch
      for (Entity entity : spawnedBatch) {
        if (entity != null && entity.isValid()) {
          entity.getScheduler().execute(plugin, entity::remove, null, 1);
        }
      }
      return null;
    }
  }

  private Entity spawnPartEntity(
      World world,
      Location rootLoc,
      ModelRuntimeId runtimeId,
      ModelId definitionId,
      EventInstanceId ownerEventId,
      ModelPartDefinition part) {

    ModelTransform transform = part.composedTransform();
    Matrix4f matrix = toMatrix4f(transform);

    return switch (part.type()) {
      case ITEM_DISPLAY -> {
        ItemAssetRef itemRef = (ItemAssetRef) part.visualAsset();
        ItemStack itemStack = resolveItemStack(itemRef.itemRef());

        yield world.spawn(
            rootLoc,
            ItemDisplay.class,
            entity -> {
              entity.setItemStack(itemStack);
              entity.setTransformationMatrix(matrix);
              entity.setItemDisplayTransform(toItemDisplayTransform(itemRef.transformMode()));
              applyRenderProperties(entity, part.renderProperties());
              tagPdc(
                  entity.getPersistentDataContainer(),
                  runtimeId,
                  definitionId,
                  part.partId().value(),
                  ResourceRole.VISUAL_PART,
                  ownerEventId);
            });
      }
      case BLOCK_DISPLAY -> {
        BlockAssetRef blockRef = (BlockAssetRef) part.visualAsset();
        org.bukkit.block.data.BlockData blockData = resolveBlockData(blockRef.blockStateRef());

        yield world.spawn(
            rootLoc,
            BlockDisplay.class,
            entity -> {
              entity.setBlock(blockData);
              entity.setTransformationMatrix(matrix);
              applyRenderProperties(entity, part.renderProperties());
              tagPdc(
                  entity.getPersistentDataContainer(),
                  runtimeId,
                  definitionId,
                  part.partId().value(),
                  ResourceRole.VISUAL_PART,
                  ownerEventId);
            });
      }
      case TEXT_DISPLAY -> {
        TextAssetRef textRef = (TextAssetRef) part.visualAsset();

        yield world.spawn(
            rootLoc,
            TextDisplay.class,
            entity -> {
              if (textRef.text() != null && !textRef.text().isBlank()) {
                entity.text(MiniMessage.miniMessage().deserialize(textRef.text()));
              }
              entity.setTransformationMatrix(matrix);
              entity.setAlignment(toTextAlignment(textRef.alignment()));
              entity.setLineWidth(textRef.lineWidth());
              entity.setTextOpacity((byte) textRef.textOpacity());
              entity.setShadowed(textRef.shadow());
              entity.setSeeThrough(textRef.seeThrough());
              applyRenderProperties(entity, part.renderProperties());
              tagPdc(
                  entity.getPersistentDataContainer(),
                  runtimeId,
                  definitionId,
                  part.partId().value(),
                  ResourceRole.VISUAL_PART,
                  ownerEventId);
            });
      }
    };
  }

  private void applyRenderProperties(Display display, ModelRenderProperties props) {
    display.setBillboard(toBillboardMode(props.billboard()));
    display.setShadowRadius(props.shadowRadius());
    display.setShadowStrength(props.shadowStrength());
    display.setViewRange(props.viewRange());

    if (props.brightnessBlock() >= 0 || props.brightnessSky() >= 0) {
      int block = Math.max(0, props.brightnessBlock());
      int sky = Math.max(0, props.brightnessSky());
      display.setBrightness(new Display.Brightness(block, sky));
    }
    if (props.glowColor() != null && !props.glowColor().isBlank()) {
      display.setGlowing(true);
      try {
        display.setGlowColorOverride(
            Color.fromRGB(Integer.parseInt(props.glowColor().replace("#", ""), 16)));
      } catch (Exception ignored) {
      }
    }
    if (props.interpolationDurationTicks() > 0) {
      display.setInterpolationDelay(props.interpolationDelayTicks());
      display.setInterpolationDuration(props.interpolationDurationTicks());
    }
    if (props.teleportDurationTicks() > 0) {
      display.setTeleportDuration(props.teleportDurationTicks());
    }
  }

  private void tagPdc(
      PersistentDataContainer pdc,
      ModelRuntimeId runtimeId,
      ModelId definitionId,
      String partId,
      ResourceRole role,
      EventInstanceId ownerEventId) {
    pdc.set(SpectraPdcKeys.MODEL_INSTANCE_ID, PersistentDataType.STRING, runtimeId.value());
    pdc.set(SpectraPdcKeys.MODEL_DEFINITION_ID, PersistentDataType.STRING, definitionId.value());
    pdc.set(SpectraPdcKeys.MODEL_PART_ID, PersistentDataType.STRING, partId);
    pdc.set(SpectraPdcKeys.RESOURCE_ROLE, PersistentDataType.STRING, role.name());
    if (ownerEventId != null) {
      pdc.set(SpectraPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING, ownerEventId.toString());
    }
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
        entity.teleportAsync(newLoc);
      }
    }
    for (UUID interactionUuid : handle.interactions().values()) {
      Entity entity = Bukkit.getEntity(interactionUuid);
      if (entity != null && entity.isValid()) {
        entity.teleportAsync(newLoc);
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

  @Override
  public boolean updatePartTransform(
      RenderedModelHandle handle, ModelPartId partId, ModelTransform newLocalTransform) {
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
    activeHandles.remove(handle.runtimeId());

    for (RenderedPartHandle partHandle : handle.parts().values()) {
      removeEntityUuid(partHandle.entityUuid());
    }
    for (UUID interactionUuid : handle.interactions().values()) {
      removeEntityUuid(interactionUuid);
    }
    return true;
  }

  public void cleanupInstance(EventInstanceId ownerEventId) {
    if (ownerEventId == null) return;
    for (RenderedModelHandle handle : List.copyOf(activeHandles.values())) {
      if (ownerEventId.equals(handle.ownerEventId())) {
        removeModel(handle);
      }
    }
  }

  @Override
  public void removeAll() {
    for (RenderedModelHandle handle : List.copyOf(activeHandles.values())) {
      removeModel(handle);
    }
    activeHandles.clear();
  }

  @Override
  public List<DiscoveredModelEntity> reconcileEntities() {
    List<DiscoveredModelEntity> discovered = new ArrayList<>();
    for (World world : Bukkit.getWorlds()) {
      for (Entity entity : world.getEntities()) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(SpectraPdcKeys.MODEL_INSTANCE_ID, PersistentDataType.STRING)) {
          String runtimeIdStr =
              pdc.get(SpectraPdcKeys.MODEL_INSTANCE_ID, PersistentDataType.STRING);
          String defIdStr = pdc.get(SpectraPdcKeys.MODEL_DEFINITION_ID, PersistentDataType.STRING);
          String partIdStr = pdc.get(SpectraPdcKeys.MODEL_PART_ID, PersistentDataType.STRING);
          String roleStr = pdc.get(SpectraPdcKeys.RESOURCE_ROLE, PersistentDataType.STRING);
          String eventIdStr = pdc.get(SpectraPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING);

          if (runtimeIdStr != null && defIdStr != null && partIdStr != null && roleStr != null) {
            ResourceRole role = ResourceRole.valueOf(roleStr);
            EventInstanceId eventId =
                eventIdStr != null ? new EventInstanceId(UUID.fromString(eventIdStr)) : null;

            discovered.add(
                new DiscoveredModelEntity(
                    entity.getUniqueId(),
                    ModelRuntimeId.of(runtimeIdStr),
                    new ModelId(defIdStr),
                    partIdStr,
                    role,
                    eventId));
          }
        }
      }
    }
    return discovered;
  }

  private void removeEntityUuid(UUID uuid) {
    Entity entity = Bukkit.getEntity(uuid);
    if (entity != null && entity.isValid()) {
      entity.getScheduler().execute(plugin, entity::remove, null, 1);
    }
  }

  private ItemStack resolveItemStack(String itemRef) {
    if (customItemProvider != null && customItemProvider.isAvailable()) {
      ItemStack custom = customItemProvider.resolveItem(itemRef, 1);
      if (custom != null) {
        return custom;
      }
    }
    String cleanRef =
        itemRef != null ? itemRef.replace("minecraft:", "").toUpperCase(Locale.ROOT) : "";
    Material mat = Material.matchMaterial(cleanRef);
    if (mat == null) {
      mat = Material.MAGMA_BLOCK;
    }
    return new ItemStack(mat);
  }

  private org.bukkit.block.data.BlockData resolveBlockData(String blockStateRef) {
    try {
      return Bukkit.createBlockData(blockStateRef);
    } catch (Exception e) {
      Material mat = Material.matchMaterial(blockStateRef);
      if (mat != null && mat.isBlock()) {
        return mat.createBlockData();
      }
      return Material.STONE.createBlockData();
    }
  }

  private Matrix4f toMatrix4f(ModelTransform transform) {
    Vector3 t = transform.translation();
    Quaternion r = transform.rotation();
    Vector3 s = transform.scale();
    Vector3 p = transform.pivot();

    Quaternionf q = new Quaternionf(r.x(), r.y(), r.z(), r.w());

    return new Matrix4f()
        .translation(t.x(), t.y(), t.z())
        .rotate(q)
        .scale(s.x(), s.y(), s.z())
        .translate(-p.x(), -p.y(), -p.z());
  }

  private Display.Billboard toBillboardMode(BillboardMode mode) {
    return switch (mode) {
      case FIXED -> Display.Billboard.FIXED;
      case CENTER -> Display.Billboard.CENTER;
      case VERTICAL -> Display.Billboard.VERTICAL;
      case HORIZONTAL -> Display.Billboard.HORIZONTAL;
    };
  }

  private ItemDisplay.ItemDisplayTransform toItemDisplayTransform(DisplayTransformMode mode) {
    return switch (mode) {
      case NONE -> ItemDisplay.ItemDisplayTransform.NONE;
      case THIRDPERSON_LEFTHAND -> ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND;
      case THIRDPERSON_RIGHTHAND -> ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND;
      case FIRSTPERSON_LEFTHAND -> ItemDisplay.ItemDisplayTransform.FIRSTPERSON_LEFTHAND;
      case FIRSTPERSON_RIGHTHAND -> ItemDisplay.ItemDisplayTransform.FIRSTPERSON_RIGHTHAND;
      case HEAD -> ItemDisplay.ItemDisplayTransform.HEAD;
      case GUI -> ItemDisplay.ItemDisplayTransform.GUI;
      case GROUND -> ItemDisplay.ItemDisplayTransform.GROUND;
      case FIXED -> ItemDisplay.ItemDisplayTransform.FIXED;
    };
  }

  private TextDisplay.TextAlignment toTextAlignment(TextAlignment alignment) {
    return switch (alignment) {
      case CENTER -> TextDisplay.TextAlignment.CENTER;
      case LEFT -> TextDisplay.TextAlignment.LEFT;
      case RIGHT -> TextDisplay.TextAlignment.RIGHT;
    };
  }
}
