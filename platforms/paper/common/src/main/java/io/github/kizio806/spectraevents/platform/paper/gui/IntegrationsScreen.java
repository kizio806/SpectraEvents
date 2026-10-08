package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationState;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders the integrations status screen. */
public final class IntegrationsScreen {

  public static Inventory createInventory(
      IntegrationRegistry integrationRegistry, LocaleCatalog locales) {
    Inventory inv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.INTEGRATIONS,
            36,
            GuiText.component(locales, "admin.gui.integrations.title", NamedTextColor.AQUA));

    int slot = 9;
    for (var entry : integrationRegistry.getAll().entrySet()) {
      if (slot >= 27) break;
      var info = entry.getValue();
      Material mat =
          info.state() == IntegrationState.ENABLED ? Material.LIME_DYE : Material.GRAY_DYE;
      inv.setItem(
          slot++,
          MainScreen.createGuiItem(
              mat,
              Component.text(info.name(), NamedTextColor.YELLOW),
              List.of(
                  GuiText.component(
                      locales,
                      "admin.gui.integrations.status",
                      info.state() == IntegrationState.ENABLED
                          ? NamedTextColor.GREEN
                          : NamedTextColor.RED,
                      java.util.Map.of("status", info.state().name())),
                  Component.text(info.details(), NamedTextColor.GRAY))));
    }

    inv.setItem(
        31,
        MainScreen.createGuiItem(
            Material.BARRIER,
            GuiText.component(locales, "common.back-main", NamedTextColor.RED),
            List.of()));
    return inv;
  }
}
