package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders the active events list screen. */
public final class ActiveEventsScreen {

  public static Inventory createInventory(
      EventInstanceRepository instanceRepository, int requestedPage) {
    List<EventInstance> instances = List.copyOf(instanceRepository.findAll());
    int page = EventGuiPagination.pageFor(requestedPage, instances.size());
    List<EventInstance> pageItems = EventGuiPagination.itemsOnPage(instances, page);
    Map<Integer, String> payloads = new HashMap<>();
    int payloadSlot = 0;
    for (EventInstance instance : pageItems) {
      payloads.put(payloadSlot++, instance.id().toString());
    }
    Inventory inv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.ACTIVE_EVENTS,
            String.valueOf(page),
            payloads,
            54,
            Component.text("Active Events", NamedTextColor.GOLD));

    int slot = 0;
    for (EventInstance instance : pageItems) {
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
                      NamedTextColor.YELLOW),
                  Component.text("Click to inspect", NamedTextColor.GREEN))));
    }

    inv.setItem(
        49,
        MainScreen.createGuiItem(
            Material.BARRIER, Component.text("Back to Main Menu", NamedTextColor.RED), List.of()));
    inv.setItem(
        53,
        MainScreen.createGuiItem(
            Material.CLOCK,
            Component.text("Refresh", NamedTextColor.AQUA),
            List.of(Component.text("Reload current instances", NamedTextColor.GRAY))));
    EventGuiPagination.addControls(inv, page, instances.size());
    return inv;
  }
}
