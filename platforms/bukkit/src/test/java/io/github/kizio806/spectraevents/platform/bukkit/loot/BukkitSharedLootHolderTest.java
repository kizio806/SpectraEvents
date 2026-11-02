package io.github.kizio806.spectraevents.platform.bukkit.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BukkitSharedLootHolderTest {

  @Test
  void holdsInstanceIdAndRejectsNull() {
    assertThrows(NullPointerException.class, () -> new BukkitSharedLootHolder(null));

    EventInstanceId id = new EventInstanceId(UUID.randomUUID());
    BukkitSharedLootHolder holder = new BukkitSharedLootHolder(id);
    assertEquals(id, holder.instanceId());
  }

  @Test
  void getInventoryThrowsUnsupportedOperationException() {
    BukkitSharedLootHolder holder =
        new BukkitSharedLootHolder(new EventInstanceId(UUID.randomUUID()));
    assertThrows(UnsupportedOperationException.class, holder::getInventory);
  }
}
