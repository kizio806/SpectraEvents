package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.platform.paper.config.PaperEventSettingsStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders operator-editable runtime overrides for one event definition. */
public final class EventConfigurationDetailScreen {
  static final int SLOT_PROFILE = 45;
  static final int SLOT_BACK = 49;
  static final int SLOT_REFRESH = 53;

  private EventConfigurationDetailScreen() {}

  public static Inventory createInventory(
      String definitionId, PaperEventSettingsStore settingsStore) {
    String profile = settingsStore.profileFor(definitionId);
    Map<String, Object> settings = settingsStore.settingsFor(definitionId, profile);
    Map<Integer, String> payloads = new LinkedHashMap<>();
    int slot = 10;
    for (String parameter : settings.keySet()) {
      if (slot >= 36) {
        break;
      }
      payloads.put(slot++, parameter);
    }
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.EVENT_CONFIGURATION_DETAIL,
            definitionId,
            payloads,
            54,
            Component.text("Configure: " + definitionId, NamedTextColor.YELLOW));

    for (Map.Entry<Integer, String> entry : payloads.entrySet()) {
      String parameter = entry.getValue();
      Object value = settings.get(parameter);
      inventory.setItem(
          entry.getKey(),
          MainScreen.createGuiItem(
              Material.COMPARATOR,
              Component.text(parameter, NamedTextColor.GOLD),
              List.of(
                  Component.text("Current: " + value, NamedTextColor.YELLOW),
                  Component.text("Left click: increase", NamedTextColor.GREEN),
                  Component.text("Right click: decrease", NamedTextColor.RED),
                  Component.text(
                      "For an exact value use /spectraevents event config",
                      NamedTextColor.DARK_GRAY))));
    }
    inventory.setItem(
        SLOT_PROFILE,
        MainScreen.createGuiItem(
            Material.REPEATER,
            Component.text("Profile: " + profile, NamedTextColor.AQUA),
            List.of(Component.text("Click to cycle easy, normal, hard", NamedTextColor.GRAY))));
    inventory.setItem(
        SLOT_BACK,
        MainScreen.createGuiItem(
            Material.BARRIER,
            Component.text("Back to Configuration", NamedTextColor.RED),
            List.of()));
    inventory.setItem(
        SLOT_REFRESH,
        MainScreen.createGuiItem(
            Material.CLOCK,
            Component.text("Refresh", NamedTextColor.AQUA),
            List.of(Component.text("Reload saved overrides", NamedTextColor.GRAY))));
    return inventory;
  }
}
