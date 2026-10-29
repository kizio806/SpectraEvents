package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Explicit confirmation screen for deleting a named event location. */
public final class LocationRemoveConfirmationScreen {
  static final int SLOT_CONFIRM = 20;
  static final int SLOT_ABORT = 24;

  private LocationRemoveConfirmationScreen() {}

  public static Inventory createInventory(String locationName, LocaleCatalog locales) {
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.LOCATION_REMOVE_CONFIRMATION,
            locationName,
            java.util.Map.of(),
            45,
            GuiText.component(locales, "admin.gui.remove-location.title", NamedTextColor.RED));
    inventory.setItem(
        SLOT_CONFIRM,
        MainScreen.createGuiItem(
            Material.LIME_CONCRETE,
            GuiText.component(
                locales,
                "admin.gui.remove-location.confirm",
                NamedTextColor.GREEN,
                java.util.Map.of("location", locationName)),
            List.of(
                GuiText.component(
                    locales, "admin.gui.remove-location.confirm-lore", NamedTextColor.GRAY))));
    inventory.setItem(
        SLOT_ABORT,
        MainScreen.createGuiItem(
            Material.RED_CONCRETE,
            GuiText.component(locales, "admin.gui.remove-location.abort", NamedTextColor.RED),
            List.of(
                GuiText.component(
                    locales, "admin.gui.remove-location.abort-lore", NamedTextColor.GRAY))));
    return inventory;
  }
}
