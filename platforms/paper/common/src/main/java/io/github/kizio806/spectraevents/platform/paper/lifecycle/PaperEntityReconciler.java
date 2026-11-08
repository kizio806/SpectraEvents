package io.github.kizio806.spectraevents.platform.paper.lifecycle;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.bukkit.lifecycle.AbstractBukkitEntityReconciler;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/** Paper entity reconciler integrating with Folia entity schedulers and PaperResourceCleaner. */
public class PaperEntityReconciler extends AbstractBukkitEntityReconciler {
  private final PaperResourceCleaner cleaner;

  public PaperEntityReconciler(Plugin plugin, PaperResourceCleaner cleaner) {
    super(plugin);
    this.cleaner = Objects.requireNonNull(cleaner, "cleaner");
  }

  @Override
  public void removeEntitySafely(Entity entity) {
    if (entity != null && entity.isValid()) {
      entity.getScheduler().execute(plugin, entity::remove, null, 1);
    }
  }

  @Override
  public void restoreInstance(EventInstanceId instanceId, List<DiscoveredEntity> entities) {
    for (DiscoveredEntity discovered : entities) {
      Object reference = discovered.platformReference();
      if (reference instanceof UUID uuid) {
        cleaner.registerCustomCleanup(
            instanceId,
            () -> {
              Entity entity = Bukkit.getEntity(uuid);
              removeEntitySafely(entity);
            });
      }
    }
  }
}
