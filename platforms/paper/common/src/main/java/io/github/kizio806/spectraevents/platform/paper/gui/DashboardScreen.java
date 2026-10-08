package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Operator overview of the currently available runtime state. */
public final class DashboardScreen {
  private DashboardScreen() {}

  public static Inventory createInventory(
      EventDefinitionRegistry definitions,
      EventInstanceRepository instances,
      IntegrationRegistry integrations,
      LocaleCatalog locales) {
    int totalInstances = instances.findAll().size();
    long running =
        instances.findAll().stream()
            .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
            .count();
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.DASHBOARD,
            27,
            GuiText.component(locales, "admin.gui.dashboard.title", NamedTextColor.DARK_PURPLE));
    inventory.setItem(
        10,
        MainScreen.createGuiItem(
            Material.NETHER_STAR,
            GuiText.component(
                locales,
                "admin.gui.dashboard.active-events",
                NamedTextColor.GOLD,
                java.util.Map.of("count", running)),
            List.of(
                GuiText.component(
                    locales,
                    "admin.gui.dashboard.persisted-instances",
                    NamedTextColor.GRAY,
                    java.util.Map.of("count", totalInstances)))));
    inventory.setItem(
        12,
        MainScreen.createGuiItem(
            Material.BOOK,
            GuiText.component(
                locales,
                "admin.gui.dashboard.definitions",
                NamedTextColor.GREEN,
                java.util.Map.of("count", definitions.getAll().size())),
            List.of(
                GuiText.component(
                    locales, "admin.gui.dashboard.definitions-lore", NamedTextColor.GRAY))));
    inventory.setItem(
        14,
        MainScreen.createGuiItem(
            Material.HOPPER,
            GuiText.component(locales, "admin.gui.dashboard.storage", NamedTextColor.GREEN),
            List.of(
                GuiText.component(
                    locales, "admin.gui.dashboard.storage-lore", NamedTextColor.GRAY))));
    inventory.setItem(
        16,
        MainScreen.createGuiItem(
            Material.COMPARATOR,
            GuiText.component(
                locales,
                "admin.gui.dashboard.integrations",
                NamedTextColor.AQUA,
                java.util.Map.of("count", integrations.enabledCount())),
            List.of(
                GuiText.component(
                    locales, "admin.gui.dashboard.integrations-lore", NamedTextColor.GRAY))));
    inventory.setItem(
        22,
        MainScreen.createGuiItem(
            Material.CLOCK,
            GuiText.component(locales, "common.refresh", NamedTextColor.AQUA),
            List.of(
                GuiText.component(
                    locales, "admin.gui.dashboard.refresh-lore", NamedTextColor.GRAY))));
    inventory.setItem(
        26,
        MainScreen.createGuiItem(
            Material.BARRIER,
            GuiText.component(locales, "common.back-main", NamedTextColor.RED),
            List.of()));
    return inventory;
  }
}
