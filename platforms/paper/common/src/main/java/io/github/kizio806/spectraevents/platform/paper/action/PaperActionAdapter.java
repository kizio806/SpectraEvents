package io.github.kizio806.spectraevents.platform.paper.action;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.platform.paper.integration.MiniPlaceholdersIntegration;
import io.github.kizio806.spectraevents.platform.paper.integration.item.CustomItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.ItemsAdderItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.NexoItemProvider;
import io.github.kizio806.spectraevents.platform.paper.integration.item.OraxenItemProvider;
import io.github.kizio806.spectraevents.platform.paper.lifecycle.PaperResourceCleaner;
import io.github.kizio806.spectraevents.platform.paper.metadata.SpectraPdcKeys;
import io.github.kizio806.spectraevents.platform.paper.render.PaperModelRenderer;
import io.github.kizio806.spectraevents.platform.paper.scheduler.RegionTaskScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Paper platform action adapter implementing {@link PlatformActionPort}. */
public final class PaperActionAdapter implements PlatformActionPort {
  private static final Logger LOGGER = Logger.getLogger(PaperActionAdapter.class.getName());

  private final PaperModelRenderer renderer;
  private final RegionTaskScheduler regionScheduler;
  private final PaperResourceCleaner cleaner;
  private final List<CustomItemProvider> itemProviders = new ArrayList<>();
  private ModelRuntimeService modelRuntimeService;

  public PaperActionAdapter(
      PaperModelRenderer renderer,
      RegionTaskScheduler regionScheduler,
      PaperResourceCleaner cleaner) {
    this.renderer = Objects.requireNonNull(renderer, "renderer");
    this.regionScheduler = Objects.requireNonNull(regionScheduler, "regionScheduler");
    this.cleaner = Objects.requireNonNull(cleaner, "cleaner");

    itemProviders.add(new NexoItemProvider());
    itemProviders.add(new OraxenItemProvider());
    itemProviders.add(new ItemsAdderItemProvider());
  }

  public void setModelRuntimeService(ModelRuntimeService modelRuntimeService) {
    this.modelRuntimeService = modelRuntimeService;
  }

  @Override
  public void executeAction(
      EventInstance instance, EventRuntimeState state, ActionDefinition action) {
    executeAction(instance, state, action, ExecutionContext.EMPTY);
  }

  @Override
  public void executeAction(
      EventInstance instance,
      EventRuntimeState state,
      ActionDefinition action,
      ExecutionContext context) {
    String type = action.type().toLowerCase();
    Map<String, Object> params = action.parameters();

    Location baseLoc = null;
    if (state.platformLocation().isPresent()
        && state.platformLocation().get() instanceof Location loc) {
      baseLoc = loc;
    }

    switch (type) {
      case "spawn_model":
        handleSpawnModel(instance, state, params, baseLoc);
        break;
      case "move_model":
        handleMoveModel(instance, baseLoc);
        break;
      case "remove_model":
        cleaner.cleanup(instance.id());
        break;
      case "play_sound":
        handlePlaySound(params, baseLoc);
        break;
      case "spawn_particles":
        handleSpawnParticles(params, baseLoc);
        break;
      case "give_item":
        handleGiveItem(params, context);
        break;
      case "send_message":
        handleSendMessage(params, context);
        break;
      case "broadcast_message":
      case "broadcast":
        handleBroadcastMessage(params);
        break;
      case "spawn_boss":
      case "spawn_entity":
        handleSpawnBoss(instance, state, params, baseLoc);
        break;
      default:
        LOGGER.info("Unhandled platform action type: " + type + " for instance " + instance.id());
        break;
    }
  }

