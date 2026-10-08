package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Shared page calculations and controls for the operator list screens. */
final class EventGuiPagination {
  static final int ITEMS_PER_PAGE = 45;
  static final int SLOT_PREVIOUS = 46;
  static final int SLOT_NEXT = 47;

  private EventGuiPagination() {}

  static int pageFor(int requestedPage, int itemCount) {
    return Math.max(0, Math.min(requestedPage, lastPageFor(itemCount)));
  }

  static int lastPageFor(int itemCount) {
    return Math.max(0, (Math.max(0, itemCount) - 1) / ITEMS_PER_PAGE);
  }

  static <T> List<T> itemsOnPage(List<T> items, int page) {
    int start = page * ITEMS_PER_PAGE;
    if (start >= items.size()) {
      return List.of();
    }
    return items.subList(start, Math.min(start + ITEMS_PER_PAGE, items.size()));
  }

  static void addControls(Inventory inventory, int page, int itemCount, LocaleCatalog locales) {
    int lastPage = lastPageFor(itemCount);
    if (page > 0) {
      inventory.setItem(
          SLOT_PREVIOUS,
          MainScreen.createGuiItem(
              Material.ARROW,
              GuiText.component(locales, "admin.gui.pagination.previous", NamedTextColor.YELLOW),
              List.of(
                  GuiText.component(
                      locales,
                      "admin.gui.pagination.progress",
                      NamedTextColor.GRAY,
                      java.util.Map.of("current", page, "total", lastPage + 1)))));
    }
    if (page < lastPage) {
      inventory.setItem(
          SLOT_NEXT,
          MainScreen.createGuiItem(
              Material.ARROW,
              GuiText.component(locales, "admin.gui.pagination.next", NamedTextColor.YELLOW),
              List.of(
                  GuiText.component(
                      locales,
                      "admin.gui.pagination.progress",
                      NamedTextColor.GRAY,
                      java.util.Map.of("current", page + 2, "total", lastPage + 1)))));
    }
  }
}
