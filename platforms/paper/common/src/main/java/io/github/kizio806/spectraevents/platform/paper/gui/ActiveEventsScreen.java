package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders the active events list screen. */
public final class ActiveEventsScreen {

  public static Inventory createInventory(EventInstanceRepository instanceRepository) {
    Inventory inv =
        Bukkit.createInventory(null, 54, Component.text("Active Events", NamedTextColor.GOLD));

    int slot = 0;
    for (EventInstance instance : instanceRepository.findAll()) {
      if (slot >= 45) break;
      inv.setItem(
          slot++,
          MainScreen.createGuiItem(
              Material.NETHER_STAR,
              Component.text(
                  "Instance " + instance.id().toString().substring(0, 8), NamedTextColor.GOLD),
              List.of(
                  Component.text(
                      "Definition: " + instance.definitionId().value(), NamedTextColor.GRAY),
                  Component.text("State: " + instance.state().name(), NamedTextColor.GRAY),
                  Component.text(
                      "Phase: " + instance.currentPhase().map(p -> p.value()).orElse("none"),
                      NamedTextColor.YELLOW))));
    }

    inv.setItem(
        49,
        MainScreen.createGuiItem(
            Material.BARRIER, Component.text("Back to Main Menu", NamedTextColor.RED), List.of()));
    return inv;
  }
}
