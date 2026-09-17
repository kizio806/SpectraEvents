package dev.spectraevents.platform.paper.gui;

import dev.spectraevents.application.config.registry.EventDefinitionRegistry;
import dev.spectraevents.application.config.registry.RegisteredEventDefinition;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders the registered event definitions screen. */
public final class DefinitionsScreen {

  public static Inventory createInventory(EventDefinitionRegistry definitionRegistry) {
    Inventory inv =
        Bukkit.createInventory(null, 54, Component.text("Event Definitions", NamedTextColor.GREEN));

    int slot = 0;
    for (RegisteredEventDefinition registered : definitionRegistry.getAll()) {
      if (slot >= 45) break;
      inv.setItem(
          slot++,
          MainScreen.createGuiItem(
              Material.PAPER,
              Component.text(registered.definition().id().value(), NamedTextColor.GREEN),
              List.of(
                  Component.text("Source: " + registered.sourceFile(), NamedTextColor.GRAY),
                  Component.text(
                      "Initial Phase: " + registered.definition().initialPhase().value(),
                      NamedTextColor.GRAY))));
    }

    inv.setItem(
        49,
        MainScreen.createGuiItem(
            Material.BARRIER, Component.text("Back to Main Menu", NamedTextColor.RED), List.of()));
    return inv;
  }
}
