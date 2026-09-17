package dev.spectraevents.platform.paper.gui;

import dev.spectraevents.application.integration.IntegrationRegistry;
import dev.spectraevents.application.integration.IntegrationState;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders the integrations status screen. */
public final class IntegrationsScreen {

  public static Inventory createInventory(IntegrationRegistry integrationRegistry) {
    Inventory inv =
        Bukkit.createInventory(
            null, 27, Component.text("Integrations Status", NamedTextColor.AQUA));

    int slot = 10;
    for (var entry : integrationRegistry.getAll().entrySet()) {
      if (slot >= 17) break;
      var info = entry.getValue();
      Material mat =
          info.state() == IntegrationState.ENABLED ? Material.LIME_DYE : Material.GRAY_DYE;
      inv.setItem(
          slot++,
          MainScreen.createGuiItem(
              mat,
              Component.text(info.name(), NamedTextColor.YELLOW),
              List.of(
                  Component.text(
                      "Status: " + info.state().name(),
                      info.state() == IntegrationState.ENABLED
                          ? NamedTextColor.GREEN
                          : NamedTextColor.RED),
                  Component.text(info.details(), NamedTextColor.GRAY))));
    }

    inv.setItem(
        22,
        MainScreen.createGuiItem(
            Material.BARRIER, Component.text("Back to Main Menu", NamedTextColor.RED), List.of()));
    return inv;
  }
}
