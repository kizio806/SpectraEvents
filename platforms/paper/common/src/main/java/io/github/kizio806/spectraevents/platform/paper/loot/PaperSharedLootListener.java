package io.github.kizio806.spectraevents.platform.paper.loot;

import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.application.execution.ExecutionContext;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.platform.paper.metadata.SpectraPdcKeys;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Performs slot removal before inventory delivery; Bukkit never owns the authoritative loot. */
public final class PaperSharedLootListener implements Listener {
  private final EventInstanceRepository repository;
  private final EventExecutionEngine engine;

  public PaperSharedLootListener(EventInstanceRepository repository, EventExecutionEngine engine) {
    this.repository = repository;
    this.engine = engine;
  }

  @EventHandler(ignoreCancelled = true)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getInventory().getHolder() instanceof PaperSharedLootHolder holder)) return;
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)
        || event.getRawSlot() < 0
        || event.getRawSlot() >= event.getInventory().getSize()) return;
    var state = engine.stateStore().get(holder.instanceId()).orElse(null);
    if (state == null) return;
    var taken = state.takeSharedLootSlot(event.getRawSlot());
    if (taken.isEmpty()) return;
    repository.saveStateDurably(state);
    var stack = taken.orElseThrow();
    Material material = Material.matchMaterial(stack.material().replace("minecraft:", ""));
    if (material == null) material = Material.DIAMOND;
    player.getInventory().addItem(new ItemStack(material, stack.amount()));
    event.getInventory().setItem(event.getRawSlot(), null);
    engine.evaluateTrigger(
        holder.instanceId(),
        new CoreTriggers.LootItemTakenTrigger(),
        ExecutionContext.withActor(player, player.getUniqueId()));
    if (state.sharedLootEmpty()) {
      engine.evaluateTrigger(
          holder.instanceId(),
          new CoreTriggers.LootContainerEmptiedTrigger(),
          ExecutionContext.withActor(player, player.getUniqueId()));
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onGroundLootPickup(EntityPickupItemEvent event) {
    Item item = event.getItem();
    String rawId =
        item.getPersistentDataContainer()
            .get(SpectraPdcKeys.INSTANCE_ID, PersistentDataType.STRING);
    Integer slot =
        item.getPersistentDataContainer()
            .get(SpectraPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER);
    if (rawId == null || slot == null) return;
    if (!(event.getEntity() instanceof Player)) {
      event.setCancelled(true);
      return;
    }
    if (!consumeGroundLoot(rawId, slot)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onGroundLootDespawn(ItemDespawnEvent event) {
    Item item = event.getEntity();
    String rawId =
        item.getPersistentDataContainer()
            .get(SpectraPdcKeys.INSTANCE_ID, PersistentDataType.STRING);
    Integer slot =
        item.getPersistentDataContainer()
            .get(SpectraPdcKeys.GROUND_LOOT_SLOT, PersistentDataType.INTEGER);
    if (rawId != null && slot != null) consumeGroundLoot(rawId, slot);
  }

  private boolean consumeGroundLoot(String rawId, int slot) {
    try {
      EventInstanceId instanceId = new EventInstanceId(UUID.fromString(rawId));
      var state =
          engine
              .stateStore()
              .get(instanceId)
              .or(() -> repository.findState(instanceId))
              .orElse(null);
      if (state == null || state.takeSharedLootSlot(slot).isEmpty()) return false;
      repository.saveStateDurably(state);
      return true;
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }
}
