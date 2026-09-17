package dev.spectraevents.platform.paper.integration.item;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class OraxenItemProvider implements CustomItemProvider {

  private final boolean available;

  public OraxenItemProvider() {
    available = Bukkit.getPluginManager().getPlugin("Oraxen") != null;
  }

  @Override
  public ItemStack resolveItem(String id, int amount) {
    if (!available) return null;
    try {
      String oraxenId = id.startsWith("oraxen:") ? id.substring(7) : id;
      var builder = OraxenItems.getItemById(oraxenId);
      if (builder != null) {
        ItemStack item = builder.build();
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
