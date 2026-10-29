package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
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

/** Renders YAML-declared scalar settings and their saved overrides. */
public final class EventConfigurationScreen {
  private EventConfigurationScreen() {}

  public static Inventory createInventory(
      EventDefinitionRegistry definitionRegistry,
      PaperEventSettingsStore settingsStore,
      LocaleCatalog locales,
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
            Component.text(locales.message("admin.configuration.title"), NamedTextColor.YELLOW));

    int renderSlot = 0;
    for (RegisteredEventDefinition registered : pageItems) {
      String id = registered.definition().id().value();
      Map<String, Object> values = settingsStore.overridesFor(id);
      inventory.setItem(
          renderSlot++,
          MainScreen.createGuiItem(
              Material.REPEATER,
              Component.text(id, NamedTextColor.GOLD),
              List.of(
                  Component.text(
                      locales.message(
                          "admin.configuration.yaml-parameters",
                          Map.of("count", registered.sourceSpec().parameters().size())),
                      NamedTextColor.YELLOW),
                  Component.text(
                      values.isEmpty()
                          ? locales.message("admin.configuration.no-overrides")
                          : locales.message(
                              "admin.configuration.overrides", Map.of("values", values)),
                      NamedTextColor.GRAY),
                  Component.text(locales.message("admin.configuration.edit"), NamedTextColor.GREEN),
                  Component.text(
                      locales.message("admin.configuration.exact-command"),
                      NamedTextColor.DARK_GRAY))));
    }

    inventory.setItem(
        49,
        MainScreen.createGuiItem(
            Material.BARRIER,
            Component.text(locales.message("common.back-main"), NamedTextColor.RED),
            List.of()));
    inventory.setItem(
        53,
        MainScreen.createGuiItem(
            Material.CLOCK,
            Component.text(locales.message("common.refresh"), NamedTextColor.AQUA),
            List.of(
                Component.text(
                    locales.message("admin.configuration.refresh"), NamedTextColor.GRAY))));
    EventGuiPagination.addControls(inventory, page, definitions.size(), locales);
    return inventory;
  }
}
