package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders the update status screen. */
public final class UpdatesScreen {

  public static Inventory createInventory(UpdateService updateService, LocaleCatalog locales) {
    Inventory inv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.UPDATES,
            27,
            GuiText.component(locales, "admin.gui.updates.title", NamedTextColor.LIGHT_PURPLE));

    UpdateInfo info = updateService.currentInfo();
    Material mat = info.updateAvailable() ? Material.NETHER_STAR : Material.EMERALD;
    Component title =
        info.updateAvailable()
            ? GuiText.component(locales, "admin.gui.updates.available", NamedTextColor.GOLD)
            : GuiText.component(locales, "admin.gui.updates.current", NamedTextColor.GREEN);

    inv.setItem(
        13,
        MainScreen.createGuiItem(
            mat,
            title,
            List.of(
                GuiText.component(
                    locales,
                    "admin.gui.updates.installed-version",
                    NamedTextColor.GRAY,
                    java.util.Map.of("version", info.currentVersion())),
                GuiText.component(
                    locales,
                    "admin.gui.updates.latest-version",
                    NamedTextColor.GRAY,
                    java.util.Map.of("version", info.latestVersion())),
                GuiText.component(
                    locales,
                    "admin.gui.updates.release-notes",
                    NamedTextColor.DARK_GRAY,
                    java.util.Map.of("notes", info.releaseNotes())))));

    inv.setItem(
        22,
        MainScreen.createGuiItem(
            Material.BARRIER,
            GuiText.component(locales, "common.back-main", NamedTextColor.RED),
            List.of()));
    return inv;
  }
}
