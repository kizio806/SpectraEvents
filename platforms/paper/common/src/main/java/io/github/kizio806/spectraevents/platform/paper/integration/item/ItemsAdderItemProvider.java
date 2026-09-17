package io.github.kizio806.spectraevents.platform.paper.integration.item;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderItemProvider implements CustomItemProvider {

  private final boolean available;

  public ItemsAdderItemProvider() {
    available = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
  }

  @Override
  public ItemStack resolveItem(String id, int amount) {
    if (!available) return null;
    try {
      String iaId = id.startsWith("itemsadder:") ? id.substring(11) : id;
      CustomStack stack = CustomStack.getInstance(iaId);
      if (stack != null) {
        ItemStack item = stack.getItemStack();
        item.setAmount(amount);
        return item;
      }
    } catch (NoClassDefFoundError | Exception ignored) {
    }
    return null;
  }

  @Override
  public boolean isAvailable() {
    return available;
  }
}
