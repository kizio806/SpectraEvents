package io.github.kizio806.spectraevents.platform.paper.gui;

import io.github.kizio806.spectraevents.application.config.locale.LocaleCatalog;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** Converts a catalog entry to an Adventure component at the Paper UI boundary. */
final class GuiText {
  private GuiText() {}

  static Component component(LocaleCatalog locales, String key, NamedTextColor color) {
    return Component.text(locales.message(key), color);
  }

  static Component component(
      LocaleCatalog locales, String key, NamedTextColor color, Map<String, ?> placeholders) {
    return Component.text(locales.message(key, placeholders), color);
  }

  static Component title(LocaleCatalog locales, String key, NamedTextColor color) {
    return Component.text(locales.message(key), color, TextDecoration.BOLD);
  }
}
