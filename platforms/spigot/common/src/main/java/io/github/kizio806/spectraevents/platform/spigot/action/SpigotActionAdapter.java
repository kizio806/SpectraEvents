package io.github.kizio806.spectraevents.platform.spigot.action;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.application.model.runtime.ModelAnchor;
import io.github.kizio806.spectraevents.application.model.runtime.ModelRuntimeService;
import io.github.kizio806.spectraevents.application.model.runtime.RenderedModelHandle;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.visual.model.ModelId;
import io.github.kizio806.spectraevents.platform.spigot.render.SpigotModelRenderer;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpigotActionAdapter implements PlatformActionPort {
  private final Plugin plugin;
  private final Logger logger;
  private final SpigotModelRenderer renderer;
  private final BukkitAudiences adventure;
  private ModelRuntimeService modelRuntimeService;

  public SpigotActionAdapter(
      Plugin plugin, SpigotModelRenderer renderer, BukkitAudiences adventure) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.renderer = renderer;
    this.adventure = adventure;
  }

  public void setModelRuntimeService(ModelRuntimeService modelRuntimeService) {
    this.modelRuntimeService = modelRuntimeService;
  }

  @Override
  public void executeAction(
      EventInstance instance, EventRuntimeState state, ActionDefinition action) {
    try {
      switch (action.type()) {
        case "spawn_model":
          handleSpawnModel(instance, state, action);
          break;
        case "remove_model":
          handleRemoveModel(instance, action);
          break;
        case "play_sound":
          handlePlaySound(instance, action);
          break;
        case "spawn_particles":
          handleSpawnParticles(instance, action);
          break;
        case "broadcast":
        case "broadcast_message":
          handleBroadcast(instance, action);
          break;
        default:
          logger.info("Spigot adapter skipping action: " + action.type());
      }
    } catch (Exception e) {
      throw new FatalActionException(
          "Action execution failed on Spigot for type " + action.type(), e);
    }
  }

  private void handleSpawnModel(
      EventInstance instance, EventRuntimeState state, ActionDefinition action) {
    if (modelRuntimeService == null) {
      logger.warning("ModelRuntimeService not set in SpigotActionAdapter");
      return;
    }
    String modelIdStr = getStringParam(action.parameters(), "model", "meteor");
    int defaultHeightOffset = "meteor".equalsIgnoreCase(modelIdStr) ? 20 : 0;
    int heightOffset = getIntParam(action.parameters(), "height-offset", defaultHeightOffset);
    if (!action.parameters().containsKey("height-offset")
        && action.parameters().containsKey("height_offset")) {
      heightOffset = getIntParam(action.parameters(), "height_offset", defaultHeightOffset);
    }

    org.bukkit.Location loc = (org.bukkit.Location) state.platformLocation().orElse(null);
    if (loc != null) {
      org.bukkit.Location spawnLoc = loc.clone().add(0, heightOffset, 0);
      ModelAnchor anchor =
          new ModelAnchor(
              spawnLoc.getWorld().getName(),
              spawnLoc.getX(),
              spawnLoc.getY(),
              spawnLoc.getZ(),
              spawnLoc.getYaw(),
              spawnLoc.getPitch());

      modelRuntimeService.spawnModel(new ModelId(modelIdStr), anchor, instance.id());
    }
  }

  private void handleRemoveModel(EventInstance instance, ActionDefinition action) {
    if (modelRuntimeService != null) {
      for (RenderedModelHandle handle : modelRuntimeService.getActiveInstances()) {
        if (instance.id().equals(handle.ownerEventId())) {
          modelRuntimeService.removeModel(handle.runtimeId());
        }
      }
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

  private void handlePlaySound(EventInstance instance, ActionDefinition action) {
    Map<String, Object> config = action.parameters();
    String soundName = (String) config.get("sound");
    if (soundName != null) {
      try {
        org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase());
        Sound sound = org.bukkit.Registry.SOUNDS.get(key);
        if (sound != null) {
          for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), sound, 1f, 1f);
          }
        }
      } catch (IllegalArgumentException ignored) {
      }
    }
  }

  private void handleSpawnParticles(EventInstance instance, ActionDefinition action) {
    Map<String, Object> config = action.parameters();
    String particleName = (String) config.get("particle");
    if (particleName != null) {
      try {
        Particle particle = Particle.valueOf(particleName.toUpperCase());
        for (Player p : Bukkit.getOnlinePlayers()) {
          p.spawnParticle(particle, p.getLocation(), 10);
        }
      } catch (IllegalArgumentException ignored) {
      }
    }
  }

  private void handleBroadcast(EventInstance instance, ActionDefinition action) {
    Map<String, Object> config = action.parameters();
    String messageStr = (String) config.get("message");
    if (messageStr != null) {
      Component msg = MiniMessage.miniMessage().deserialize(messageStr);
      for (Player p : Bukkit.getOnlinePlayers()) {
        adventure.player(p).sendMessage(msg);
      }
    }
  }
}
