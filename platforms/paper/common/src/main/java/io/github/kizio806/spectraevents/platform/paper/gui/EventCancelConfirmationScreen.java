package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Explicit confirmation screen for cancellation, which triggers cleanup of runtime resources. */
public final class EventCancelConfirmationScreen {
  static final int SLOT_CONFIRM = 20;
  static final int SLOT_ABORT = 24;

  private EventCancelConfirmationScreen() {}

  public static Inventory createInventory(String instanceId, LocaleCatalog locales) {
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.EVENT_CANCEL_CONFIRMATION,
            instanceId,
            java.util.Map.of(),
            45,
            GuiText.component(locales, "admin.gui.cancel.title", NamedTextColor.RED));
    inventory.setItem(
        SLOT_CONFIRM,
        MainScreen.createGuiItem(
            Material.LIME_CONCRETE,
            GuiText.component(locales, "admin.gui.cancel.confirm", NamedTextColor.GREEN),
            List.of(
                GuiText.component(locales, "admin.gui.cancel.confirm-lore", NamedTextColor.GRAY))));
    inventory.setItem(
        SLOT_ABORT,
        MainScreen.createGuiItem(
            Material.RED_CONCRETE,
            GuiText.component(locales, "admin.gui.cancel.abort", NamedTextColor.RED),
            List.of(
                GuiText.component(locales, "admin.gui.cancel.abort-lore", NamedTextColor.GRAY))));
    return inventory;
  }
}
