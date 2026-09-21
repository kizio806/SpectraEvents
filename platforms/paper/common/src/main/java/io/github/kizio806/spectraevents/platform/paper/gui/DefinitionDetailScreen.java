package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.registry.RegisteredEventDefinition;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

/**
 * Renders the detail screen for a single event definition, showing its phases and offering a Start
 * button. The screen is read-only except for the Start and Back actions.
 *
 * <p>Slot layout (54-slot inventory):
 *
 * <pre>
 *  Rows 0–3 (slots 0–35): phase items (up to 36 phases)
 *  Row 4 slot 45: [Back]
 *  Row 4 slot 49: [Start Event]
 * </pre>
 */
public final class DefinitionDetailScreen {

  /** Slot index reserved for the Back button. */
  static final int SLOT_BACK = 45;

  /** Slot index reserved for the Start Event button. */
  static final int SLOT_START = 49;

  /**
   * Creates and populates the inventory for the given definition ID.
   *
   * @param definitionId definition identifier to display
   * @param definitionRegistry registry used to look up the definition
   * @return the populated inventory, or {@code null} when the definition is not found
   */
  public static Inventory createInventory(
      String definitionId, EventDefinitionRegistry definitionRegistry) {
    RegisteredEventDefinition registered = definitionRegistry.findById(definitionId).orElse(null);
    if (registered == null) {
      return null;
    }

    EventDefinition definition = registered.definition();

    Inventory inv =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.DEFINITION_DETAIL,
            definitionId,
            /* slotPayloads= */ java.util.Map.of(),
            54,
            Component.text("Definition: " + definitionId, NamedTextColor.DARK_GREEN));

    // Render one item per phase (up to 36 items in the first four rows)
    int slot = 0;
    for (PhaseId phaseId : definition.phaseIds()) {
      if (slot >= 36) break;
      PhaseDefinition phase =
          definition.phase(phaseId).orElseThrow(() -> new IllegalStateException("missing phase"));

      List<Component> lore = new ArrayList<>();
      lore.add(
          Component.text(
              "On-enter actions: " + phase.onEnterActions().size(), NamedTextColor.GRAY));
      lore.add(Component.text("Transition rules: " + phase.rules().size(), NamedTextColor.GRAY));

      boolean isInitial = phaseId.equals(definition.initialPhase());
      Material phaseMat = isInitial ? Material.LIME_WOOL : Material.LIGHT_GRAY_WOOL;
      Component phaseName =
          isInitial
              ? Component.text(phaseId.value(), NamedTextColor.GREEN, TextDecoration.BOLD)
                  .append(Component.text(" (initial)", NamedTextColor.YELLOW))
              : Component.text(phaseId.value(), NamedTextColor.WHITE);

      inv.setItem(slot++, MainScreen.createGuiItem(phaseMat, phaseName, lore));
    }

    inv.setItem(
        SLOT_BACK,
        MainScreen.createGuiItem(
            Material.ARROW,
            Component.text("Back to Definitions", NamedTextColor.YELLOW),
            List.of()));

    inv.setItem(
        SLOT_START,
        MainScreen.createGuiItem(
            Material.NETHER_STAR,
            Component.text("Start Event", NamedTextColor.GREEN, TextDecoration.BOLD),
            List.of(
                Component.text(
                    "Starts '" + definitionId + "' at your location.", NamedTextColor.GRAY))));

    return inv;
  }
}
