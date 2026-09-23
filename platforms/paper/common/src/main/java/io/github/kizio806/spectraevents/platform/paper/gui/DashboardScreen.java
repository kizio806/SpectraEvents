package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.integration.IntegrationRegistry;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.core.event.runtime.EventLifecycleState;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Operator overview of the currently available runtime state. */
public final class DashboardScreen {
  private DashboardScreen() {}

  public static Inventory createInventory(
      EventDefinitionRegistry definitions,
      EventInstanceRepository instances,
      IntegrationRegistry integrations) {
    int totalInstances = instances.findAll().size();
    long running =
        instances.findAll().stream()
            .filter(instance -> instance.state() == EventLifecycleState.RUNNING)
            .count();
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.DASHBOARD,
            27,
            Component.text("SpectraEvents Dashboard", NamedTextColor.DARK_PURPLE));
    inventory.setItem(
        10,
        MainScreen.createGuiItem(
            Material.NETHER_STAR,
            Component.text("Active Events: " + running, NamedTextColor.GOLD),
            List.of(Component.text(totalInstances + " persisted instances", NamedTextColor.GRAY))));
    inventory.setItem(
        12,
        MainScreen.createGuiItem(
            Material.BOOK,
            Component.text("Definitions: " + definitions.getAll().size(), NamedTextColor.GREEN),
            List.of(Component.text("Registered and ready to start", NamedTextColor.GRAY))));
    inventory.setItem(
        14,
        MainScreen.createGuiItem(
            Material.HOPPER,
            Component.text("Storage: available", NamedTextColor.GREEN),
            List.of(Component.text("SQLite repository responded", NamedTextColor.GRAY))));
    inventory.setItem(
        16,
        MainScreen.createGuiItem(
            Material.COMPARATOR,
            Component.text("Integrations: " + integrations.enabledCount(), NamedTextColor.AQUA),
            List.of(Component.text("Enabled optional integrations", NamedTextColor.GRAY))));
    inventory.setItem(
        22,
        MainScreen.createGuiItem(
            Material.CLOCK,
            Component.text("Refresh", NamedTextColor.AQUA),
            List.of(Component.text("Reload dashboard values", NamedTextColor.GRAY))));
    inventory.setItem(
        26,
        MainScreen.createGuiItem(
            Material.BARRIER, Component.text("Back to Main Menu", NamedTextColor.RED), List.of()));
    return inventory;
  }
}
