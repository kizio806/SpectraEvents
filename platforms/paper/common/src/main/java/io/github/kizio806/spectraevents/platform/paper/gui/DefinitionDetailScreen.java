package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
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
      String definitionId, EventDefinitionRegistry definitionRegistry, LocaleCatalog locales) {
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
            GuiText.component(
                locales,
                "admin.gui.definition-detail.title",
                NamedTextColor.DARK_GREEN,
                java.util.Map.of("definition", definitionId)));

    // Render one item per phase (up to 36 items in the first four rows)
    int slot = 0;
    for (PhaseId phaseId : definition.phaseIds()) {
      if (slot >= 36) break;
      PhaseDefinition phase =
          definition.phase(phaseId).orElseThrow(() -> new IllegalStateException("missing phase"));

      List<Component> lore = new ArrayList<>();
      lore.add(
          GuiText.component(
              locales,
              "admin.gui.definition-detail.on-enter-actions",
              NamedTextColor.GRAY,
              java.util.Map.of("count", phase.onEnterActions().size())));
      lore.add(
          GuiText.component(
              locales,
              "admin.gui.definition-detail.transitions",
              NamedTextColor.GRAY,
              java.util.Map.of("count", phase.rules().size())));

      boolean isInitial = phaseId.equals(definition.initialPhase());
      Material phaseMat = isInitial ? Material.LIME_WOOL : Material.LIGHT_GRAY_WOOL;
      Component phaseName =
          isInitial
              ? Component.text(phaseId.value(), NamedTextColor.GREEN, TextDecoration.BOLD)
                  .append(
                      GuiText.component(
                          locales, "admin.gui.definition-detail.initial", NamedTextColor.YELLOW))
              : Component.text(phaseId.value(), NamedTextColor.WHITE);

      inv.setItem(slot++, MainScreen.createGuiItem(phaseMat, phaseName, lore));
    }

    inv.setItem(
        SLOT_BACK,
        MainScreen.createGuiItem(
            Material.ARROW,
            GuiText.component(locales, "admin.gui.definition-detail.back", NamedTextColor.YELLOW),
            List.of()));

    inv.setItem(
        SLOT_START,
        MainScreen.createGuiItem(
            Material.NETHER_STAR,
            GuiText.title(locales, "admin.gui.definition-detail.start", NamedTextColor.GREEN),
            List.of(
                GuiText.component(
                    locales,
                    "admin.gui.definition-detail.start-lore",
                    NamedTextColor.GRAY,
                    java.util.Map.of("definition", definitionId)))));

    return inv;
  }
}
