package io.github.kizio806.spectraevents.platform.bukkit.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/** Renders current state and operator actions for one event instance. */
public final class EventInstanceDetailScreen {
  static final int SLOT_BACK = 45;
  static final int SLOT_CANCEL = 49;
  static final int SLOT_REFRESH = 53;

  private EventInstanceDetailScreen() {}

  public static Inventory createInventory(
      EventInstance instance, EventExecutionEngine engine, LocaleCatalog locales) {
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.EVENT_INSTANCE_DETAIL,
            instance.id().toString(),
            Map.of(),
            54,
            GuiText.component(
                locales,
                "admin.gui.instance.title",
                NamedTextColor.GOLD,
                Map.of("event", instance.definitionId().value())));

    List<Component> details = new ArrayList<>();
    details.add(
        GuiText.component(
            locales, "admin.gui.instance.id", NamedTextColor.GRAY, Map.of("id", instance.id())));
    details.add(
        GuiText.component(
            locales,
            "admin.gui.instance.state",
            NamedTextColor.GRAY,
            Map.of("state", instance.state())));
    details.add(
        GuiText.component(
            locales,
            "admin.gui.instance.phase",
            NamedTextColor.YELLOW,
            Map.of("phase", instance.currentPhase().map(phase -> phase.value()).orElse("none"))));
    engine
        .status(instance.id())
        .ifPresent(
            status -> {
              if (status.maxHealth() > 0) {
                details.add(
                    GuiText.component(
                        locales,
                        "admin.gui.instance.health",
                        NamedTextColor.RED,
                        Map.of("current", status.currentHealth(), "maximum", status.maxHealth())));
              }
              if (status.maxHits() > 0) {
                details.add(
                    GuiText.component(
                        locales,
                        "admin.gui.instance.hits",
                        NamedTextColor.LIGHT_PURPLE,
                        Map.of("current", status.currentHits(), "maximum", status.maxHits())));
              }
              if (status.location() != null) {
                var location = status.location();
                details.add(
                    GuiText.component(
                        locales,
                        "admin.gui.instance.location",
                        NamedTextColor.AQUA,
                        Map.of(
                            "location",
                            location.world()
                                + " "
                                + Math.round(location.x())
                                + ", "
                                + Math.round(location.y())
                                + ", "
                                + Math.round(location.z()))));
              }
              if (status.timerDeadlineMillis() > 0) {
                long remaining =
                    Math.max(0L, status.timerDeadlineMillis() - System.currentTimeMillis());
                details.add(
                    GuiText.component(
                        locales,
                        "admin.gui.instance.remaining",
                        NamedTextColor.GREEN,
                        Map.of("seconds", Duration.ofMillis(remaining).toSeconds())));
              }
              if (status.locked()) {
                details.add(
                    GuiText.component(locales, "admin.gui.instance.locked", NamedTextColor.GOLD));
              }
            });

    inventory.setItem(
        22,
        MainScreen.createGuiItem(
            Material.NETHER_STAR,
            Component.text(instance.definitionId().value(), NamedTextColor.GOLD),
            details));
    inventory.setItem(
        SLOT_BACK,
        MainScreen.createGuiItem(
            Material.ARROW,
            GuiText.component(locales, "admin.gui.instance.back", NamedTextColor.YELLOW),
            List.of()));
    inventory.setItem(
        SLOT_CANCEL,
        MainScreen.createGuiItem(
            Material.REDSTONE,
            GuiText.component(locales, "admin.gui.instance.cancel", NamedTextColor.RED),
            List.of(
                GuiText.component(
                    locales, "admin.gui.instance.cancel-lore", NamedTextColor.GRAY))));
    inventory.setItem(
        SLOT_REFRESH,
        MainScreen.createGuiItem(
            Material.CLOCK,
            GuiText.component(locales, "common.refresh", NamedTextColor.AQUA),
            List.of(
                GuiText.component(
                    locales, "admin.gui.instance.refresh-lore", NamedTextColor.GRAY))));
    return inventory;
  }
}
