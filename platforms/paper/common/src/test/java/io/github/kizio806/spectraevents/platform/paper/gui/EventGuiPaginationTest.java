package io.github.kizio806.spectraevents.platform.paper.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EventGuiPaginationTest {

  @Test
  void keepsListPagesInsideTheAvailableRange() {
    List<Integer> items = java.util.stream.IntStream.range(0, 91).boxed().toList();

    assertEquals(2, EventGuiPagination.lastPageFor(items.size()));
    assertEquals(0, EventGuiPagination.pageFor(-1, items.size()));
    assertEquals(2, EventGuiPagination.pageFor(99, items.size()));
    assertEquals(45, EventGuiPagination.itemsOnPage(items, 0).size());
    assertEquals(45, EventGuiPagination.itemsOnPage(items, 1).size());
    assertEquals(List.of(90), EventGuiPagination.itemsOnPage(items, 2));
  }

  @Test
  void usesTheFirstPageForAnEmptyList() {
    assertEquals(0, EventGuiPagination.lastPageFor(0));
    assertEquals(0, EventGuiPagination.pageFor(3, 0));
    assertEquals(List.of(), EventGuiPagination.itemsOnPage(List.of(), 0));
  }
}
