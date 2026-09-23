package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders saved profiles and overrides for each registered event definition. */
public final class EventConfigurationScreen {
  private EventConfigurationScreen() {}

  public static Inventory createInventory(
      EventDefinitionRegistry definitionRegistry,
      PaperEventSettingsStore settingsStore,
      int requestedPage) {
    List<RegisteredEventDefinition> definitions = List.copyOf(definitionRegistry.getAll());
    int page = EventGuiPagination.pageFor(requestedPage, definitions.size());
    List<RegisteredEventDefinition> pageItems = EventGuiPagination.itemsOnPage(definitions, page);
    Map<Integer, String> payloads = new HashMap<>();
    int slot = 0;
    for (RegisteredEventDefinition registered : pageItems) {
      payloads.put(slot++, registered.definition().id().value());
    }

    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.EVENT_CONFIGURATION,
            String.valueOf(page),
            payloads,
            54,
            Component.text("Event Configuration", NamedTextColor.YELLOW));

    int renderSlot = 0;
    for (RegisteredEventDefinition registered : pageItems) {
      String id = registered.definition().id().value();
      String profile = settingsStore.profileFor(id);
      Map<String, Object> values = settingsStore.settingsFor(id, profile);
      inventory.setItem(
          renderSlot++,
          MainScreen.createGuiItem(
              Material.REPEATER,
              Component.text(id, NamedTextColor.GOLD),
              List.of(
                  Component.text("Profile: " + profile, NamedTextColor.YELLOW),
                  Component.text(
                      values.isEmpty() ? "No saved overrides" : values.toString(),
                      NamedTextColor.GRAY),
                  Component.text("Click to edit profile and overrides", NamedTextColor.GREEN),
                  Component.text(
                      "Exact values are also available by command", NamedTextColor.DARK_GRAY))));
    }

    inventory.setItem(
        49,
        MainScreen.createGuiItem(
            Material.BARRIER, Component.text("Back to Main Menu", NamedTextColor.RED), List.of()));
    inventory.setItem(
        53,
        MainScreen.createGuiItem(
            Material.CLOCK,
            Component.text("Refresh", NamedTextColor.AQUA),
            List.of(Component.text("Reload saved profiles and overrides", NamedTextColor.GRAY))));
    EventGuiPagination.addControls(inventory, page, definitions.size());
    return inventory;
  }
}
