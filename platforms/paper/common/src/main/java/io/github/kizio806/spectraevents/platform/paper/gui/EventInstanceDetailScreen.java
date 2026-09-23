package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.execution.EventExecutionEngine;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

  public static Inventory createInventory(EventInstance instance, EventExecutionEngine engine) {
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.EVENT_INSTANCE_DETAIL,
            instance.id().toString(),
            java.util.Map.of(),
            54,
            Component.text("Event: " + instance.definitionId().value(), NamedTextColor.GOLD));

    List<Component> details = new ArrayList<>();
    details.add(Component.text("ID: " + instance.id(), NamedTextColor.GRAY));
    details.add(Component.text("State: " + instance.state(), NamedTextColor.GRAY));
    details.add(
        Component.text(
            "Phase: " + instance.currentPhase().map(phase -> phase.value()).orElse("none"),
            NamedTextColor.YELLOW));
    engine
        .status(instance.id())
        .ifPresent(
            status -> {
              if (status.maxHealth() > 0) {
                details.add(
                    Component.text(
                        "Health: " + status.currentHealth() + "/" + status.maxHealth(),
                        NamedTextColor.RED));
              }
              if (status.maxHits() > 0) {
                details.add(
                    Component.text(
                        "Hits: " + status.currentHits() + "/" + status.maxHits(),
                        NamedTextColor.LIGHT_PURPLE));
              }
              status
                  .location()
                  .ifPresent(
                      location ->
                          details.add(
                              Component.text(
                                  "Location: "
                                      + location.world()
                                      + " "
                                      + Math.round(location.x())
                                      + ", "
                                      + Math.round(location.y())
                                      + ", "
                                      + Math.round(location.z()),
                                  NamedTextColor.AQUA)));
              if (status.timerDeadlineMillis() > 0) {
                long remaining =
                    Math.max(0L, status.timerDeadlineMillis() - System.currentTimeMillis());
                details.add(
                    Component.text(
                        "Time remaining: " + Duration.ofMillis(remaining).toSeconds() + "s",
                        NamedTextColor.GREEN));
              }
              if (status.locked()) {
                details.add(Component.text("Interaction: locked", NamedTextColor.GOLD));
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
            Material.ARROW, Component.text("Back to Events", NamedTextColor.YELLOW), List.of()));
    inventory.setItem(
        SLOT_CANCEL,
        MainScreen.createGuiItem(
            Material.REDSTONE,
            Component.text("Cancel Event", NamedTextColor.RED),
            List.of(Component.text("Requires confirmation", NamedTextColor.GRAY))));
    inventory.setItem(
        SLOT_REFRESH,
        MainScreen.createGuiItem(
            Material.CLOCK,
            Component.text("Refresh", NamedTextColor.AQUA),
            List.of(Component.text("Reload current state", NamedTextColor.GRAY))));
    return inventory;
  }
}
