package io.github.kizio806.spectraevents.platform.spigot.lifecycle;

import io.github.kizio806.spectraevents.application.port.PlatformEntityReconcilerPort;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.spigot.render.SpigotModelRenderer;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class SpigotEntityReconciler implements PlatformEntityReconcilerPort {
  private final Plugin plugin;
  private final SpigotModelRenderer renderer;

  public SpigotEntityReconciler(Plugin plugin, SpigotModelRenderer renderer) {
    this.plugin = plugin;
    this.renderer = renderer;
  }

  @Override
  public List<DiscoveredEntity> scanLoadedEntities() {
    return List.of();
  }

  @Override
  public void removeEntity(Object platformEntityReference) {
    if (platformEntityReference instanceof Entity entity) {
      entity.remove();
    }
  }

  @Override
  public void restoreInstance(EventInstanceId instanceId, List<DiscoveredEntity> entities) {
    // Basic stub
  }
}
