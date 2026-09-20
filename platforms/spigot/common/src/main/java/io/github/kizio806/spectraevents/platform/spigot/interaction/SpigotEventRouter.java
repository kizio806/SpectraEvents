package io.github.kizio806.spectraevents.platform.spigot.interaction;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.spigot.metadata.SpigotPdcKeys;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

/** Routes Bukkit entity events into the shared event engine. */
public final class SpigotEventRouter implements Listener {
  private final EventOrchestrationService orchestrationService;
  private final EventExecutionEngine executionEngine;

  public SpigotEventRouter(
      EventOrchestrationService orchestrationService, EventExecutionEngine executionEngine) {
    this.orchestrationService = orchestrationService;
    this.executionEngine = executionEngine;
  }

  @EventHandler(ignoreCancelled = true)
  public void onInteract(PlayerInteractEntityEvent event) {
    if (handleInteraction(event.getPlayer(), event.getRightClicked())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player
        && handleInteraction(player, event.getEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onDeath(EntityDeathEvent event) {
    EventInstanceId instanceId = instanceId(event.getEntity());
    if (instanceId == null) {
      return;
    }
    try {
      orchestrationService.getEventInfo(instanceId.toString());
      Player killer = event.getEntity().getKiller();
      ExecutionContext context =
          killer == null ? ExecutionContext.EMPTY : new ExecutionContext(killer, Map.of());
      executionEngine.evaluateTrigger(
          instanceId, new ConfiguredTriggerDefinition("entity_death"), context);
    } catch (IllegalArgumentException ignored) {
      // Stale platform entity is removed by reconciliation/cleanup.
    }
  }

  private boolean handleInteraction(Player player, Entity entity) {
    EventInstanceId instanceId = instanceId(entity);
    if (instanceId == null) {
      return false;
    }
    try {
      orchestrationService.getEventInfo(instanceId.toString());
      executionEngine.evaluateTrigger(
          instanceId,
          new ConfiguredTriggerDefinition("interaction"),
          ExecutionContext.withActor(player));
      return true;
    } catch (IllegalArgumentException ignored) {
      return true;
    }
  }

  private EventInstanceId instanceId(Entity entity) {
    String raw =
        entity
            .getPersistentDataContainer()
            .get(SpigotPdcKeys.EVENT_INSTANCE_ID, PersistentDataType.STRING);
    if (raw == null) {
      return null;
    }
    try {
      return new EventInstanceId(UUID.fromString(raw));
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }
}
