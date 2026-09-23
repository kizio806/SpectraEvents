package io.github.kizio806.spectraevents.platform.paper.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Explicit confirmation screen for deleting a named event location. */
public final class LocationRemoveConfirmationScreen {
  static final int SLOT_CONFIRM = 20;
  static final int SLOT_ABORT = 24;

  private LocationRemoveConfirmationScreen() {}

  public static Inventory createInventory(String locationName) {
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.LOCATION_REMOVE_CONFIRMATION,
            locationName,
            java.util.Map.of(),
            45,
            Component.text("Remove Location", NamedTextColor.RED));
    inventory.setItem(
        SLOT_CONFIRM,
        MainScreen.createGuiItem(
            Material.LIME_CONCRETE,
            Component.text("Remove " + locationName, NamedTextColor.GREEN),
            List.of(Component.text("This cannot be undone", NamedTextColor.GRAY))));
    inventory.setItem(
        SLOT_ABORT,
        MainScreen.createGuiItem(
            Material.RED_CONCRETE,
            Component.text("Keep Location", NamedTextColor.RED),
            List.of(Component.text("Return without deleting", NamedTextColor.GRAY))));
    return inventory;
  }
}
