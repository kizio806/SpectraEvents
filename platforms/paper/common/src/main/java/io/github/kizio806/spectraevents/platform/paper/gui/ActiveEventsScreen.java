package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders the active events list screen. */
public final class ActiveEventsScreen {

  public static Inventory createInventory(
      EventInstanceRepository instanceRepository, LocaleCatalog locales, int requestedPage) {
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
            GuiText.component(locales, "admin.gui.active.title", NamedTextColor.GOLD));

    int slot = 0;
    for (EventInstance instance : pageItems) {
      inv.setItem(
          slot++,
          MainScreen.createGuiItem(
              Material.NETHER_STAR,
              GuiText.component(
                  locales,
                  "admin.gui.active.instance",
                  NamedTextColor.GOLD,
                  Map.of("id", instance.id().toString().substring(0, 8))),
              List.of(
                  GuiText.component(
                      locales,
                      "admin.gui.active.definition",
                      NamedTextColor.GRAY,
                      Map.of("definition", instance.definitionId().value())),
                  GuiText.component(
                      locales,
                      "admin.gui.active.state",
                      NamedTextColor.GRAY,
                      Map.of("state", instance.state().name())),
                  GuiText.component(
                      locales,
                      "admin.gui.active.phase",
                      NamedTextColor.YELLOW,
                      Map.of("phase", instance.currentPhase().map(p -> p.value()).orElse("none"))),
                  GuiText.component(locales, "admin.gui.active.inspect", NamedTextColor.GREEN))));
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
                GuiText.component(locales, "admin.gui.active.refresh-lore", NamedTextColor.GRAY))));
    EventGuiPagination.addControls(inv, page, instances.size(), locales);
    return inv;
  }
}
