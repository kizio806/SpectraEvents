package io.github.kizio806.spectraevents.platform.paper.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Custom InventoryHolder marker for SpectraEvents Admin GUI screens. */
public class AdminGuiHolder implements InventoryHolder {
  private final AdminGuiController.MenuType menuType;

  public AdminGuiHolder(AdminGuiController.MenuType menuType) {
    this.menuType = menuType;
  }

  public AdminGuiController.MenuType menuType() {
    return menuType;
  }

  @Override
  public Inventory getInventory() {
    return null;
  }
}
