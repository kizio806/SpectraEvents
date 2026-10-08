package io.github.kizio806.spectraevents.platform.paper.loot;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identifies a server-authoritative public event inventory. */
public record PaperSharedLootHolder(EventInstanceId instanceId) implements InventoryHolder {
  @Override
  public @NotNull Inventory getInventory() {
    throw new UnsupportedOperationException("Inventory is created by Bukkit");
  }
}
