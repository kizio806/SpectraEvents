package io.github.kizio806.spectraevents.platform.paper.integration.item;

import com.nexomc.nexo.api.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class NexoItemProvider implements CustomItemProvider {

  private final boolean available;

  public NexoItemProvider() {
    available = Bukkit.getPluginManager().getPlugin("Nexo") != null;
  }

  @Override
  public ItemStack resolveItem(String id, int amount) {
    if (!available) return null;
    try {
      String nexoId = id.startsWith("nexo:") ? id.substring(5) : id;
      var builder = NexoItems.itemFromId(nexoId);
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
