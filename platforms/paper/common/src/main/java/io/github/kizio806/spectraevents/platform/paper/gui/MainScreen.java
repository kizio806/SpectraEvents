package io.github.kizio806.spectraevents.platform.paper.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Renders the main admin inventory screen. */
public final class MainScreen {

  public static Inventory createInventory() {
    Inventory inv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.MAIN,
            27,
            Component.text(
                "SpectraEvents Admin Panel", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

    inv.setItem(
        4,
        createGuiItem(
            Material.SPYGLASS,
            Component.text("Dashboard", NamedTextColor.LIGHT_PURPLE),
            List.of(Component.text("View event and system status", NamedTextColor.GRAY))));
    inv.setItem(
        10,
        createGuiItem(
            Material.CHEST,
            Component.text("Active Events", NamedTextColor.GOLD),
            List.of(
                Component.text("View and manage running event instances", NamedTextColor.GRAY))));
    inv.setItem(
        12,
        createGuiItem(
            Material.BOOK,
            Component.text("Definitions", NamedTextColor.GREEN),
            List.of(Component.text("View and manage event definitions", NamedTextColor.GRAY))));
    inv.setItem(
        14,
        createGuiItem(
            Material.COMPARATOR,
            Component.text("Integrations", NamedTextColor.AQUA),
            List.of(
                Component.text("Status of optional plugin integrations", NamedTextColor.GRAY))));
    inv.setItem(
        16,
        createGuiItem(
            Material.BEACON,
            Component.text("Update System", NamedTextColor.LIGHT_PURPLE),
            List.of(Component.text("Check for updates and release info", NamedTextColor.GRAY))));
    inv.setItem(
        20,
        createGuiItem(
            Material.REPEATER,
            Component.text("Event Configuration", NamedTextColor.YELLOW),
            List.of(Component.text("Set difficulty profiles and overrides", NamedTextColor.GRAY))));
    inv.setItem(
        22,
        createGuiItem(
            Material.COMPASS,
            Component.text("Event Locations", NamedTextColor.AQUA),
            List.of(Component.text("Save and review event start locations", NamedTextColor.GRAY))));

    return inv;
  }

  static ItemStack createGuiItem(Material mat, Component name, List<Component> lore) {
    ItemStack item = new ItemStack(mat);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.displayName(name);
      meta.lore(lore);
      if (!item.setItemMeta(meta)) {
        throw new IllegalStateException("Could not apply GUI item metadata");
      }
    }
    return item;
  }
}
