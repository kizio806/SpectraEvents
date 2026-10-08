package io.github.kizio806.spectraevents.platform.paper.interaction;

import io.github.kizio806.spectraevents.application.config.compiled.ConfiguredTriggerDefinition;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.service.EventOrchestrationService;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.paper.metadata.SpectraPdcKeys;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Routes native EntityDeathEvents to generic execution engine or specific delegates based on PDC.
 */
public final class PaperEntityDeathRouter implements Listener {
  private final EventOrchestrationService orchestrationService;
  private final EventExecutionEngine executionEngine;
  private final Map<String, EventEntityDeathDelegate> delegates = new ConcurrentHashMap<>();

  public PaperEntityDeathRouter(
      EventOrchestrationService orchestrationService, EventExecutionEngine executionEngine) {
    this.orchestrationService = orchestrationService;
    this.executionEngine = executionEngine;
  }

  public PaperEntityDeathRouter(EventOrchestrationService orchestrationService) {
    this(orchestrationService, null);
  }

  public void registerDelegate(String definitionId, EventEntityDeathDelegate delegate) {
    delegates.put(definitionId, delegate);
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();
    PersistentDataContainer pdc = entity.getPersistentDataContainer();

    String instanceIdStr = pdc.get(SpectraPdcKeys.INSTANCE_ID, PersistentDataType.STRING);
    if (instanceIdStr == null) {
      return;
    }

    try {
      EventInstanceId instanceId = new EventInstanceId(UUID.fromString(instanceIdStr));
      EventInstance instance = orchestrationService.getEventInfo(instanceId.toString());

      String waveId = pdc.get(SpectraPdcKeys.WAVE_ID, PersistentDataType.STRING);
      if (waveId != null && executionEngine != null) {
        org.bukkit.entity.Player killer = entity.getKiller();
        ExecutionContext context =
            killer != null
                ? new ExecutionContext(killer, killer.getUniqueId(), Map.of())
                : ExecutionContext.EMPTY;
        executionEngine.recordWaveEntityDeath(instanceId, waveId, entity.getUniqueId(), context);
      }

      EventEntityDeathDelegate delegate = delegates.get(instance.definitionId().value());
      if (delegate != null) {
        delegate.handleEntityDeath(entity, instance);
      } else if (executionEngine != null) {
        org.bukkit.entity.Player killer = entity.getKiller();
        ExecutionContext ctx =
            killer != null
                ? new ExecutionContext(killer, killer.getUniqueId(), Map.of())
                : ExecutionContext.EMPTY;
        executionEngine.evaluateTrigger(
            instanceId, new ConfiguredTriggerDefinition("entity_death"), ctx);
      }
    } catch (IllegalArgumentException e) {
      return;
    }
  }
}
