package io.github.kizio806.spectraevents.platform.spigot.loot;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Identifies the one authoritative shared-loot inventory for a running event. */
public record SpigotSharedLootHolder(EventInstanceId instanceId) implements InventoryHolder {
  @Override
  public Inventory getInventory() {
    throw new UnsupportedOperationException("Inventory is created by Bukkit");
  }
}
