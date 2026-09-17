package io.github.kizio806.spectraevents.platform.spigot.v26_2.action;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.platform.spigot.v26_2.render.SpigotModelRenderer;
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

  public SpigotActionAdapter(
      Plugin plugin, SpigotModelRenderer renderer, BukkitAudiences adventure) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.renderer = renderer;
    this.adventure = adventure;
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
    ModelDefinition modelDef = (ModelDefinition) action.parameters().get("model");
    org.bukkit.Location loc = (org.bukkit.Location) state.platformLocation().orElse(null);
    if (modelDef != null && loc != null) {
      renderer.spawn(instance.id(), modelDef, loc);
    }
  }

  private void handleRemoveModel(EventInstance instance, ActionDefinition action) {
    String modelIdStr = (String) action.parameters().get("model_id");
    if (modelIdStr != null) {
      logger.info("Spigot: remove_model requested for " + modelIdStr);
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
