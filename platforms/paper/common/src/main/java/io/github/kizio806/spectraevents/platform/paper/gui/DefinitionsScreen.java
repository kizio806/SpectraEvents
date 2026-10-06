package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders the registered event definitions screen. */
public final class DefinitionsScreen {

  public static Inventory createInventory(
      EventDefinitionRegistry definitionRegistry, LocaleCatalog locales, int requestedPage) {
    List<RegisteredEventDefinition> definitions = List.copyOf(definitionRegistry.getAll());
    int page = EventGuiPagination.pageFor(requestedPage, definitions.size());
    List<RegisteredEventDefinition> pageItems = EventGuiPagination.itemsOnPage(definitions, page);
    Map<Integer, String> slotPayloads = new HashMap<>();

    // Reserve slot 49 for the Back button; items fill rows 0–4 (slots 0–44).
    int slot = 0;
    for (RegisteredEventDefinition registered : pageItems) {
      slotPayloads.put(slot, registered.definition().id().value());
      slot++;
    }

    Inventory inv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.DEFINITIONS,
            String.valueOf(page),
            slotPayloads,
            54,
            GuiText.component(locales, "admin.gui.definitions.title", NamedTextColor.GREEN));

    int renderSlot = 0;
    for (RegisteredEventDefinition registered : pageItems) {
      inv.setItem(
          renderSlot++,
          MainScreen.createGuiItem(
              Material.PAPER,
              Component.text(registered.definition().id().value(), NamedTextColor.GREEN),
              List.of(
                  GuiText.component(
                      locales,
                      "admin.gui.definitions.source",
                      NamedTextColor.GRAY,
                      Map.of("source", registered.sourceFile())),
                  GuiText.component(
                      locales,
                      "admin.gui.definitions.initial-phase",
                      NamedTextColor.GRAY,
                      Map.of("phase", registered.definition().initialPhase().value())),
                  GuiText.component(
                      locales, "admin.gui.definitions.details", NamedTextColor.YELLOW))));
    }

    inv.setItem(
        49,
        MainScreen.createGuiItem(
            Material.BARRIER,
            GuiText.component(locales, "common.back-main", NamedTextColor.RED),
            List.of()));
    inv.setItem(
        53,
        MainScreen.createGuiItem(
            Material.CLOCK,
            GuiText.component(locales, "common.refresh", NamedTextColor.AQUA),
            List.of(
                GuiText.component(
                    locales, "admin.gui.definitions.refresh-lore", NamedTextColor.GRAY))));
    EventGuiPagination.addControls(inv, page, definitions.size(), locales);
    return inv;
  }
}
