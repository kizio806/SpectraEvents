package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.update.UpdateInfo;
import io.github.kizio806.spectraevents.application.update.UpdateService;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders the update status screen. */
public final class UpdatesScreen {

  public static Inventory createInventory(UpdateService updateService) {
    Inventory inv =
        Bukkit.createInventory(
            null, 27, Component.text("Update System", NamedTextColor.LIGHT_PURPLE));

    UpdateInfo info = updateService.currentInfo();
    Material mat = info.updateAvailable() ? Material.NETHER_STAR : Material.EMERALD;
    Component title =
        info.updateAvailable()
            ? Component.text("Update Available!", NamedTextColor.GOLD)
            : Component.text("Up to Date", NamedTextColor.GREEN);

    inv.setItem(
        13,
        MainScreen.createGuiItem(
            mat,
            title,
            List.of(
                Component.text("Current Version: " + info.currentVersion(), NamedTextColor.GRAY),
                Component.text("Latest Version: " + info.latestVersion(), NamedTextColor.GRAY),
                Component.text(
                    "Release Notes: " + info.releaseNotes(), NamedTextColor.DARK_GRAY))));

    inv.setItem(
        22,
        MainScreen.createGuiItem(
            Material.BARRIER, Component.text("Back to Main Menu", NamedTextColor.RED), List.of()));
    return inv;
  }
}
