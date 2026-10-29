package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
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

    for (int i = 0; i < 9; i++) {
      inv.setItem(i, MainScreen.createFillerItem(Material.GRAY_STAINED_GLASS_PANE));
    }
    for (int i = 27; i < 36; i++) {
      inv.setItem(i, MainScreen.createFillerItem(Material.GRAY_STAINED_GLASS_PANE));
    }

    int slot = 9;
    for (var entry : integrationRegistry.getAll().entrySet()) {
      if (slot >= 27) break;
      var info = entry.getValue();
      Material mat =
          switch (info.state()) {
            case ACTIVE -> Material.LIME_DYE;
            case AVAILABLE -> Material.YELLOW_DYE;
            case DEGRADED -> Material.ORANGE_DYE;
            case FAILED -> Material.RED_DYE;
            case ABSENT -> Material.GRAY_DYE;
          };
      NamedTextColor statusColor =
          switch (info.state()) {
            case ACTIVE -> NamedTextColor.GREEN;
            case AVAILABLE -> NamedTextColor.YELLOW;
            case DEGRADED -> NamedTextColor.GOLD;
            case FAILED -> NamedTextColor.RED;
            case ABSENT -> NamedTextColor.GRAY;
          };
      inv.setItem(
          slot++,
          MainScreen.createGuiItem(
              mat,
              Component.text(info.name(), NamedTextColor.YELLOW),
              List.of(
                  GuiText.component(
                      locales,
                      "admin.gui.integrations.status",
                      statusColor,
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