  private void handleSpawnModel(
      EventInstance instance,
      EventRuntimeState state,
      Map<String, Object> params,
      Location baseLoc) {
    if (baseLoc == null) {
      throw new FatalActionException(
          "Cannot spawn model: platform location reference is null for instance " + instance.id());
    }

    String modelIdStr = getStringParam(params, "model", "meteor");
    int defaultHeightOffset = "meteor".equalsIgnoreCase(modelIdStr) ? 20 : 0;
    int heightOffset = getIntParam(params, "height-offset", defaultHeightOffset);
    if (!params.containsKey("height-offset") && params.containsKey("height_offset")) {
      heightOffset = getIntParam(params, "height_offset", defaultHeightOffset);
    }

    Location spawnLoc = baseLoc.clone().add(0, heightOffset, 0);
    ModelId modelId = new ModelId(modelIdStr);

    regionScheduler.executeAt(
        spawnLoc,
        () -> {
          if (modelRuntimeService == null) {
            LOGGER.warning("ModelRuntimeService not initialized in PaperActionAdapter");
            return;
          }

          ModelAnchor anchor =
              new ModelAnchor(
                  spawnLoc.getWorld().getName(),
                  spawnLoc.getX(),
                  spawnLoc.getY(),
                  spawnLoc.getZ(),
                  spawnLoc.getYaw(),
                  spawnLoc.getPitch());

          RenderedModelHandle handle =
              modelRuntimeService.spawnModel(modelId, anchor, instance.id());

          if (handle != null) {
            cleaner.registerCustomCleanup(
                instance.id(), () -> modelRuntimeService.removeModel(handle.runtimeId()));
          } else {
            throw new FatalActionException(
                "Failed to spawn 3D model '" + modelId.value() + "' for instance " + instance.id());
          }
        });
  }

  private void handleMoveModel(EventInstance instance, Location baseLoc) {
    if (baseLoc == null || modelRuntimeService == null) return;
    regionScheduler.executeAt(
        baseLoc,
        () -> {
          ModelAnchor anchor =
              new ModelAnchor(
                  baseLoc.getWorld().getName(),
                  baseLoc.getX(),
                  baseLoc.getY(),
                  baseLoc.getZ(),
                  baseLoc.getYaw(),
                  baseLoc.getPitch());
          for (RenderedModelHandle handle : modelRuntimeService.getActiveInstances()) {
            if (instance.id().equals(handle.ownerEventId())) {
              modelRuntimeService.updateModelTransform(handle.runtimeId(), anchor);
            }
          }
        });
  }

  private void handlePlaySound(Map<String, Object> params, Location baseLoc) {
    if (baseLoc == null) return;
    World world = baseLoc.getWorld();
    if (world == null) return;

    String soundName = getStringParam(params, "sound", "minecraft:entity.generic.explode");
    float volume = getFloatParam(params, "volume", 1.0f);
    float pitch = getFloatParam(params, "pitch", 1.0f);

    Sound sound = resolveSound(soundName);
    regionScheduler.executeAt(
        baseLoc,
        () -> {
          if (sound != null) {
            world.playSound(baseLoc, sound, volume, pitch);
          } else {
            world.playSound(baseLoc, soundName, volume, pitch);
          }
        });
  }

  private void handleSpawnParticles(Map<String, Object> params, Location baseLoc) {
    if (baseLoc == null) return;
    World world = baseLoc.getWorld();
    if (world == null) return;

    String particleName = getStringParam(params, "particle", "minecraft:explosion");
    int count = getIntParam(params, "count", 10);

    Particle particle = resolveParticle(particleName);
    regionScheduler.executeAt(
        baseLoc,
        () -> {
          if (particle != null) {
            world.spawnParticle(particle, baseLoc, count);
          }
        });
  }

  private void handleGiveItem(Map<String, Object> params, ExecutionContext context) {
    if (context == null || !(context.actor() instanceof Player player)) {
      return;
    }
    String materialName = getStringParam(params, "material", "minecraft:diamond");
    int amount = getIntParam(params, "amount", 1);

    ItemStack itemStack = null;

    for (CustomItemProvider provider : itemProviders) {
      if (provider.isAvailable()) {
        ItemStack custom = provider.resolveItem(materialName, amount);
        if (custom != null) {
          itemStack = custom;
          break;
        }
      }
    }

    if (itemStack == null) {
      Material mat = resolveMaterial(materialName);
      if (mat != null) {
        itemStack = new ItemStack(mat, amount);
      }
    }

    if (itemStack != null) {
      ItemStack finalStack = itemStack;
      regionScheduler.executeFor(
          player,
          () -> {
            player.getInventory().addItem(finalStack);
          });
    }
  }

