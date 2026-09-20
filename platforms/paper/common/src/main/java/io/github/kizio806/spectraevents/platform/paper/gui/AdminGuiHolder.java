package io.github.kizio806.spectraevents.platform.paper.gui;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Custom InventoryHolder marker for SpectraEvents Admin GUI screens. */
public final class AdminGuiHolder implements InventoryHolder {
  private final AdminGuiController.MenuType menuType;
  private Inventory inventory;

  private AdminGuiHolder(AdminGuiController.MenuType menuType) {
    this.menuType = menuType;
  }

  public static Inventory createInventory(
      AdminGuiController.MenuType menuType, int size, Component title) {
    AdminGuiHolder holder = new AdminGuiHolder(menuType);
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.inventory = inventory;
    return inventory;
  }

  public AdminGuiController.MenuType menuType() {
    return menuType;
  }

  @Override
  public Inventory getInventory() {
    return Objects.requireNonNull(inventory, "inventory");
  }
}
