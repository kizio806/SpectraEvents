package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Renders the main admin inventory screen. */
public final class MainScreen {

  public static Inventory createInventory(LocaleCatalog locales) {
    Inventory inv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.MAIN,
            27,
            GuiText.title(locales, "admin.gui.main.title", NamedTextColor.DARK_PURPLE));

    inv.setItem(
        4,
        createGuiItem(
            Material.SPYGLASS,
            GuiText.component(locales, "admin.gui.main.dashboard", NamedTextColor.LIGHT_PURPLE),
            List.of(
                GuiText.component(locales, "admin.gui.main.dashboard-lore", NamedTextColor.GRAY))));
    inv.setItem(
        10,
        createGuiItem(
            Material.CHEST,
            GuiText.component(locales, "admin.gui.main.active-events", NamedTextColor.GOLD),
            List.of(
                GuiText.component(
                    locales, "admin.gui.main.active-events-lore", NamedTextColor.GRAY))));
    inv.setItem(
        12,
        createGuiItem(
            Material.BOOK,
            GuiText.component(locales, "admin.gui.main.definitions", NamedTextColor.GREEN),
            List.of(
                GuiText.component(
                    locales, "admin.gui.main.definitions-lore", NamedTextColor.GRAY))));
    inv.setItem(
        14,
        createGuiItem(
            Material.COMPARATOR,
            GuiText.component(locales, "admin.gui.main.integrations", NamedTextColor.AQUA),
            List.of(
                GuiText.component(
                    locales, "admin.gui.main.integrations-lore", NamedTextColor.GRAY))));
    inv.setItem(
        16,
        createGuiItem(
            Material.BEACON,
            GuiText.component(locales, "admin.gui.main.updates", NamedTextColor.LIGHT_PURPLE),
            List.of(
                GuiText.component(locales, "admin.gui.main.updates-lore", NamedTextColor.GRAY))));
    inv.setItem(
        20,
        createGuiItem(
            Material.REPEATER,
            GuiText.component(locales, "admin.gui.main.configuration", NamedTextColor.YELLOW),
            List.of(
                GuiText.component(
                    locales, "admin.gui.main.configuration-lore", NamedTextColor.GRAY))));
    inv.setItem(
        22,
        createGuiItem(
            Material.COMPASS,
            GuiText.component(locales, "admin.gui.main.locations", NamedTextColor.AQUA),
            List.of(
                GuiText.component(locales, "admin.gui.main.locations-lore", NamedTextColor.GRAY))));

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