  private void handleSendMessage(Map<String, Object> params, ExecutionContext context) {
    if (context == null || !(context.actor() instanceof CommandSender sender)) {
      return;
    }
    String msg = getStringParam(params, "message", "");
    if (!msg.isEmpty()) {
      sender.sendMessage(MiniPlaceholdersIntegration.getMiniMessage().deserialize(msg));
    }
  }

  private void handleBroadcastMessage(Map<String, Object> params) {
    String msg = getStringParam(params, "message", "");
    if (!msg.isEmpty()) {
      Bukkit.broadcast(MiniPlaceholdersIntegration.getMiniMessage().deserialize(msg));
    }
  }

  private void handleSpawnBoss(
      EventInstance instance,
      EventRuntimeState state,
      Map<String, Object> params,
      Location baseLoc) {
    if (baseLoc == null) return;

    int offsetX = getIntParam(params, "offset-x", 2);
    int offsetY = getIntParam(params, "offset-y", 0);
    int offsetZ = getIntParam(params, "offset-z", 0);
    String typeName = getStringParam(params, "entity_type", "minecraft:zombie");
    String name = getStringParam(params, "name", "<red>Boss");

    Location spawnLoc = baseLoc.clone().add(offsetX, offsetY, offsetZ);
    EntityType entityType = resolveEntityType(typeName);

    regionScheduler.executeAt(
        spawnLoc,
        () -> {
          World world = spawnLoc.getWorld();
          if (world == null) return;

          Entity entity = world.spawnEntity(spawnLoc, entityType);
          entity.customName(MiniPlaceholdersIntegration.getMiniMessage().deserialize(name));
          entity.setCustomNameVisible(true);

          entity
              .getPersistentDataContainer()
              .set(SpectraPdcKeys.INSTANCE_ID, PersistentDataType.STRING, instance.id().toString());

          state.setBossEntityId(entity.getUniqueId());

          cleaner.registerCustomCleanup(
              instance.id(),
              () -> {
                if (entity.isValid()) {
                  entity.remove();
                }
              });
        });
  }

  private Material resolveMaterial(String name) {
    try {
      String formatted = name.replace("minecraft:", "").toUpperCase();
      return Material.matchMaterial(formatted);
    } catch (Exception e) {
      return Material.DIAMOND;
    }
  }

  private EntityType resolveEntityType(String name) {
    try {
      String formatted = name.replace("minecraft:", "").toUpperCase();
      return EntityType.valueOf(formatted);
    } catch (Exception e) {
      return EntityType.ZOMBIE;
    }
  }

  @SuppressWarnings("removal")
  private Sound resolveSound(String name) {
    try {
      String formatted = name.replace("minecraft:", "").replace('.', '_').toUpperCase();
      return Sound.valueOf(formatted);
    } catch (Exception e) {
      return Sound.ENTITY_GENERIC_EXPLODE;
    }
  }

  private Particle resolveParticle(String name) {
    try {
      String formatted = name.replace("minecraft:", "").toUpperCase();
      return Particle.valueOf(formatted);
    } catch (Exception e) {
      return Particle.EXPLOSION;
    }
  }

  private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
    Object val = params.get(key);
    return val != null ? String.valueOf(val) : defaultValue;
  }

  private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
    Object val = params.get(key);
    if (val == null) return defaultValue;
    if (val instanceof Number n) return n.intValue();
    try {
      return Integer.parseInt(String.valueOf(val));
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private float getFloatParam(Map<String, Object> params, String key, float defaultValue) {
    Object val = params.get(key);
    if (val == null) return defaultValue;
    if (val instanceof Number n) return n.floatValue();
    try {
      return Float.parseFloat(String.valueOf(val));
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
    Object val = params.get(key);
    if (val == null) return defaultValue;
    if (val instanceof Boolean b) return b;
    return Boolean.parseBoolean(String.valueOf(val));
  }
}
