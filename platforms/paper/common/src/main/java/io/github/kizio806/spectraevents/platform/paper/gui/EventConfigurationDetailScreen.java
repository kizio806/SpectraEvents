package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.DurationText;
import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import io.github.kizio806.spectraevents.application.config.registry.EventDefinitionRegistry;
import io.github.kizio806.spectraevents.application.config.spec.EventParameterType;
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
  static final int SLOT_BACK = 49;
  static final int SLOT_REFRESH = 53;

  private EventConfigurationDetailScreen() {}

  public static Inventory createInventory(
      String definitionId,
      EventDefinitionRegistry definitions,
      PaperEventSettingsStore settingsStore,
      LocaleCatalog locales) {
    var definition = definitions.findById(definitionId).orElseThrow();
    Map<String, Object> settings = settingsStore.overridesFor(definitionId);
    Map<Integer, String> payloads = new LinkedHashMap<>();
    int slot = 10;
    for (var declaration : definition.sourceSpec().parameters().values()) {
      if (!declaration.guiEditable()) {
        continue;
      }
      if (slot >= 36) {
        break;
      }
      payloads.put(slot++, declaration.name());
    }
    Inventory inventory =
        AdminGuiHolder.createInventory(
            AdminGuiController.MenuType.EVENT_CONFIGURATION_DETAIL,
            definitionId,
            payloads,
            54,
            Component.text(
                locales.message("admin.configuration.configure", Map.of("event", definitionId)),
                NamedTextColor.YELLOW));

    for (Map.Entry<Integer, String> entry : payloads.entrySet()) {
      String parameter = entry.getValue();
      var declaration = definition.sourceSpec().parameters().get(parameter);
      Object value = settings.getOrDefault(parameter, declaration.defaultValue());
      String displayedValue =
          declaration.type() == EventParameterType.DURATION
              ? DurationText.format(DurationText.parse(value))
              : String.valueOf(value);
      String range =
          declaration.type() == EventParameterType.DURATION
              ? DurationText.format(java.time.Duration.ofSeconds(declaration.minimum().longValue()))
                  + ".."
                  + DurationText.format(
                      java.time.Duration.ofSeconds(declaration.maximum().longValue()))
                  + " step "
                  + DurationText.format(
                      java.time.Duration.ofSeconds(declaration.step().longValue()))
              : declaration.minimum()
                  + ".."
                  + declaration.maximum()
                  + " step "
                  + declaration.step();
      inventory.setItem(
          entry.getKey(),
          MainScreen.createGuiItem(
              Material.COMPARATOR,
              Component.text(parameter, NamedTextColor.GOLD),
              List.of(
                  Component.text(
                      locales.message(
                          "admin.configuration.current", Map.of("value", displayedValue)),
                      NamedTextColor.YELLOW),
                  Component.text(
                      locales.message("admin.configuration.range", Map.of("range", range)),
                      NamedTextColor.GRAY),
                  Component.text(
                      locales.message("admin.configuration.increase"), NamedTextColor.GREEN),
                  Component.text(
                      locales.message("admin.configuration.decrease"), NamedTextColor.RED),
                  Component.text(
                      locales.message("admin.configuration.exact-command"),
                      NamedTextColor.DARK_GRAY))));
    }
    inventory.setItem(
        SLOT_BACK,
        MainScreen.createGuiItem(
            Material.BARRIER,
            Component.text(locales.message("admin.configuration.back"), NamedTextColor.RED),
            List.of()));
    inventory.setItem(
        SLOT_REFRESH,
        MainScreen.createGuiItem(
            Material.CLOCK,
            Component.text(locales.message("common.refresh"), NamedTextColor.AQUA),
            List.of(
                Component.text(
                    locales.message("admin.configuration.refresh"), NamedTextColor.GRAY))));
    return inventory;
  }
}
