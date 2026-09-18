package io.github.kizio806.spectraevents.platform.paper.interaction;

import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.paper.metadata.SpectraPdcKeys;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Generic interaction router that listens to native Bukkit entity interactions, extracts PDC data,
 * resolves the core domain event instance, and delegates to a specific handler.
 */
public final class PaperInteractionRouter implements Listener {
  private final EventOrchestrationService orchestrationService;
  private final EventExecutionEngine executionEngine;
  private final Map<String, EventInteractionDelegate> delegates = new ConcurrentHashMap<>();

  public PaperInteractionRouter(
      EventOrchestrationService orchestrationService, EventExecutionEngine executionEngine) {
    this.orchestrationService = orchestrationService;
    this.executionEngine = executionEngine;
  }

  public PaperInteractionRouter(EventOrchestrationService orchestrationService) {
    this(orchestrationService, null);
  }

  /** Registers a delegate for a specific event definition ID (e.g., "meteor"). */
  public void registerDelegate(String definitionId, EventInteractionDelegate delegate) {
    delegates.put(definitionId, delegate);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    if (handleEntityEvent(event.getPlayer(), event.getRightClicked())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player) {
      if (handleEntityEvent(player, event.getEntity())) {
        event.setCancelled(true);
      }
    }
  }

  private boolean handleEntityEvent(Player player, Entity clicked) {
    PersistentDataContainer pdc = clicked.getPersistentDataContainer();
    String instanceIdStr = pdc.get(SpectraPdcKeys.INSTANCE_ID, PersistentDataType.STRING);
    if (instanceIdStr == null) {
      return false;
    }

    try {
      EventInstanceId instanceId = new EventInstanceId(UUID.fromString(instanceIdStr));
      EventInstance instance = orchestrationService.getEventInfo(instanceId.toString());

      String modelIdStr = pdc.get(SpectraPdcKeys.MODEL_ID, PersistentDataType.STRING);
      String phase = instance.currentPhase().map(p -> p.value()).orElse("none");

      EventInteractionDelegate delegate = delegates.get(instance.definitionId().value());
      if (delegate != null) {
        delegate.handleInteraction(player, instance, phase, modelIdStr);
      } else if (executionEngine != null) {
        boolean handled =
            executionEngine.evaluateTrigger(
                instanceId,
                new io.github.kizio806.spectraevents.application.config.compiled
                    .ConfiguredTriggerDefinition("interaction"));
        if (handled) {
          executionEngine
              .stateStore()
              .get(instanceId)
              .ifPresent(
                  state -> {
                    state.recordDamage(player.getUniqueId(), 1);
                    if (state.isLocked()) {
                      long remaining = state.lockedUntilMillis() - System.currentTimeMillis();
                      player.sendMessage(
                          Component.text(
                                  String.format(
                                      "Event is locked for another %.1fs.", remaining / 1000.0f))
                              .color(NamedTextColor.RED));
                    }
                  });
        }
      }
      return true;

    } catch (IllegalArgumentException e) {
      player.sendMessage(
          Component.text(
              "Entity tied to unknown/invalid instance: " + instanceIdStr, NamedTextColor.RED));
      return true;
    }
  }
}
