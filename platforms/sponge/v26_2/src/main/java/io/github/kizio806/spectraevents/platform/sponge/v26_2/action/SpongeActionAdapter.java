package io.github.kizio806.spectraevents.platform.sponge.v26_2.action;

import io.github.kizio806.spectraevents.application.execution.EventRuntimeState;
import io.github.kizio806.spectraevents.application.execution.FatalActionException;
import io.github.kizio806.spectraevents.application.port.PlatformActionPort;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.platform.sponge.v26_2.SpongeBootstrap;
import io.github.kizio806.spectraevents.platform.sponge.v26_2.render.SpongeModelRenderer;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.spongepowered.api.Sponge;

public class SpongeActionAdapter implements PlatformActionPort {
  private final SpongeBootstrap plugin;
  private final SpongeModelRenderer renderer;

  public SpongeActionAdapter(SpongeBootstrap plugin, SpongeModelRenderer renderer) {
    this.plugin = plugin;
    this.renderer = renderer;
  }

  @Override
  public void executeAction(
      EventInstance instance, EventRuntimeState state, ActionDefinition action) {
    try {
      switch (action.type()) {
        case "spawn_model":
          handleSpawnModel(instance, state, action);
          break;
        case "broadcast":
        case "broadcast_message":
          handleBroadcast(instance, action);
          break;
        default:
          plugin.getLogger().info("Sponge adapter skipping action: " + action.type());
      }
    } catch (Exception e) {
      throw new FatalActionException(
          "Action execution failed on Sponge for type " + action.type(), e);
    }
  }

  private void handleSpawnModel(
      EventInstance instance, EventRuntimeState state, ActionDefinition action) {
    ModelDefinition modelDef = (ModelDefinition) action.parameters().get("model");
    Object loc = state.platformLocation().orElse(null);
    if (modelDef != null && loc != null) {
      renderer.spawn(instance.id(), modelDef, loc);
    }
  }

  private void handleBroadcast(EventInstance instance, ActionDefinition action) {
    Map<String, Object> config = action.parameters();
    String messageStr = (String) config.get("message");
    if (messageStr != null) {
      Sponge.server()
          .broadcastAudience()
          .sendMessage(MiniMessage.miniMessage().deserialize(messageStr));
    }
  }
}
