package io.github.kizio806.spectraevents.platform.paper.integration.item;

import org.bukkit.inventory.ItemStack;

public interface CustomItemProvider {
  /**
   * Resolves a custom item stack.
   *
   * @param id the custom item ID (e.g. nexo:my_item)
   * @param amount the amount
   * @return the ItemStack, or null if not found/unsupported
   */
  ItemStack resolveItem(String id, int amount);

  /**
   * @return true if this provider is active and available
   */
  boolean isAvailable();
}
