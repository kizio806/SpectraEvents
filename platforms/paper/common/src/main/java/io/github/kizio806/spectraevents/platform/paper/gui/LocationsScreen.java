package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.execution.EventLocation;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders operator-defined start locations. */
public final class LocationsScreen {
  private LocationsScreen() {}

  public static Inventory createInventory(
      PaperEventSettingsStore settingsStore, int requestedPage) {
    Map<String, EventLocation> locations = settingsStore.locations();
    List<Map.Entry<String, EventLocation>> entries = List.copyOf(locations.entrySet());
    int page = EventGuiPagination.pageFor(requestedPage, entries.size());
    List<Map.Entry<String, EventLocation>> pageItems =
        EventGuiPagination.itemsOnPage(entries, page);
    Map<Integer, String> payloads = new LinkedHashMap<>();
    int payloadSlot = 0;
    for (Map.Entry<String, EventLocation> entry : pageItems) {
      payloads.put(payloadSlot++, entry.getKey());
    }
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.LOCATIONS,
            String.valueOf(page),
            payloads,
            54,
            Component.text("Event Locations", NamedTextColor.AQUA));
    int slot = 0;
    for (Map.Entry<String, EventLocation> entry : pageItems) {
      EventLocation location = entry.getValue();
      inventory.setItem(
          slot++,
          MainScreen.createGuiItem(
              Material.COMPASS,
              Component.text(entry.getKey(), NamedTextColor.AQUA),
              List.of(
                  Component.text(location.world(), NamedTextColor.GRAY),
                  Component.text(
                      String.format("%.1f, %.1f, %.1f", location.x(), location.y(), location.z()),
                      NamedTextColor.GRAY),
                  Component.text("Click to remove", NamedTextColor.RED))));
    }
    inventory.setItem(
        45,
        MainScreen.createGuiItem(
            Material.LIME_DYE,
            Component.text("Save Current Location", NamedTextColor.GREEN),
            List.of(
                Component.text("Creates the next available location name", NamedTextColor.GRAY))));
    inventory.setItem(
        49,
        MainScreen.createGuiItem(
            Material.BARRIER, Component.text("Back to Main Menu", NamedTextColor.RED), List.of()));
    inventory.setItem(
        53,
        MainScreen.createGuiItem(
            Material.CLOCK,
            Component.text("Refresh", NamedTextColor.AQUA),
            List.of(Component.text("Reload saved locations", NamedTextColor.GRAY))));
    EventGuiPagination.addControls(inventory, page, entries.size());
    return inventory;
  }
}
