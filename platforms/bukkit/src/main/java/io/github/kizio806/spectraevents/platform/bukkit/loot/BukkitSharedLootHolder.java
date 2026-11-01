package io.github.kizio806.spectraevents.platform.bukkit.loot;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Objects;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identifies the authoritative shared-loot inventory for a running event instance. */
public record BukkitSharedLootHolder(EventInstanceId instanceId) implements InventoryHolder {
  public BukkitSharedLootHolder {
    Objects.requireNonNull(instanceId, "instanceId");
  }

  @Override
  public @NotNull Inventory getInventory() {
    throw new UnsupportedOperationException("Inventory is created by Bukkit");
  }
}
