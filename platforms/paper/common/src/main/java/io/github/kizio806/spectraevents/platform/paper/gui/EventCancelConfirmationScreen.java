package io.github.kizio806.spectraevents.platform.paper.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Explicit confirmation screen for cancellation, which triggers cleanup of runtime resources. */
public final class EventCancelConfirmationScreen {
  static final int SLOT_CONFIRM = 20;
  static final int SLOT_ABORT = 24;

  private EventCancelConfirmationScreen() {}

  public static Inventory createInventory(String instanceId) {
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.EVENT_CANCEL_CONFIRMATION,
            instanceId,
            java.util.Map.of(),
            45,
            Component.text("Confirm Cancellation", NamedTextColor.RED));
    inventory.setItem(
        SLOT_CONFIRM,
        MainScreen.createGuiItem(
            Material.LIME_CONCRETE,
            Component.text("Confirm Cancel", NamedTextColor.GREEN),
            List.of(
                Component.text("Stops the event and cleans its resources", NamedTextColor.GRAY))));
    inventory.setItem(
        SLOT_ABORT,
        MainScreen.createGuiItem(
            Material.RED_CONCRETE,
            Component.text("Keep Event", NamedTextColor.RED),
            List.of(Component.text("Return without changing the instance", NamedTextColor.GRAY))));
    return inventory;
  }
}
