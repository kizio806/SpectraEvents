package io.github.kizio806.spectraevents.platform.sponge.v26_2.lifecycle;

import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.sponge.v26_2.SpongeBootstrap;
import io.github.kizio806.spectraevents.platform.sponge.v26_2.render.SpongeModelRenderer;
import java.util.List;

public class SpongeEntityReconciler implements PlatformEntityReconcilerPort {
  private final SpongeBootstrap plugin;
  private final SpongeModelRenderer renderer;

  public SpongeEntityReconciler(SpongeBootstrap plugin, SpongeModelRenderer renderer) {
    this.plugin = plugin;
    this.renderer = renderer;
  }

  @Override
  public List<DiscoveredEntity> scanLoadedEntities() {
    return List.of();
  }

  @Override
  public void removeEntity(Object platformEntityReference) {
    // Not implemented
  }

  @Override
  public void restoreInstance(EventInstanceId instanceId, List<DiscoveredEntity> entities) {
    // Not implemented
  }
}
