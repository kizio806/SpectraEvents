package io.github.kizio806.spectraevents.platform.sponge.v26_2.render;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.visual.model.ModelDefinition;
import io.github.kizio806.spectraevents.platform.sponge.v26_2.SpongeBootstrap;

public class SpongeModelRenderer {
  private final SpongeBootstrap plugin;

  public SpongeModelRenderer(SpongeBootstrap plugin) {
    this.plugin = plugin;
  }

  public boolean spawn(EventInstanceId instanceId, ModelDefinition model, Object location) {
    // Sponge model renderer not fully implemented due to API mapping limitations
    plugin.getLogger().info("SpongeModelRenderer: spawn() called for " + model.id().value());
    return true;
  }

  public void remove(EventInstanceId instanceId) {
    plugin.getLogger().info("SpongeModelRenderer: remove() called for " + instanceId.toString());
  }
}
