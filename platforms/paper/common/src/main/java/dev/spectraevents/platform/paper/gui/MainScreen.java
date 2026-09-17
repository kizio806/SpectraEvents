package dev.spectraevents.platform.paper.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Renders the main admin inventory screen. */
public final class MainScreen {

  public static Inventory createInventory() {
    Inventory inv =
        Bukkit.createInventory(
            null,
            27,
            Component.text(
                "SpectraEvents Admin Panel", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

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

    return inv;
  }

  static ItemStack createGuiItem(Material mat, Component name, List<Component> lore) {
    ItemStack item = new ItemStack(mat);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.displayName(name);
      meta.lore(lore);
      item.setItemMeta(meta);
    }
    return item;
  }
}
