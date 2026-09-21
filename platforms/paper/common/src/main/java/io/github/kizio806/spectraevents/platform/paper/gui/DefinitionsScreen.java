package io.github.kizio806.spectraevents.platform.paper.gui;

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

  public static Inventory createInventory(EventDefinitionRegistry definitionRegistry) {
    Map<Integer, String> slotPayloads = new HashMap<>();

    // Reserve slot 49 for the Back button; items fill rows 0–4 (slots 0–44).
    int slot = 0;
    for (RegisteredEventDefinition registered : definitionRegistry.getAll()) {
      if (slot >= 45) break;
      slotPayloads.put(slot, registered.definition().id().value());
      slot++;
    }

    Inventory inv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.DEFINITIONS,
            null,
            slotPayloads,
            54,
            Component.text("Event Definitions", NamedTextColor.GREEN));

    int renderSlot = 0;
    for (RegisteredEventDefinition registered : definitionRegistry.getAll()) {
      if (renderSlot >= 45) break;
      inv.setItem(
          renderSlot++,
          MainScreen.createGuiItem(
              Material.PAPER,
              Component.text(registered.definition().id().value(), NamedTextColor.GREEN),
              List.of(
                  Component.text("Source: " + registered.sourceFile(), NamedTextColor.GRAY),
                  Component.text(
                      "Initial Phase: " + registered.definition().initialPhase().value(),
                      NamedTextColor.GRAY),
                  Component.text("\u00bb Click to view details", NamedTextColor.YELLOW))));
    }

    inv.setItem(
        49,
        MainScreen.createGuiItem(
            Material.BARRIER, Component.text("Back to Main Menu", NamedTextColor.RED), List.of()));
    return inv;
  }
}
