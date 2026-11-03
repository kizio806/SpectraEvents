package io.github.kizio806.spectraevents.platform.spigot.lifecycle;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.lifecycle.AbstractBukkitEntityReconciler;
import io.github.kizio806.spectraevents.platform.spigot.action.SpigotActionAdapter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/** Spigot entity reconciler with synchronous entity cleanup. */
public class SpigotEntityReconciler extends AbstractBukkitEntityReconciler {
  private final SpigotActionAdapter actionAdapter;

  public SpigotEntityReconciler(Plugin plugin, SpigotActionAdapter actionAdapter) {
    super(plugin);
    this.actionAdapter = Objects.requireNonNull(actionAdapter, "actionAdapter");
  }

  public Plugin getPlugin() {
    return plugin;
  }

  @Override
  public void removeEntitySafely(Entity entity) {
    if (entity != null && entity.isValid()) {
      entity.remove();
    }
  }

  @Override
  public void restoreInstance(EventInstanceId instanceId, List<DiscoveredEntity> entities) {
    for (DiscoveredEntity discovered : entities) {
      Object reference = discovered.platformReference();
      if (reference instanceof UUID uuid) {
        actionAdapter.registerRecoveredEntity(instanceId, uuid);
      }
    }
  }
}
