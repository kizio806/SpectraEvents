package io.github.kizio806.spectraevents.platform.paper.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
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

  static void addControls(Inventory inventory, int page, int itemCount) {
    int lastPage = lastPageFor(itemCount);
    if (page > 0) {
      inventory.setItem(
          SLOT_PREVIOUS,
          MainScreen.createGuiItem(
              Material.ARROW,
              Component.text("Previous page", NamedTextColor.YELLOW),
              List.of(
                  Component.text("Page " + page + " of " + (lastPage + 1), NamedTextColor.GRAY))));
    }
    if (page < lastPage) {
      inventory.setItem(
          SLOT_NEXT,
          MainScreen.createGuiItem(
              Material.ARROW,
              Component.text("Next page", NamedTextColor.YELLOW),
              List.of(
                  Component.text(
                      "Page " + (page + 2) + " of " + (lastPage + 1), NamedTextColor.GRAY))));
    }
  }
}
